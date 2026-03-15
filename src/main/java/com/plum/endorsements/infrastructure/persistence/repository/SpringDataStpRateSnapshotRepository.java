package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.StpRateSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataStpRateSnapshotRepository extends JpaRepository<StpRateSnapshotEntity, UUID> {
    List<StpRateSnapshotEntity> findByInsurerIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            UUID insurerId, LocalDate from, LocalDate to);
    Optional<StpRateSnapshotEntity> findByInsurerIdAndSnapshotDate(UUID insurerId, LocalDate date);
}
