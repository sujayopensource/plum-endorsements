package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.ProvisionalCoverage;
import com.plum.endorsements.domain.port.ProvisionalCoverageRepository;
import com.plum.endorsements.infrastructure.persistence.mapper.EndorsementMapper;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataProvisionalCoverageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaProvisionalCoverageRepositoryAdapter implements ProvisionalCoverageRepository {

    private final SpringDataProvisionalCoverageRepository springDataRepo;
    private final EndorsementMapper mapper;

    @Override
    public ProvisionalCoverage save(ProvisionalCoverage coverage) {
        var entity = mapper.toEntity(coverage);
        var saved = springDataRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProvisionalCoverage> findByEndorsementId(UUID endorsementId) {
        return springDataRepo.findByEndorsementId(endorsementId).map(mapper::toDomain);
    }

    @Override
    public List<ProvisionalCoverage> findActiveByEmployeeId(UUID employeeId) {
        return springDataRepo.findActiveByEmployeeId(employeeId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProvisionalCoverage> findStaleProvisionalCoverages(int maxDays) {
        Instant cutoff = Instant.now().minus(maxDays, ChronoUnit.DAYS);
        return springDataRepo.findStale(cutoff)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProvisionalCoverage> findActiveExpiringBefore(Instant warningCutoff, Instant staleCutoff) {
        return springDataRepo.findExpiringBetween(warningCutoff, staleCutoff)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
