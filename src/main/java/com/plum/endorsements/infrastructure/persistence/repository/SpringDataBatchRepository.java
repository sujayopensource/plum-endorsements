package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.EndorsementBatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface SpringDataBatchRepository extends JpaRepository<EndorsementBatchEntity, UUID> {
    List<EndorsementBatchEntity> findByInsurerId(UUID insurerId);
    List<EndorsementBatchEntity> findByStatus(String status);
    boolean existsByInsurerIdAndStatusIn(UUID insurerId, List<String> statuses);

    @Query("SELECT DISTINCT b FROM EndorsementBatchEntity b JOIN EndorsementEntity e ON e.batchId = b.id WHERE e.employerId = :employerId")
    Page<EndorsementBatchEntity> findByEmployerId(UUID employerId, Pageable pageable);
}
