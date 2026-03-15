package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.port.EndorsementRepository;
import com.plum.endorsements.infrastructure.persistence.mapper.EndorsementMapper;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataEndorsementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaEndorsementRepositoryAdapter implements EndorsementRepository {

    private final SpringDataEndorsementRepository springDataRepo;
    private final EndorsementMapper mapper;

    @Override
    public Endorsement save(Endorsement endorsement) {
        var entity = mapper.toEntity(endorsement);
        var saved = springDataRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Endorsement> findById(UUID id) {
        return springDataRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Endorsement> findByIdempotencyKey(String key) {
        return springDataRepo.findByIdempotencyKey(key).map(mapper::toDomain);
    }

    @Override
    public Page<Endorsement> findByEmployerId(UUID employerId, Pageable pageable) {
        return springDataRepo.findByEmployerId(employerId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Endorsement> findByEmployerIdAndStatusIn(UUID employerId, List<EndorsementStatus> statuses, Pageable pageable) {
        List<String> statusStrings = statuses.stream().map(Enum::name).toList();
        return springDataRepo.findByEmployerIdAndStatusIn(employerId, statusStrings, pageable).map(mapper::toDomain);
    }

    @Override
    public List<Endorsement> findByStatus(EndorsementStatus status) {
        return springDataRepo.findByStatus(status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Endorsement> findByStatusAndInsurerId(EndorsementStatus status, UUID insurerId) {
        return springDataRepo.findByStatusAndInsurerId(status.name(), insurerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Endorsement> findByBatchId(UUID batchId) {
        return springDataRepo.findByBatchId(batchId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByEmployerIdAndStatus(UUID employerId, EndorsementStatus status) {
        return springDataRepo.countByEmployerIdAndStatus(employerId, status.name());
    }

    @Override
    public List<Endorsement> findByEmployerIdAndCreatedAtAfter(UUID employerId, java.time.Instant after) {
        return springDataRepo.findByEmployerIdAndCreatedAtAfter(employerId, after)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
