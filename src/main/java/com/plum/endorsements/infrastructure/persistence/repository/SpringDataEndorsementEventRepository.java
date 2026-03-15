package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.EndorsementEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SpringDataEndorsementEventRepository extends JpaRepository<EndorsementEventEntity, Long> {
    List<EndorsementEventEntity> findByEndorsementIdOrderByCreatedAtAsc(UUID endorsementId);
}
