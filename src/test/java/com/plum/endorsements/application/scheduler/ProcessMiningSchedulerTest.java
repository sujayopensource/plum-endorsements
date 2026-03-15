package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.service.ProcessMiningService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessMiningScheduler")
class ProcessMiningSchedulerTest {

    @Mock private ProcessMiningService processMiningService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private ProcessMiningScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ProcessMiningScheduler(processMiningService, meterRegistry);
    }

    @Test
    @DisplayName("runDailyAnalysis calls generateInsights on service")
    void runDailyAnalysis_CallsService() {
        scheduler.runDailyAnalysis();

        verify(processMiningService).generateInsights();
    }

    @Test
    @DisplayName("runDailyAnalysis records success timer metric")
    void runDailyAnalysis_RecordsSuccessMetric() {
        scheduler.runDailyAnalysis();

        verify(meterRegistry).timer("endorsement.scheduler.duration",
                "scheduler", "process_mining", "result", "success");
        verify(meterRegistry.counter("endorsement.scheduler.execution",
                "scheduler", "process_mining", "result", "success")).increment();
    }

    @Test
    @DisplayName("runDailyAnalysis records failure metric when service throws")
    void runDailyAnalysis_ServiceFails_RecordsFailureMetric() {
        doThrow(new RuntimeException("Mining failed"))
                .when(processMiningService).generateInsights();

        scheduler.runDailyAnalysis();

        verify(meterRegistry).timer("endorsement.scheduler.duration",
                "scheduler", "process_mining", "result", "failure");
        verify(meterRegistry.counter("endorsement.scheduler.execution",
                "scheduler", "process_mining", "result", "failure")).increment();
    }

    @Test
    @DisplayName("runDailyAnalysis does not propagate exception from service")
    void runDailyAnalysis_ServiceFails_DoesNotPropagate() {
        doThrow(new RuntimeException("Connection timeout"))
                .when(processMiningService).generateInsights();

        // Should not throw
        scheduler.runDailyAnalysis();

        verify(processMiningService).generateInsights();
    }

    @Test
    @DisplayName("runDailyAnalysis completes successfully with no-op service")
    void runDailyAnalysis_CompletesSuccessfully() {
        doNothing().when(processMiningService).generateInsights();

        scheduler.runDailyAnalysis();

        verify(processMiningService, times(1)).generateInsights();
    }
}
