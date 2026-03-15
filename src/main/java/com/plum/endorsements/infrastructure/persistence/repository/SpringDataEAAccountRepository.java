package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.EAAccountEntity;
import com.plum.endorsements.infrastructure.persistence.entity.EAAccountId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface SpringDataEAAccountRepository extends JpaRepository<EAAccountEntity, EAAccountId> {
    Optional<EAAccountEntity> findByEmployerIdAndInsurerId(UUID employerId, UUID insurerId);
    List<EAAccountEntity> findByEmployerId(UUID employerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM EAAccountEntity a WHERE a.employerId = :employerId AND a.insurerId = :insurerId")
    Optional<EAAccountEntity> findByEmployerIdAndInsurerIdForUpdate(
            @Param("employerId") UUID employerId, @Param("insurerId") UUID insurerId);
}
