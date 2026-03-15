package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import com.plum.endorsements.domain.service.EndorsementStateMachine;
import com.plum.endorsements.infrastructure.insurer.InsurerRouter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchAssemblyScheduler {

    private final EndorsementRepository endorsementRepository;
    private final BatchRepository batchRepository;
    private final EAAccountRepository eaAccountRepository;
    private final InsurerRouter insurerRouter;
    private final EndorsementStateMachine stateMachine;
    private final EventPublisher eventPublisher;
    private final BatchOptimizerPort batchOptimizer;
    private final MeterRegistry meterRegistry;

    @Value("${endorsement.intelligence.batch-optimizer.enabled:true}")
    private boolean optimizerEnabled;

    @Scheduled(cron = "${endorsement.batch.schedule-cron}")
    @SchedulerLock(name = "batchAssembly", lockAtLeastFor = "PT1M", lockAtMostFor = "PT14M")
    @Transactional
    public void assembleAndSubmitBatches() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            log.info("Starting batch assembly cycle");

            List<Endorsement> queued = endorsementRepository
                    .findByStatusAndInsurerId(EndorsementStatus.QUEUED_FOR_BATCH, null);

            if (queued.isEmpty()) {
                log.debug("No endorsements queued for batch submission");
                return;
            }

            Map<UUID, List<Endorsement>> byInsurer = queued.stream()
                    .collect(Collectors.groupingBy(Endorsement::getInsurerId));

            for (var entry : byInsurer.entrySet()) {
                UUID insurerId = entry.getKey();
                List<Endorsement> endorsements = entry.getValue();
                assembleBatchForInsurer(insurerId, endorsements);
            }

            log.info("Batch assembly cycle completed");
        } catch (Exception e) {
            result = "failure";
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("endorsement.scheduler.duration",
                    "scheduler", "batch_assembly", "result", result));
            meterRegistry.counter("endorsement.scheduler.execution",
                    "scheduler", "batch_assembly", "result", result).increment();
        }
    }

    private void assembleBatchForInsurer(UUID insurerId, List<Endorsement> endorsements) {
        // Guard: insurer can process one batch at a time (PDF requirement)
        List<BatchStatus> activeStatuses = List.of(
                BatchStatus.ASSEMBLING, BatchStatus.SUBMITTED,
                BatchStatus.PROCESSING, BatchStatus.PARTIAL_COMPLETE);
        if (batchRepository.existsByInsurerIdAndStatusIn(insurerId, activeStatuses)) {
            meterRegistry.counter("endorsement.batch.skipped.active",
                    "insurerId", insurerId.toString()).increment();
            log.warn("Skipping batch assembly for insurer {}: active batch still in progress", insurerId);
            return;
        }

        InsurerPort insurerPort = insurerRouter.resolve(insurerId);
        InsurerPort.InsurerCapabilities caps = insurerPort.getCapabilities();
        int maxBatchSize = caps.maxBatchSize();

        // Use batch optimizer if enabled
        List<Endorsement> optimizedQueue = endorsements;
        if (optimizerEnabled) {
            try {
                // Get EA account for the first employer (batch-level optimization)
                UUID firstEmployerId = endorsements.get(0).getEmployerId();
                EAAccount account = eaAccountRepository
                        .findByEmployerIdAndInsurerId(firstEmployerId, insurerId)
                        .orElse(null);

                BatchOptimizerPort.OptimizedBatchPlan plan = batchOptimizer.optimizeBatch(
                        endorsements, account, caps);
                optimizedQueue = plan.endorsements();

                if (plan.estimatedSavings().signum() > 0) {
                    eventPublisher.publish(new EndorsementEvent.BatchOptimized(
                            UUID.randomUUID(), Instant.now(), firstEmployerId,
                            null, plan.strategy(), plan.estimatedSavings()));
                }

                log.info("Batch optimizer: strategy='{}', savings=₹{}",
                        plan.strategy(), plan.estimatedSavings());
            } catch (Exception e) {
                log.warn("Batch optimizer failed, falling back to default sequencing: {}", e.getMessage());
                optimizedQueue = endorsements;
            }
        }

        for (int i = 0; i < optimizedQueue.size(); i += maxBatchSize) {
            List<Endorsement> chunk = optimizedQueue.subList(
                    i, Math.min(i + maxBatchSize, optimizedQueue.size()));

            BigDecimal totalPremium = chunk.stream()
                    .map(Endorsement::getPremiumAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            EndorsementBatch batch = EndorsementBatch.builder()
                    .insurerId(insurerId)
                    .status(BatchStatus.ASSEMBLING)
                    .endorsementCount(chunk.size())
                    .totalPremium(totalPremium)
                    .createdAt(Instant.now())
                    .build();

            batch = batchRepository.save(batch);

            try {
                MDC.put("batchId", batch.getId().toString());
                MDC.put("insurerId", insurerId.toString());

                for (Endorsement e : chunk) {
                    e.setBatchId(batch.getId());
                    stateMachine.transition(e, EndorsementStatus.BATCH_SUBMITTED);
                    endorsementRepository.save(e);
                    eventPublisher.publish(new EndorsementEvent.BatchSubmitted(
                            e.getId(), Instant.now(), e.getEmployerId(), batch.getId()));
                }

                List<Map<String, Object>> payload = chunk.stream()
                        .map(e -> Map.<String, Object>of("endorsementId", e.getId().toString()))
                        .toList();

                String insurerBatchRef = insurerPort.submitBatch(batch.getId(), payload);

                batch.setStatus(BatchStatus.SUBMITTED);
                batch.setSubmittedAt(Instant.now());
                batch.setInsurerBatchRef(insurerBatchRef);
                batch.setSlaDeadline(Instant.now().plus(caps.batchSlaHours(), ChronoUnit.HOURS));
                batchRepository.save(batch);

                meterRegistry.summary("endorsement.batch.size").record(chunk.size());

                log.info("Submitted batch {} with {} endorsements to insurer {}",
                        batch.getId(), chunk.size(), insurerId);
            } finally {
                MDC.remove("batchId");
                MDC.remove("insurerId");
            }
        }
    }
}
