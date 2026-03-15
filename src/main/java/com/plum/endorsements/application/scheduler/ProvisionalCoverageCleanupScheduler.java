package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.ProvisionalCoverage;
import com.plum.endorsements.domain.port.EndorsementRepository;
import com.plum.endorsements.domain.port.EventPublisher;
import com.plum.endorsements.domain.port.NotificationPort;
import com.plum.endorsements.domain.port.ProvisionalCoverageRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProvisionalCoverageCleanupScheduler {

    private final ProvisionalCoverageRepository provisionalCoverageRepository;
    private final EndorsementRepository endorsementRepository;
    private final EventPublisher eventPublisher;
    private final NotificationPort notificationPort;
    private final MeterRegistry meterRegistry;

    @Value("${endorsement.provisional-coverage.max-days:30}")
    private int maxDays;

    @Value("${endorsement.provisional-coverage.warning-days-before-expiry:2}")
    private int warningDaysBeforeExpiry;

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "provisionalCoverageCleanup", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    @Transactional
    public void expireStaleProvisionalCoverages() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            log.info("Starting provisional coverage cleanup (maxDays={}, warningDays={})", maxDays, warningDaysBeforeExpiry);

            // Warn about coverages approaching expiry
            warnExpiringCoverages();

            List<ProvisionalCoverage> stale = provisionalCoverageRepository
                    .findStaleProvisionalCoverages(maxDays);

            if (stale.isEmpty()) {
                log.debug("No stale provisional coverages found");
                return;
            }

            Instant now = Instant.now();
            int expiredCount = 0;
            int skippedCount = 0;

            for (ProvisionalCoverage coverage : stale) {
                // Gap 2 fix: check endorsement status before expiring
                Optional<Endorsement> endorsementOpt = endorsementRepository
                        .findById(coverage.getEndorsementId());
                if (endorsementOpt.isPresent() && endorsementOpt.get().getStatus().isActive()) {
                    log.warn("Skipping coverage {} — endorsement {} is still active (status={})",
                            coverage.getId(), coverage.getEndorsementId(),
                            endorsementOpt.get().getStatus());
                    skippedCount++;
                    continue;
                }

                coverage.expire(now);
                provisionalCoverageRepository.save(coverage);
                expiredCount++;
                log.warn("Expired stale provisional coverage {} for endorsement {}",
                        coverage.getId(), coverage.getEndorsementId());

                // Gap 3 fix: publish event and notify employer
                eventPublisher.publish(new EndorsementEvent.ProvisionalCoverageExpired(
                        coverage.getEndorsementId(), now,
                        coverage.getEmployerId(), coverage.getEmployeeId()));

                notificationPort.notifyCoverageExpired(coverage.getEmployerId(), coverage.getEmployeeId(),
                        "Provisional coverage expired without insurer confirmation after " + maxDays + " days. Immediate action required.");
            }

            meterRegistry.counter("endorsement.coverage.expired").increment(expiredCount);
            log.info("Expired {} stale provisional coverages, skipped {} with active endorsements",
                    expiredCount, skippedCount);
        } catch (Exception e) {
            result = "failure";
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("endorsement.scheduler.duration",
                    "scheduler", "coverage_cleanup", "result", result));
            meterRegistry.counter("endorsement.scheduler.execution",
                    "scheduler", "coverage_cleanup", "result", result).increment();
        }
    }

    private void warnExpiringCoverages() {
        Instant now = Instant.now();
        int warningDays = maxDays - warningDaysBeforeExpiry;
        Instant warningCutoff = now.minus(warningDays, ChronoUnit.DAYS);
        Instant staleCutoff = now.minus(maxDays, ChronoUnit.DAYS);

        List<ProvisionalCoverage> expiringSoon = provisionalCoverageRepository
                .findActiveExpiringBefore(warningCutoff, staleCutoff);

        for (ProvisionalCoverage coverage : expiringSoon) {
            int daysRemaining = maxDays - (int) ChronoUnit.DAYS.between(coverage.getCreatedAt(), now);
            notificationPort.notifyCoverageAtRisk(
                    coverage.getEmployerId(),
                    coverage.getEndorsementId(),
                    "Provisional coverage will expire in " + daysRemaining
                            + " days unless endorsement is confirmed by the insurer");
            log.warn("Coverage at risk: endorsement={}, employer={}, daysRemaining={}",
                    coverage.getEndorsementId(), coverage.getEmployerId(), daysRemaining);
        }

        if (!expiringSoon.isEmpty()) {
            meterRegistry.counter("endorsement.coverage.warning").increment(expiringSoon.size());
            log.info("Sent {} coverage-expiring-soon warnings", expiringSoon.size());
        }
    }
}
