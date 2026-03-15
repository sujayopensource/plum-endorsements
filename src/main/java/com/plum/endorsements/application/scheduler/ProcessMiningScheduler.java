package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.service.ProcessMiningService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "endorsement.intelligence.process-mining.enabled", havingValue = "true")
public class ProcessMiningScheduler {

    private final ProcessMiningService processMiningService;
    private final MeterRegistry meterRegistry;

    @Scheduled(cron = "${endorsement.intelligence.process-mining.schedule-cron}")
    @SchedulerLock(name = "processMining", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void runDailyAnalysis() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            log.info("Starting daily process mining analysis");
            processMiningService.generateInsights();
            processMiningService.captureAllStpRateSnapshots();
            log.info("Daily process mining analysis completed");
        } catch (Exception e) {
            result = "failure";
            log.error("Daily process mining analysis failed", e);
        } finally {
            sample.stop(meterRegistry.timer("endorsement.scheduler.duration",
                    "scheduler", "process_mining", "result", result));
            meterRegistry.counter("endorsement.scheduler.execution",
                    "scheduler", "process_mining", "result", result).increment();
        }
    }
}
