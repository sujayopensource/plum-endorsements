package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.ReconciliationRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataReconciliationRunRepository extends JpaRepository<ReconciliationRunEntity, UUID> {
    List<ReconciliationRunEntity> findByInsurerId(UUID insurerId);
}
