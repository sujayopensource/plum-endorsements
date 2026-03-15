package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.EndorsementBatch;
import com.plum.endorsements.domain.model.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface BatchRepository {
    EndorsementBatch save(EndorsementBatch batch);
    Optional<EndorsementBatch> findById(UUID id);
    List<EndorsementBatch> findByInsurerId(UUID insurerId);
    List<EndorsementBatch> findByStatus(BatchStatus status);
    boolean existsByInsurerIdAndStatusIn(UUID insurerId, List<BatchStatus> statuses);
    Page<EndorsementBatch> findByEmployerId(UUID employerId, Pageable pageable);
}
