package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.StpRateSnapshot;
import com.plum.endorsements.domain.port.StpRateSnapshotRepository;
import com.plum.endorsements.infrastructure.persistence.entity.StpRateSnapshotEntity;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataStpRateSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaStpRateSnapshotRepositoryAdapter implements StpRateSnapshotRepository {

    private final SpringDataStpRateSnapshotRepository springDataRepo;

    @Override
    public StpRateSnapshot save(StpRateSnapshot snapshot) {
        StpRateSnapshotEntity entity = toEntity(snapshot);
        StpRateSnapshotEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<StpRateSnapshot> findByInsurerIdAndDateRange(UUID insurerId, LocalDate from, LocalDate to) {
        return springDataRepo.findByInsurerIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(insurerId, from, to)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<StpRateSnapshot> findByInsurerIdAndDate(UUID insurerId, LocalDate date) {
        return springDataRepo.findByInsurerIdAndSnapshotDate(insurerId, date).map(this::toDomain);
    }

    private StpRateSnapshot toDomain(StpRateSnapshotEntity entity) {
        return StpRateSnapshot.builder()
                .id(entity.getId())
                .insurerId(entity.getInsurerId())
                .snapshotDate(entity.getSnapshotDate())
                .totalEndorsements(entity.getTotalEndorsements())
                .stpEndorsements(entity.getStpEndorsements())
                .stpRate(entity.getStpRate())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private StpRateSnapshotEntity toEntity(StpRateSnapshot domain) {
        return StpRateSnapshotEntity.builder()
                .id(domain.getId())
                .insurerId(domain.getInsurerId())
                .snapshotDate(domain.getSnapshotDate())
                .totalEndorsements(domain.getTotalEndorsements())
                .stpEndorsements(domain.getStpEndorsements())
                .stpRate(domain.getStpRate())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
