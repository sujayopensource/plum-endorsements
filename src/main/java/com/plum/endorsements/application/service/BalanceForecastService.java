package com.plum.endorsements.application.service;

import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceForecastService {

    private final BalanceForecastPort forecastEngine;
    private final BalanceForecastRepository forecastRepository;
    private final EndorsementRepository endorsementRepository;
    private final EAAccountRepository eaAccountRepository;
    private final EventPublisher eventPublisher;
    private final NotificationPort notificationPort;
    private final MeterRegistry meterRegistry;

    @Value("${endorsement.intelligence.balance-forecast.alert-days-ahead:7}")
    private int alertDaysAhead;

    @Transactional
    public BalanceForecastRecord generateForecast(UUID employerId, UUID insurerId) {
        // Gather history
        List<Endorsement> history = new ArrayList<>();
        history.addAll(endorsementRepository.findByStatus(EndorsementStatus.CREATED));
        history.addAll(endorsementRepository.findByStatus(EndorsementStatus.VALIDATED));
        history.addAll(endorsementRepository.findByStatus(EndorsementStatus.CONFIRMED));
        history.addAll(endorsementRepository.findByStatus(EndorsementStatus.PROVISIONALLY_COVERED));

        BalanceForecastPort.ForecastResult result = forecastEngine.generateForecast(employerId, insurerId, history);

        // Check current balance and compute shortfall
        Optional<EAAccount> accountOpt = eaAccountRepository.findByEmployerIdAndInsurerId(employerId, insurerId);
        BigDecimal shortfall = BigDecimal.ZERO;
        boolean topUpRequired = false;

        if (accountOpt.isPresent()) {
            EAAccount account = accountOpt.get();
            BigDecimal available = account.availableBalance();
            shortfall = result.forecastedNeed().subtract(available);
            topUpRequired = shortfall.signum() > 0;
        }

        BalanceForecastRecord record = BalanceForecastRecord.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .forecastDate(LocalDate.now().plusDays(result.daysAhead()))
                .forecastedAmount(result.forecastedNeed())
                .narrative(result.narrative())
                .createdAt(Instant.now())
                .build();

        record = forecastRepository.save(record);

        meterRegistry.counter("endorsement.forecast.generated",
                "employerId", employerId.toString()).increment();

        // Publish event
        eventPublisher.publish(new EndorsementEvent.ForecastGenerated(
                UUID.randomUUID(), Instant.now(), employerId,
                result.forecastedNeed(), result.daysAhead(), result.narrative()));

        // Alert if shortfall detected
        if (topUpRequired) {
            meterRegistry.counter("endorsement.forecast.shortfall.detected",
                    "employerId", employerId.toString()).increment();

            eventPublisher.publish(new EndorsementEvent.BalanceForecastAlert(
                    UUID.randomUUID(), Instant.now(), employerId, shortfall));

            notificationPort.notifyInsufficientBalance(employerId,
                    result.forecastedNeed(),
                    accountOpt.map(EAAccount::availableBalance).orElse(BigDecimal.ZERO));

            notificationPort.notifyForecastShortfall(employerId, shortfall, result.daysAhead());

            log.warn("Forecast shortfall for employer {}: need={}, shortfall={}",
                    employerId, result.forecastedNeed(), shortfall);
        }

        return record;
    }

    public Optional<BalanceForecastRecord> getLatestForecast(UUID employerId, UUID insurerId) {
        return forecastRepository.findLatestByEmployerIdAndInsurerId(employerId, insurerId);
    }

    public List<BalanceForecastRecord> getForecastHistory(UUID employerId) {
        return forecastRepository.findByEmployerId(employerId);
    }
}
