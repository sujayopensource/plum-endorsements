package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.ProcessMiningMetric;
import com.plum.endorsements.domain.port.ProcessMiningRepository;
import com.plum.endorsements.infrastructure.persistence.entity.ProcessMiningMetricEntity;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataProcessMiningMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaProcessMiningRepositoryAdapter implements ProcessMiningRepository {

    private final SpringDataProcessMiningMetricRepository springDataRepo;

    @Override
    public ProcessMiningMetric save(ProcessMiningMetric metric) {
        ProcessMiningMetricEntity entity = toEntity(metric);
        ProcessMiningMetricEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ProcessMiningMetric> saveAll(List<ProcessMiningMetric> metrics) {
        List<ProcessMiningMetricEntity> entities = metrics.stream().map(this::toEntity).toList();
        return springDataRepo.saveAll(entities).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ProcessMiningMetric> findById(UUID id) {
        return springDataRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProcessMiningMetric> findByInsurerId(UUID insurerId) {
        return springDataRepo.findByInsurerId(insurerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ProcessMiningMetric> findLatestByInsurerId(UUID insurerId) {
        return springDataRepo.findByInsurerIdOrderByCalculatedAtDesc(insurerId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByInsurerId(UUID insurerId) {
        springDataRepo.deleteByInsurerId(insurerId);
    }

    private ProcessMiningMetric toDomain(ProcessMiningMetricEntity entity) {
        return ProcessMiningMetric.builder()
                .id(entity.getId())
                .insurerId(entity.getInsurerId())
                .fromStatus(entity.getFromStatus())
                .toStatus(entity.getToStatus())
                .avgDurationMs(entity.getAvgDurationMs())
                .p95DurationMs(entity.getP95DurationMs())
                .p99DurationMs(entity.getP99DurationMs())
                .sampleCount(entity.getSampleCount())
                .happyPathPct(entity.getHappyPathPct())
                .calculatedAt(entity.getCalculatedAt())
                .build();
    }

    private ProcessMiningMetricEntity toEntity(ProcessMiningMetric domain) {
        return ProcessMiningMetricEntity.builder()
                .id(domain.getId())
                .insurerId(domain.getInsurerId())
                .fromStatus(domain.getFromStatus())
                .toStatus(domain.getToStatus())
                .avgDurationMs(domain.getAvgDurationMs())
                .p95DurationMs(domain.getP95DurationMs())
                .p99DurationMs(domain.getP99DurationMs())
                .sampleCount(domain.getSampleCount())
                .happyPathPct(domain.getHappyPathPct())
                .calculatedAt(domain.getCalculatedAt())
                .build();
    }
}
