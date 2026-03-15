package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.EndorsementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;

public interface SpringDataEndorsementRepository extends JpaRepository<EndorsementEntity, UUID> {
    Optional<EndorsementEntity> findByIdempotencyKey(String key);
    Page<EndorsementEntity> findByEmployerId(UUID employerId, Pageable pageable);
    Page<EndorsementEntity> findByEmployerIdAndStatusIn(UUID employerId, List<String> statuses, Pageable pageable);
    List<EndorsementEntity> findByStatus(String status);
    List<EndorsementEntity> findByStatusAndInsurerId(String status, UUID insurerId);
    List<EndorsementEntity> findByBatchId(UUID batchId);
    long countByEmployerIdAndStatus(UUID employerId, String status);
    long countByStatus(String status);
    List<EndorsementEntity> findByEmployerIdAndCreatedAtAfter(UUID employerId, Instant after);
}
