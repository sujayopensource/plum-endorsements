package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.InsurerConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataInsurerConfigurationRepository extends JpaRepository<InsurerConfigurationEntity, UUID> {
    Optional<InsurerConfigurationEntity> findByInsurerCode(String insurerCode);
    List<InsurerConfigurationEntity> findByActiveTrue();
}
