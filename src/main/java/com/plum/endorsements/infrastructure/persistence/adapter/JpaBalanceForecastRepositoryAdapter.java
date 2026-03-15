package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.BalanceForecastRecord;
import com.plum.endorsements.domain.port.BalanceForecastRepository;
import com.plum.endorsements.infrastructure.persistence.entity.BalanceForecastEntity;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataBalanceForecastRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaBalanceForecastRepositoryAdapter implements BalanceForecastRepository {

    private final SpringDataBalanceForecastRepository springDataRepo;

    @Override
    public BalanceForecastRecord save(BalanceForecastRecord forecast) {
        BalanceForecastEntity entity = toEntity(forecast);
        BalanceForecastEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<BalanceForecastRecord> findById(UUID id) {
        return springDataRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<BalanceForecastRecord> findLatestByEmployerIdAndInsurerId(UUID employerId, UUID insurerId) {
        return springDataRepo.findFirstByEmployerIdAndInsurerIdOrderByCreatedAtDesc(employerId, insurerId)
                .map(this::toDomain);
    }

    @Override
    public List<BalanceForecastRecord> findByEmployerId(UUID employerId) {
        return springDataRepo.findByEmployerId(employerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<BalanceForecastRecord> findByEmployerIdAndInsurerId(UUID employerId, UUID insurerId) {
        return springDataRepo.findByEmployerIdAndInsurerId(employerId, insurerId)
                .stream().map(this::toDomain).toList();
    }

    private BalanceForecastRecord toDomain(BalanceForecastEntity entity) {
        return BalanceForecastRecord.builder()
                .id(entity.getId())
                .employerId(entity.getEmployerId())
                .insurerId(entity.getInsurerId())
                .forecastDate(entity.getForecastDate())
                .forecastedAmount(entity.getForecastedAmount())
                .actualAmount(entity.getActualAmount())
                .accuracy(entity.getAccuracy())
                .narrative(entity.getNarrative())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private BalanceForecastEntity toEntity(BalanceForecastRecord domain) {
        return BalanceForecastEntity.builder()
                .id(domain.getId())
                .employerId(domain.getEmployerId())
                .insurerId(domain.getInsurerId())
                .forecastDate(domain.getForecastDate())
                .forecastedAmount(domain.getForecastedAmount())
                .actualAmount(domain.getActualAmount())
                .accuracy(domain.getAccuracy())
                .narrative(domain.getNarrative())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
