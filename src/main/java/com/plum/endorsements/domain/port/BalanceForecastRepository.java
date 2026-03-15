package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.BalanceForecastRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BalanceForecastRepository {
    BalanceForecastRecord save(BalanceForecastRecord forecast);
    Optional<BalanceForecastRecord> findById(UUID id);
    Optional<BalanceForecastRecord> findLatestByEmployerIdAndInsurerId(UUID employerId, UUID insurerId);
    List<BalanceForecastRecord> findByEmployerId(UUID employerId);
    List<BalanceForecastRecord> findByEmployerIdAndInsurerId(UUID employerId, UUID insurerId);
}
