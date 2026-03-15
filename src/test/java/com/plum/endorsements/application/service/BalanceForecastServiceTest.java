package com.plum.endorsements.application.service;

import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceForecastService")
class BalanceForecastServiceTest {

    @Mock private BalanceForecastPort forecastEngine;
    @Mock private BalanceForecastRepository forecastRepository;
    @Mock private EndorsementRepository endorsementRepository;
    @Mock private EAAccountRepository eaAccountRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private NotificationPort notificationPort;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    @InjectMocks
    private BalanceForecastService service;

    private UUID employerId;
    private UUID insurerId;

    @BeforeEach
    void setUp() {
        employerId = UUID.randomUUID();
        insurerId = UUID.randomUUID();
        ReflectionTestUtils.setField(service, "alertDaysAhead", 7);
    }

    @Test
    @DisplayName("generateForecast saves record and publishes ForecastGenerated event")
    void generateForecast_SavesAndPublishesEvent() {
        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());
        when(eaAccountRepository.findByEmployerIdAndInsurerId(employerId, insurerId))
                .thenReturn(Optional.empty());
        when(forecastEngine.generateForecast(eq(employerId), eq(insurerId), anyList()))
                .thenReturn(new BalanceForecastPort.ForecastResult(
                        new BigDecimal("50000.00"), 30, new BigDecimal("1666.67"),
                        BigDecimal.ZERO, false, "Forecast narrative"));
        when(forecastRepository.save(any(BalanceForecastRecord.class))).thenAnswer(i -> {
            BalanceForecastRecord r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        BalanceForecastRecord result = service.generateForecast(employerId, insurerId);

        assertThat(result).isNotNull();
        assertThat(result.getEmployerId()).isEqualTo(employerId);
        assertThat(result.getInsurerId()).isEqualTo(insurerId);
        assertThat(result.getForecastedAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));

        verify(forecastRepository).save(any(BalanceForecastRecord.class));
        verify(eventPublisher).publish(any(EndorsementEvent.ForecastGenerated.class));
    }

    @Test
    @DisplayName("generateForecast with shortfall triggers alert and notification")
    void generateForecast_WithShortfall_TriggersAlertAndNotification() {
        EAAccount account = EAAccount.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .balance(new BigDecimal("20000.00"))
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());
        when(eaAccountRepository.findByEmployerIdAndInsurerId(employerId, insurerId))
                .thenReturn(Optional.of(account));
        when(forecastEngine.generateForecast(eq(employerId), eq(insurerId), anyList()))
                .thenReturn(new BalanceForecastPort.ForecastResult(
                        new BigDecimal("50000.00"), 30, new BigDecimal("1666.67"),
                        new BigDecimal("30000.00"), true, "Shortfall forecast"));
        when(forecastRepository.save(any(BalanceForecastRecord.class))).thenAnswer(i -> {
            BalanceForecastRecord r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        service.generateForecast(employerId, insurerId);

        // Should publish both ForecastGenerated and BalanceForecastAlert
        verify(eventPublisher).publish(any(EndorsementEvent.ForecastGenerated.class));
        verify(eventPublisher).publish(any(EndorsementEvent.BalanceForecastAlert.class));
        verify(notificationPort).notifyInsufficientBalance(
                eq(employerId),
                eq(new BigDecimal("50000.00")),
                eq(new BigDecimal("20000.00"))
        );
        verify(notificationPort).notifyForecastShortfall(
                eq(employerId),
                eq(new BigDecimal("30000.00")),
                eq(30)
        );
    }

    @Test
    @DisplayName("generateForecast without shortfall does not trigger alert")
    void generateForecast_NoShortfall_NoAlert() {
        EAAccount account = EAAccount.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .balance(new BigDecimal("100000.00"))
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());
        when(eaAccountRepository.findByEmployerIdAndInsurerId(employerId, insurerId))
                .thenReturn(Optional.of(account));
        when(forecastEngine.generateForecast(eq(employerId), eq(insurerId), anyList()))
                .thenReturn(new BalanceForecastPort.ForecastResult(
                        new BigDecimal("50000.00"), 30, new BigDecimal("1666.67"),
                        BigDecimal.ZERO, false, "Sufficient balance"));
        when(forecastRepository.save(any(BalanceForecastRecord.class))).thenAnswer(i -> {
            BalanceForecastRecord r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        service.generateForecast(employerId, insurerId);

        verify(eventPublisher).publish(any(EndorsementEvent.ForecastGenerated.class));
        verify(eventPublisher, never()).publish(any(EndorsementEvent.BalanceForecastAlert.class));
        verify(notificationPort, never()).notifyInsufficientBalance(any(), any(), any());
        verify(notificationPort, never()).notifyForecastShortfall(any(), any(), anyInt());
    }

    @Test
    @DisplayName("generateForecast without EA account publishes event but no shortfall alert")
    void generateForecast_NoAccount_NoShortfallAlert() {
        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());
        when(eaAccountRepository.findByEmployerIdAndInsurerId(employerId, insurerId))
                .thenReturn(Optional.empty());
        when(forecastEngine.generateForecast(eq(employerId), eq(insurerId), anyList()))
                .thenReturn(new BalanceForecastPort.ForecastResult(
                        new BigDecimal("50000.00"), 30, new BigDecimal("1666.67"),
                        BigDecimal.ZERO, false, "No account found"));
        when(forecastRepository.save(any(BalanceForecastRecord.class))).thenAnswer(i -> {
            BalanceForecastRecord r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        service.generateForecast(employerId, insurerId);

        verify(eventPublisher).publish(any(EndorsementEvent.ForecastGenerated.class));
        verify(eventPublisher, never()).publish(any(EndorsementEvent.BalanceForecastAlert.class));
        verify(notificationPort, never()).notifyInsufficientBalance(any(), any(), any());
    }

    @Test
    @DisplayName("getLatestForecast delegates to repository")
    void getLatestForecast_DelegatesToRepository() {
        BalanceForecastRecord expected = BalanceForecastRecord.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .insurerId(insurerId)
                .forecastedAmount(new BigDecimal("30000.00"))
                .build();

        when(forecastRepository.findLatestByEmployerIdAndInsurerId(employerId, insurerId))
                .thenReturn(Optional.of(expected));

        Optional<BalanceForecastRecord> result = service.getLatestForecast(employerId, insurerId);

        assertThat(result).isPresent();
        assertThat(result.get().getForecastedAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
    }

    // --- Phase 3 Edge Case Tests ---

    @Test
    @DisplayName("generateForecast handles zero balance account correctly")
    void shouldHandleZeroBalanceAccount() {
        EAAccount account = EAAccount.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .balance(BigDecimal.ZERO)
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());
        when(eaAccountRepository.findByEmployerIdAndInsurerId(employerId, insurerId))
                .thenReturn(Optional.of(account));
        when(forecastEngine.generateForecast(eq(employerId), eq(insurerId), anyList()))
                .thenReturn(new BalanceForecastPort.ForecastResult(
                        new BigDecimal("30000.00"), 30, new BigDecimal("1000.00"),
                        new BigDecimal("30000.00"), true, "Zero balance, full shortfall"));
        when(forecastRepository.save(any(BalanceForecastRecord.class))).thenAnswer(i -> {
            BalanceForecastRecord r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        BalanceForecastRecord result = service.generateForecast(employerId, insurerId);

        assertThat(result).isNotNull();
        assertThat(result.getForecastedAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));

        // With zero balance and positive forecast, shortfall alert should fire
        verify(eventPublisher).publish(any(EndorsementEvent.BalanceForecastAlert.class));
        verify(notificationPort).notifyInsufficientBalance(eq(employerId),
                eq(new BigDecimal("30000.00")), eq(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("generateForecast handles negative balance (overdrawn) account")
    void shouldHandleNegativeBalance() {
        EAAccount account = EAAccount.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .balance(new BigDecimal("-5000.00"))
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());
        when(eaAccountRepository.findByEmployerIdAndInsurerId(employerId, insurerId))
                .thenReturn(Optional.of(account));
        when(forecastEngine.generateForecast(eq(employerId), eq(insurerId), anyList()))
                .thenReturn(new BalanceForecastPort.ForecastResult(
                        new BigDecimal("20000.00"), 30, new BigDecimal("666.67"),
                        new BigDecimal("25000.00"), true, "Overdrawn account"));
        when(forecastRepository.save(any(BalanceForecastRecord.class))).thenAnswer(i -> {
            BalanceForecastRecord r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        BalanceForecastRecord result = service.generateForecast(employerId, insurerId);

        assertThat(result).isNotNull();
        // Shortfall should be greater than forecasted need since balance is negative
        // forecastedNeed(20000) - availableBalance(-5000) = 25000 shortfall
        verify(eventPublisher).publish(any(EndorsementEvent.BalanceForecastAlert.class));
        verify(notificationPort).notifyInsufficientBalance(eq(employerId),
                eq(new BigDecimal("20000.00")), eq(new BigDecimal("-5000.00")));
    }

    @Test
    @DisplayName("getForecastHistory delegates to repository and returns list")
    void getForecastHistory_DelegatesToRepository() {
        BalanceForecastRecord r1 = BalanceForecastRecord.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .insurerId(insurerId)
                .forecastedAmount(new BigDecimal("30000.00"))
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        BalanceForecastRecord r2 = BalanceForecastRecord.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .insurerId(insurerId)
                .forecastedAmount(new BigDecimal("45000.00"))
                .createdAt(Instant.now())
                .build();

        when(forecastRepository.findByEmployerId(employerId)).thenReturn(List.of(r1, r2));

        List<BalanceForecastRecord> result = service.getForecastHistory(employerId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getForecastedAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
        assertThat(result.get(1).getForecastedAmount()).isEqualByComparingTo(new BigDecimal("45000.00"));
        verify(forecastRepository).findByEmployerId(employerId);
    }

    @Test
    @DisplayName("getLatestForecast returns empty when no forecast exists")
    void getLatestForecast_ReturnsEmpty_WhenNotFound() {
        when(forecastRepository.findLatestByEmployerIdAndInsurerId(employerId, insurerId))
                .thenReturn(Optional.empty());

        Optional<BalanceForecastRecord> result = service.getLatestForecast(employerId, insurerId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("calculateAccuracy returns correct value when actual is available")
    void shouldCalculateAccuracyWhenActualAvailable() {
        // This tests the domain model method via the service flow
        BalanceForecastRecord record = BalanceForecastRecord.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .insurerId(insurerId)
                .forecastedAmount(new BigDecimal("50000.00"))
                .build();

        // Record actual amount and verify accuracy calculation
        record.recordActual(new BigDecimal("48000.00"));

        assertThat(record.getActualAmount()).isEqualByComparingTo(new BigDecimal("48000.00"));
        assertThat(record.getAccuracy()).isNotNull();
        // Error = |48000 - 50000| = 2000, errorRate = 2000/50000 = 0.04
        // Accuracy = 1 - 0.04 = 0.96
        assertThat(record.getAccuracy()).isEqualByComparingTo(new BigDecimal("0.9600"));
    }
}
