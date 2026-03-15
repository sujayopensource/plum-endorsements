package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.ProcessMiningMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataProcessMiningMetricRepository extends JpaRepository<ProcessMiningMetricEntity, UUID> {
    List<ProcessMiningMetricEntity> findByInsurerId(UUID insurerId);
    List<ProcessMiningMetricEntity> findByInsurerIdOrderByCalculatedAtDesc(UUID insurerId);
    void deleteByInsurerId(UUID insurerId);
}
