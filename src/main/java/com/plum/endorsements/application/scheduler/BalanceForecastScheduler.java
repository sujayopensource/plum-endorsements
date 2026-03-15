package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.service.BalanceForecastService;
import com.plum.endorsements.domain.model.EAAccount;
import com.plum.endorsements.domain.port.EAAccountRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "endorsement.intelligence.balance-forecast.enabled", havingValue = "true")
public class BalanceForecastScheduler {

    private final BalanceForecastService forecastService;
    private final EAAccountRepository eaAccountRepository;
    private final MeterRegistry meterRegistry;

    @Scheduled(cron = "${endorsement.intelligence.balance-forecast.schedule-cron}")
    @SchedulerLock(name = "balanceForecast", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void runDailyForecast() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            log.info("Starting daily balance forecast generation");

            List<EAAccount> accounts = eaAccountRepository.findAll();
            int generated = 0;

            for (EAAccount account : accounts) {
                try {
                    forecastService.generateForecast(account.getEmployerId(), account.getInsurerId());
                    generated++;
                } catch (Exception e) {
                    log.error("Failed to generate forecast for employer {} insurer {}",
                            account.getEmployerId(), account.getInsurerId(), e);
                }
            }

            log.info("Daily forecast generation completed: {} forecasts generated", generated);
        } catch (Exception e) {
            result = "failure";
            log.error("Daily forecast generation failed", e);
        } finally {
            sample.stop(meterRegistry.timer("endorsement.scheduler.duration",
                    "scheduler", "balance_forecast", "result", result));
            meterRegistry.counter("endorsement.scheduler.execution",
                    "scheduler", "balance_forecast", "result", result).increment();
        }
    }
}
