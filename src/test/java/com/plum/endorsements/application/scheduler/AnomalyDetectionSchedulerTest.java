package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.service.AnomalyDetectionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnomalyDetectionScheduler")
class AnomalyDetectionSchedulerTest {

    @Mock private AnomalyDetectionService anomalyDetectionService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private AnomalyDetectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AnomalyDetectionScheduler(anomalyDetectionService, meterRegistry);
    }

    @Test
    @DisplayName("runScheduledAnalysis calls batchAnalysis on service")
    void runScheduledAnalysis_CallsService() {
        scheduler.runScheduledAnalysis();

        verify(anomalyDetectionService).runBatchAnalysis();
    }

    @Test
    @DisplayName("runScheduledAnalysis records success timer metric")
    void runScheduledAnalysis_RecordsSuccessMetric() {
        scheduler.runScheduledAnalysis();

        verify(meterRegistry).timer("endorsement.scheduler.duration",
                "scheduler", "anomaly_detection", "result", "success");
        verify(meterRegistry.counter("endorsement.scheduler.execution",
                "scheduler", "anomaly_detection", "result", "success")).increment();
    }

    @Test
    @DisplayName("runScheduledAnalysis records failure metric when service throws")
    void runScheduledAnalysis_ServiceFails_RecordsFailureMetric() {
        doThrow(new RuntimeException("Analysis failed"))
                .when(anomalyDetectionService).runBatchAnalysis();

        scheduler.runScheduledAnalysis();

        verify(meterRegistry).timer("endorsement.scheduler.duration",
                "scheduler", "anomaly_detection", "result", "failure");
        verify(meterRegistry.counter("endorsement.scheduler.execution",
                "scheduler", "anomaly_detection", "result", "failure")).increment();
    }

    @Test
    @DisplayName("runScheduledAnalysis does not propagate exception")
    void runScheduledAnalysis_ServiceFails_DoesNotPropagate() {
        doThrow(new RuntimeException("DB connection failed"))
                .when(anomalyDetectionService).runBatchAnalysis();

        // Should not throw
        scheduler.runScheduledAnalysis();

        verify(anomalyDetectionService).runBatchAnalysis();
    }
}
