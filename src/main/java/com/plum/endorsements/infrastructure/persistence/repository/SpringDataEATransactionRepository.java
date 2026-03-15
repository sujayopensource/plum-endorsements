package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.EATransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SpringDataEATransactionRepository extends JpaRepository<EATransactionEntity, Long> {
    List<EATransactionEntity> findByEmployerIdAndInsurerIdOrderByCreatedAtDesc(UUID employerId, UUID insurerId);
}
