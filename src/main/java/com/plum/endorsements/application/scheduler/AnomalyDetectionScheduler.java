package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.service.AnomalyDetectionService;
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
@ConditionalOnProperty(name = "endorsement.intelligence.anomaly-detection.enabled", havingValue = "true")
public class AnomalyDetectionScheduler {

    private final AnomalyDetectionService anomalyDetectionService;
    private final MeterRegistry meterRegistry;

    @Scheduled(cron = "${endorsement.intelligence.anomaly-detection.schedule-cron}")
    @SchedulerLock(name = "anomalyDetection", lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
    public void runScheduledAnalysis() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            log.info("Starting scheduled anomaly detection analysis");
            anomalyDetectionService.runBatchAnalysis();
            log.info("Scheduled anomaly detection analysis completed");
        } catch (Exception e) {
            result = "failure";
            log.error("Scheduled anomaly detection analysis failed", e);
        } finally {
            sample.stop(meterRegistry.timer("endorsement.scheduler.duration",
                    "scheduler", "anomaly_detection", "result", result));
            meterRegistry.counter("endorsement.scheduler.execution",
                    "scheduler", "anomaly_detection", "result", result).increment();
        }
    }
}
