package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface EndorsementRepository {
    Endorsement save(Endorsement endorsement);
    Optional<Endorsement> findById(UUID id);
    Optional<Endorsement> findByIdempotencyKey(String key);
    Page<Endorsement> findByEmployerId(UUID employerId, Pageable pageable);
    Page<Endorsement> findByEmployerIdAndStatusIn(UUID employerId, List<EndorsementStatus> statuses, Pageable pageable);
    List<Endorsement> findByStatus(EndorsementStatus status);
    List<Endorsement> findByStatusAndInsurerId(EndorsementStatus status, UUID insurerId);
    List<Endorsement> findByBatchId(UUID batchId);
    long countByEmployerIdAndStatus(UUID employerId, EndorsementStatus status);
    List<Endorsement> findByEmployerIdAndCreatedAtAfter(UUID employerId, java.time.Instant after);
}
