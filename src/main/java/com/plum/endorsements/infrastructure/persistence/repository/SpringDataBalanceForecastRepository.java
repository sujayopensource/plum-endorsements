package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.BalanceForecastEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataBalanceForecastRepository extends JpaRepository<BalanceForecastEntity, UUID> {
    List<BalanceForecastEntity> findByEmployerId(UUID employerId);
    List<BalanceForecastEntity> findByEmployerIdAndInsurerId(UUID employerId, UUID insurerId);
    Optional<BalanceForecastEntity> findFirstByEmployerIdAndInsurerIdOrderByCreatedAtDesc(UUID employerId, UUID insurerId);
}
