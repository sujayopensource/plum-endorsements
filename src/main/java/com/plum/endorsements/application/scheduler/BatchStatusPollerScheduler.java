package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.handler.ProcessEndorsementHandler;
import com.plum.endorsements.domain.model.BatchStatus;
import com.plum.endorsements.domain.model.EndorsementBatch;
import com.plum.endorsements.domain.port.BatchRepository;
import com.plum.endorsements.domain.port.InsurerPort;
import com.plum.endorsements.domain.port.NotificationPort;
import com.plum.endorsements.infrastructure.insurer.InsurerRouter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchStatusPollerScheduler {

    private final BatchRepository batchRepository;
    private final InsurerRouter insurerRouter;
    private final ProcessEndorsementHandler processHandler;
    private final NotificationPort notificationPort;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "60000")
    @SchedulerLock(name = "batchStatusPoller", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    @Transactional
    public void pollBatchStatuses() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            List<EndorsementBatch> submitted = batchRepository.findByStatus(BatchStatus.SUBMITTED);
            List<EndorsementBatch> processing = batchRepository.findByStatus(BatchStatus.PROCESSING);

            List<EndorsementBatch> activeBatches = new ArrayList<>(submitted);
            activeBatches.addAll(processing);

            if (activeBatches.isEmpty()) {
                return;
            }

            log.debug("Polling status for {} active batches", activeBatches.size());

            for (EndorsementBatch batch : activeBatches) {
                pollBatch(batch);
            }
        } catch (Exception e) {
            result = "failure";
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("endorsement.scheduler.duration",
                    "scheduler", "batch_poller", "result", result));
            meterRegistry.counter("endorsement.scheduler.execution",
                    "scheduler", "batch_poller", "result", result).increment();
        }
    }

    private void pollBatch(EndorsementBatch batch) {
        if (batch.getInsurerBatchRef() == null) {
            return;
        }

        try {
            MDC.put("batchId", batch.getId().toString());

            if (batch.isSlaBreached(Instant.now())) {
                log.warn("Batch {} has breached SLA deadline", batch.getId());
                notificationPort.notifyBatchSlaBreached(batch.getId(), batch.getInsurerId());
            }

            InsurerPort insurerPort = insurerRouter.resolve(batch.getInsurerId());
            InsurerPort.BatchStatusResult statusResult = insurerPort.checkBatchStatus(batch.getInsurerBatchRef());

            switch (statusResult.status()) {
                case "PROCESSING" -> {
                    if (batch.getStatus() != BatchStatus.PROCESSING) {
                        batch.setStatus(BatchStatus.PROCESSING);
                        batchRepository.save(batch);
                    }
                }
                case "COMPLETED" -> {
                    batch.setStatus(BatchStatus.COMPLETE);
                    batchRepository.save(batch);
                    for (InsurerPort.EndorsementResult er : statusResult.results()) {
                        if (er.confirmed()) {
                            processHandler.handleConfirmation(er.endorsementId(), er.insurerReference());
                        } else {
                            processHandler.handleRejection(er.endorsementId(), er.rejectionReason());
                        }
                    }
                    log.info("Batch {} completed with {} results", batch.getId(), statusResult.results().size());
                }
                case "FAILED" -> {
                    batch.setStatus(BatchStatus.FAILED);
                    batchRepository.save(batch);
                    log.error("Batch {} failed at insurer", batch.getId());
                }
                default -> log.debug("Batch {} status: {}", batch.getId(), statusResult.status());
            }
        } finally {
            MDC.remove("batchId");
        }
    }
}
