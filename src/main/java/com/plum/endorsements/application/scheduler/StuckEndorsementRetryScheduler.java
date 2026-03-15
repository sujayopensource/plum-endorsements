package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.handler.ProcessEndorsementHandler;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.port.EndorsementRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Gap 4 fix: Periodically finds endorsements stuck in RETRY_PENDING
 * and resubmits them to the insurer. Without this scheduler, endorsements
 * that fail and are marked for retry would remain in RETRY_PENDING
 * indefinitely, leaving provisional coverage in limbo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StuckEndorsementRetryScheduler {

    private final EndorsementRepository endorsementRepository;
    private final ProcessEndorsementHandler processEndorsementHandler;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${endorsement.retry.poll-interval-ms:300000}")
    @SchedulerLock(name = "stuckEndorsementRetry", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    public void retryStuckEndorsements() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            List<Endorsement> retryPending = endorsementRepository
                    .findByStatus(EndorsementStatus.RETRY_PENDING);

            if (retryPending.isEmpty()) {
                log.debug("No stuck RETRY_PENDING endorsements found");
                return;
            }

            log.info("Found {} endorsements in RETRY_PENDING, resubmitting", retryPending.size());
            int resubmitted = 0;

            for (Endorsement endorsement : retryPending) {
                try {
                    processEndorsementHandler.submitToInsurer(endorsement.getId());
                    resubmitted++;
                    log.info("Resubmitted stuck endorsement {} (retry #{})",
                            endorsement.getId(), endorsement.getRetryCount());
                } catch (Exception e) {
                    log.error("Failed to resubmit endorsement {}: {}",
                            endorsement.getId(), e.getMessage());
                    meterRegistry.counter("endorsement.retry.resubmit.error").increment();
                }
            }

            meterRegistry.counter("endorsement.retry.resubmitted").increment(resubmitted);
            log.info("Resubmitted {} of {} stuck endorsements", resubmitted, retryPending.size());
        } catch (Exception e) {
            result = "failure";
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("endorsement.scheduler.duration",
                    "scheduler", "retry_stuck", "result", result));
            meterRegistry.counter("endorsement.scheduler.execution",
                    "scheduler", "retry_stuck", "result", result).increment();
        }
    }
}
