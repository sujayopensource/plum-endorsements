package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.service.ReconciliationEngine;
import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.service.InsurerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final ReconciliationEngine reconciliationEngine;
    private final InsurerRegistry insurerRegistry;
    private final MeterRegistry meterRegistry;

    @Scheduled(cron = "0 */15 * * * *")
    @SchedulerLock(name = "reconciliation", lockAtLeastFor = "PT1M", lockAtMostFor = "PT14M")
    public void runReconciliation() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            List<InsurerConfiguration> activeInsurers = insurerRegistry.getAllActiveInsurers();
            log.info("Starting scheduled reconciliation for {} active insurers", activeInsurers.size());

            for (InsurerConfiguration insurer : activeInsurers) {
                try {
                    reconciliationEngine.reconcileInsurer(insurer.getInsurerId());
                } catch (Exception e) {
                    log.error("Reconciliation failed for insurer {} ({}): {}",
                            insurer.getInsurerCode(), insurer.getInsurerId(), e.getMessage(), e);
                    meterRegistry.counter("endorsement.reconciliation.error",
                            "insurerCode", insurer.getInsurerCode()).increment();
                }
            }
        } catch (Exception e) {
            result = "failure";
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("endorsement.scheduler.duration",
                    "scheduler", "reconciliation", "result", result));
            meterRegistry.counter("endorsement.scheduler.execution",
                    "scheduler", "reconciliation", "result", result).increment();
        }
    }
}
