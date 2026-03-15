package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.ErrorResolution;
import com.plum.endorsements.domain.port.ErrorResolutionRepository;
import com.plum.endorsements.infrastructure.persistence.entity.ErrorResolutionEntity;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataErrorResolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaErrorResolutionRepositoryAdapter implements ErrorResolutionRepository {

    private final SpringDataErrorResolutionRepository springDataRepo;

    @Override
    public ErrorResolution save(ErrorResolution resolution) {
        ErrorResolutionEntity entity = toEntity(resolution);
        ErrorResolutionEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ErrorResolution> findById(UUID id) {
        return springDataRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<ErrorResolution> findByEndorsementId(UUID endorsementId) {
        return springDataRepo.findByEndorsementId(endorsementId).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByAutoApplied(boolean autoApplied) {
        return springDataRepo.countByAutoApplied(autoApplied);
    }

    @Override
    public long count() {
        return springDataRepo.count();
    }

    @Override
    public List<ErrorResolution> findByEndorsementIdAndOutcomeIsNull(UUID endorsementId) {
        return springDataRepo.findByEndorsementIdAndOutcomeIsNull(endorsementId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countByOutcome(String outcome) {
        return springDataRepo.countByOutcome(outcome);
    }

    private ErrorResolution toDomain(ErrorResolutionEntity entity) {
        return ErrorResolution.builder()
                .id(entity.getId())
                .endorsementId(entity.getEndorsementId())
                .errorType(entity.getErrorType())
                .originalValue(entity.getOriginalValue())
                .correctedValue(entity.getCorrectedValue())
                .resolution(entity.getResolution())
                .confidence(entity.getConfidence())
                .autoApplied(entity.isAutoApplied())
                .createdAt(entity.getCreatedAt())
                .outcome(entity.getOutcome())
                .outcomeAt(entity.getOutcomeAt())
                .outcomeEndorsementStatus(entity.getOutcomeEndorsementStatus())
                .build();
    }

    private ErrorResolutionEntity toEntity(ErrorResolution domain) {
        return ErrorResolutionEntity.builder()
                .id(domain.getId())
                .endorsementId(domain.getEndorsementId())
                .errorType(domain.getErrorType())
                .originalValue(domain.getOriginalValue())
                .correctedValue(domain.getCorrectedValue())
                .resolution(domain.getResolution())
                .confidence(domain.getConfidence())
                .autoApplied(domain.isAutoApplied())
                .createdAt(domain.getCreatedAt())
                .outcome(domain.getOutcome())
                .outcomeAt(domain.getOutcomeAt())
                .outcomeEndorsementStatus(domain.getOutcomeEndorsementStatus())
                .build();
    }
}
