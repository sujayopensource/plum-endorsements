package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.ProcessMiningMetric;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessMiningRepository {
    ProcessMiningMetric save(ProcessMiningMetric metric);
    List<ProcessMiningMetric> saveAll(List<ProcessMiningMetric> metrics);
    Optional<ProcessMiningMetric> findById(UUID id);
    List<ProcessMiningMetric> findByInsurerId(UUID insurerId);
    List<ProcessMiningMetric> findLatestByInsurerId(UUID insurerId);
    void deleteByInsurerId(UUID insurerId);
}
