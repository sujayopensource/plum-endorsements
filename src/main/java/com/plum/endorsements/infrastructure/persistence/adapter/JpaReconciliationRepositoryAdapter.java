package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.ReconciliationItem;
import com.plum.endorsements.domain.model.ReconciliationOutcome;
import com.plum.endorsements.domain.model.ReconciliationRun;
import com.plum.endorsements.domain.port.ReconciliationRepository;
import com.plum.endorsements.infrastructure.persistence.mapper.EndorsementMapper;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataReconciliationItemRepository;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataReconciliationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaReconciliationRepositoryAdapter implements ReconciliationRepository {

    private final SpringDataReconciliationRunRepository runRepository;
    private final SpringDataReconciliationItemRepository itemRepository;
    private final EndorsementMapper mapper;

    @Override
    public ReconciliationRun saveRun(ReconciliationRun run) {
        return mapper.toDomain(runRepository.save(mapper.toEntity(run)));
    }

    @Override
    public ReconciliationItem saveItem(ReconciliationItem item) {
        return mapper.toDomain(itemRepository.save(mapper.toEntity(item)));
    }

    @Override
    public Optional<ReconciliationRun> findRunById(UUID runId) {
        return runRepository.findById(runId).map(mapper::toDomain);
    }

    @Override
    public List<ReconciliationRun> findRunsByInsurerId(UUID insurerId) {
        return runRepository.findByInsurerId(insurerId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<ReconciliationItem> findItemsByRunId(UUID runId) {
        return itemRepository.findByRunId(runId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<ReconciliationItem> findItemsByOutcome(UUID runId, ReconciliationOutcome outcome) {
        return itemRepository.findByRunIdAndOutcome(runId, outcome.name()).stream()
                .map(mapper::toDomain).toList();
    }
}
