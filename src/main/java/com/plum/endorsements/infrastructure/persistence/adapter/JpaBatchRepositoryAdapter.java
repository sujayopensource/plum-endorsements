package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.BatchStatus;
import com.plum.endorsements.domain.model.EndorsementBatch;
import com.plum.endorsements.domain.port.BatchRepository;
import com.plum.endorsements.infrastructure.persistence.mapper.EndorsementMapper;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaBatchRepositoryAdapter implements BatchRepository {

    private final SpringDataBatchRepository springDataRepo;
    private final EndorsementMapper mapper;

    @Override
    public EndorsementBatch save(EndorsementBatch batch) {
        var entity = mapper.toEntity(batch);
        var saved = springDataRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<EndorsementBatch> findById(UUID id) {
        return springDataRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<EndorsementBatch> findByInsurerId(UUID insurerId) {
        return springDataRepo.findByInsurerId(insurerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<EndorsementBatch> findByStatus(BatchStatus status) {
        return springDataRepo.findByStatus(status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByInsurerIdAndStatusIn(UUID insurerId, List<BatchStatus> statuses) {
        List<String> statusStrings = statuses.stream().map(Enum::name).toList();
        return springDataRepo.existsByInsurerIdAndStatusIn(insurerId, statusStrings);
    }

    @Override
    public Page<EndorsementBatch> findByEmployerId(UUID employerId, Pageable pageable) {
        return springDataRepo.findByEmployerId(employerId, pageable).map(mapper::toDomain);
    }
}
