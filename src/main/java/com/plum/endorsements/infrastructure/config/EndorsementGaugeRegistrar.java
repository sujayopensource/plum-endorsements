package com.plum.endorsements.infrastructure.config;

import com.plum.endorsements.domain.port.InsurerConfigurationRepository;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataEndorsementRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EndorsementGaugeRegistrar {

    private final MeterRegistry meterRegistry;
    private final SpringDataEndorsementRepository repository;
    private final InsurerConfigurationRepository insurerConfigurationRepository;

    @PostConstruct
    public void registerGauges() {
        for (String status : List.of("CREATED", "VALIDATED", "PROVISIONALLY_COVERED",
                "SUBMITTED_REALTIME", "QUEUED_FOR_BATCH", "BATCH_SUBMITTED",
                "INSURER_PROCESSING", "CONFIRMED", "REJECTED",
                "RETRY_PENDING", "FAILED_PERMANENT")) {
            Gauge.builder("endorsement.active.count", repository,
                            repo -> repo.countByStatus(status))
                    .tag("status", status)
                    .description("Number of endorsements in " + status + " state")
                    .register(meterRegistry);
        }

        Gauge.builder("endorsement.insurer.active.count", insurerConfigurationRepository,
                        repo -> repo.findAllActive().size())
                .description("Number of active insurer configurations")
                .register(meterRegistry);
    }
}
