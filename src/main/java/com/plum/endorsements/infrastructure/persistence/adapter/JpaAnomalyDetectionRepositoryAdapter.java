package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.AnomalyDetection;
import com.plum.endorsements.domain.model.AnomalyStatus;
import com.plum.endorsements.domain.model.AnomalyType;
import com.plum.endorsements.domain.port.AnomalyDetectionRepository;
import com.plum.endorsements.infrastructure.persistence.entity.AnomalyDetectionEntity;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataAnomalyDetectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaAnomalyDetectionRepositoryAdapter implements AnomalyDetectionRepository {

    private final SpringDataAnomalyDetectionRepository springDataRepo;

    @Override
    public AnomalyDetection save(AnomalyDetection anomaly) {
        AnomalyDetectionEntity entity = toEntity(anomaly);
        AnomalyDetectionEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<AnomalyDetection> findById(UUID id) {
        return springDataRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<AnomalyDetection> findByEmployerId(UUID employerId) {
        return springDataRepo.findByEmployerId(employerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<AnomalyDetection> findByStatus(AnomalyStatus status) {
        return springDataRepo.findByStatus(status.name()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<AnomalyDetection> findByEmployerIdAndFlaggedAtAfter(UUID employerId, Instant after) {
        return springDataRepo.findByEmployerIdAndFlaggedAtAfter(employerId, after)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countByEmployerIdAndFlaggedAtAfter(UUID employerId, Instant after) {
        return springDataRepo.countByEmployerIdAndFlaggedAtAfter(employerId, after);
    }

    @Override
    public long countByAnomalyTypeAndStatus(AnomalyType anomalyType, AnomalyStatus status) {
        return springDataRepo.countByAnomalyTypeAndStatus(anomalyType.name(), status.name());
    }

    @Override
    public long countByAnomalyType(AnomalyType anomalyType) {
        return springDataRepo.countByAnomalyType(anomalyType.name());
    }

    private AnomalyDetection toDomain(AnomalyDetectionEntity entity) {
        return AnomalyDetection.builder()
                .id(entity.getId())
                .endorsementId(entity.getEndorsementId())
                .employerId(entity.getEmployerId())
                .anomalyType(AnomalyType.valueOf(entity.getAnomalyType()))
                .score(entity.getScore())
                .explanation(entity.getExplanation())
                .flaggedAt(entity.getFlaggedAt())
                .reviewedAt(entity.getReviewedAt())
                .status(AnomalyStatus.valueOf(entity.getStatus()))
                .reviewerNotes(entity.getReviewerNotes())
                .build();
    }

    private AnomalyDetectionEntity toEntity(AnomalyDetection domain) {
        return AnomalyDetectionEntity.builder()
                .id(domain.getId())
                .endorsementId(domain.getEndorsementId())
                .employerId(domain.getEmployerId())
                .anomalyType(domain.getAnomalyType().name())
                .score(domain.getScore())
                .explanation(domain.getExplanation())
                .flaggedAt(domain.getFlaggedAt())
                .reviewedAt(domain.getReviewedAt())
                .status(domain.getStatus().name())
                .reviewerNotes(domain.getReviewerNotes())
                .build();
    }
}
