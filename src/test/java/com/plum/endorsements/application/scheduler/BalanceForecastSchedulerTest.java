package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.service.BalanceForecastService;
import com.plum.endorsements.domain.model.EAAccount;
import com.plum.endorsements.domain.port.EAAccountRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceForecastScheduler")
class BalanceForecastSchedulerTest {

    @Mock private BalanceForecastService forecastService;
    @Mock private EAAccountRepository eaAccountRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private BalanceForecastScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BalanceForecastScheduler(forecastService, eaAccountRepository, meterRegistry);
    }

    private EAAccount buildAccount(UUID employerId, UUID insurerId) {
        return EAAccount.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .balance(new BigDecimal("50000.00"))
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("runDailyForecast iterates all accounts and generates forecasts")
    void runDailyForecast_IteratesAllAccounts() {
        UUID employer1 = UUID.randomUUID();
        UUID employer2 = UUID.randomUUID();
        UUID insurer1 = UUID.randomUUID();
        UUID insurer2 = UUID.randomUUID();

        List<EAAccount> accounts = List.of(
                buildAccount(employer1, insurer1),
                buildAccount(employer2, insurer2)
        );

        when(eaAccountRepository.findAll()).thenReturn(accounts);

        scheduler.runDailyForecast();

        verify(forecastService).generateForecast(employer1, insurer1);
        verify(forecastService).generateForecast(employer2, insurer2);
        verify(forecastService, times(2)).generateForecast(any(), any());
    }

    @Test
    @DisplayName("runDailyForecast with no accounts is a no-op")
    void runDailyForecast_NoAccounts_NoOp() {
        when(eaAccountRepository.findAll()).thenReturn(List.of());

        scheduler.runDailyForecast();

        verify(forecastService, never()).generateForecast(any(), any());
    }

    @Test
    @DisplayName("runDailyForecast continues when one account forecast fails")
    void runDailyForecast_OneFails_ContinuesWithRest() {
        UUID employer1 = UUID.randomUUID();
        UUID employer2 = UUID.randomUUID();
        UUID insurer1 = UUID.randomUUID();
        UUID insurer2 = UUID.randomUUID();

        List<EAAccount> accounts = List.of(
                buildAccount(employer1, insurer1),
                buildAccount(employer2, insurer2)
        );

        when(eaAccountRepository.findAll()).thenReturn(accounts);
        doThrow(new RuntimeException("Forecast failed")).when(forecastService)
                .generateForecast(employer1, insurer1);

        scheduler.runDailyForecast();

        // Second account should still be processed despite first failure
        verify(forecastService).generateForecast(employer1, insurer1);
        verify(forecastService).generateForecast(employer2, insurer2);
    }

    @Test
    @DisplayName("runDailyForecast records success metric")
    void runDailyForecast_RecordsSuccessMetric() {
        when(eaAccountRepository.findAll()).thenReturn(List.of());

        scheduler.runDailyForecast();

        verify(meterRegistry).timer("endorsement.scheduler.duration",
                "scheduler", "balance_forecast", "result", "success");
        verify(meterRegistry.counter("endorsement.scheduler.execution",
                "scheduler", "balance_forecast", "result", "success")).increment();
    }

    @Test
    @DisplayName("runDailyForecast records failure metric when findAll throws")
    void runDailyForecast_FindAllFails_RecordsFailureMetric() {
        when(eaAccountRepository.findAll()).thenThrow(new RuntimeException("DB down"));

        scheduler.runDailyForecast();

        verify(meterRegistry).timer("endorsement.scheduler.duration",
                "scheduler", "balance_forecast", "result", "failure");
        verify(meterRegistry.counter("endorsement.scheduler.execution",
                "scheduler", "balance_forecast", "result", "failure")).increment();
    }
}
