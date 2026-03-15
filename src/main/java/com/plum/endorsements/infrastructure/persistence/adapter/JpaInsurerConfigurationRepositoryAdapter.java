package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.port.InsurerConfigurationRepository;
import com.plum.endorsements.infrastructure.persistence.mapper.EndorsementMapper;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataInsurerConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaInsurerConfigurationRepositoryAdapter implements InsurerConfigurationRepository {

    private final SpringDataInsurerConfigurationRepository repository;
    private final EndorsementMapper mapper;

    @Override
    public Optional<InsurerConfiguration> findByInsurerId(UUID insurerId) {
        return repository.findById(insurerId).map(mapper::toDomain);
    }

    @Override
    public Optional<InsurerConfiguration> findByInsurerCode(String code) {
        return repository.findByInsurerCode(code).map(mapper::toDomain);
    }

    @Override
    public List<InsurerConfiguration> findAllActive() {
        return repository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public InsurerConfiguration save(InsurerConfiguration config) {
        return mapper.toDomain(repository.save(mapper.toEntity(config)));
    }
}
