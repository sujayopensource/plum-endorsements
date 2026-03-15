package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.ReconciliationItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataReconciliationItemRepository extends JpaRepository<ReconciliationItemEntity, UUID> {
    List<ReconciliationItemEntity> findByRunId(UUID runId);
    List<ReconciliationItemEntity> findByRunIdAndOutcome(UUID runId, String outcome);
}
