package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.InsurerConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsurerConfigurationRepository {
    Optional<InsurerConfiguration> findByInsurerId(UUID insurerId);
    Optional<InsurerConfiguration> findByInsurerCode(String code);
    List<InsurerConfiguration> findAllActive();
    InsurerConfiguration save(InsurerConfiguration config);
}
