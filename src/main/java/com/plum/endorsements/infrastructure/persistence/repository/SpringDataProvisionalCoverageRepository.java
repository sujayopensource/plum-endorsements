package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.ProvisionalCoverageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface SpringDataProvisionalCoverageRepository extends JpaRepository<ProvisionalCoverageEntity, UUID> {
    Optional<ProvisionalCoverageEntity> findByEndorsementId(UUID endorsementId);
    @Query("SELECT p FROM ProvisionalCoverageEntity p WHERE p.employeeId = :employeeId AND p.confirmedAt IS NULL AND p.expiredAt IS NULL")
    List<ProvisionalCoverageEntity> findActiveByEmployeeId(UUID employeeId);
    @Query("SELECT p FROM ProvisionalCoverageEntity p WHERE p.confirmedAt IS NULL AND p.expiredAt IS NULL AND p.createdAt < :cutoff")
    List<ProvisionalCoverageEntity> findStale(java.time.Instant cutoff);

    @Query("SELECT p FROM ProvisionalCoverageEntity p WHERE p.confirmedAt IS NULL AND p.expiredAt IS NULL AND p.createdAt < :warningCutoff AND p.createdAt >= :staleCutoff")
    List<ProvisionalCoverageEntity> findExpiringBetween(java.time.Instant warningCutoff, java.time.Instant staleCutoff);
}
