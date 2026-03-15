package com.plum.endorsements.application.handler;

import com.plum.endorsements.application.exception.EndorsementNotFoundException;
import com.plum.endorsements.application.exception.InsurerNotFoundException;
import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import com.plum.endorsements.domain.service.InsurerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EndorsementQueryHandler {

    private final EndorsementRepository endorsementRepository;
    private final BatchRepository batchRepository;
    private final EAAccountRepository eaAccountRepository;
    private final ProvisionalCoverageRepository provisionalCoverageRepository;
    private final InsurerRegistry insurerRegistry;
    private final ReconciliationRepository reconciliationRepository;

    public Endorsement findById(UUID id) {
        return endorsementRepository.findById(id)
                .orElseThrow(() -> new EndorsementNotFoundException(id));
    }

    public Page<Endorsement> findByEmployerId(UUID employerId, Pageable pageable) {
        return endorsementRepository.findByEmployerId(employerId, pageable);
    }

    public Page<Endorsement> findByEmployerIdAndStatuses(UUID employerId, List<EndorsementStatus> statuses,
                                                          Pageable pageable) {
        return endorsementRepository.findByEmployerIdAndStatusIn(employerId, statuses, pageable);
    }

    public Page<Endorsement> findOutstandingByEmployerId(UUID employerId, Pageable pageable) {
        List<EndorsementStatus> outstandingStatuses = List.of(
                EndorsementStatus.CREATED,
                EndorsementStatus.VALIDATED,
                EndorsementStatus.PROVISIONALLY_COVERED,
                EndorsementStatus.SUBMITTED_REALTIME,
                EndorsementStatus.QUEUED_FOR_BATCH,
                EndorsementStatus.BATCH_SUBMITTED,
                EndorsementStatus.INSURER_PROCESSING,
                EndorsementStatus.RETRY_PENDING
        );
        return endorsementRepository.findByEmployerIdAndStatusIn(employerId, outstandingStatuses, pageable);
    }

    public Page<EndorsementBatch> findBatchesByEmployerId(UUID employerId, Pageable pageable) {
        return batchRepository.findByEmployerId(employerId, pageable);
    }

    public Optional<EndorsementBatch> findBatchById(UUID batchId) {
        return batchRepository.findById(batchId);
    }

    public Optional<EAAccount> findEAAccount(UUID employerId, UUID insurerId) {
        return eaAccountRepository.findByEmployerIdAndInsurerId(employerId, insurerId);
    }

    public Optional<ProvisionalCoverage> findProvisionalCoverage(UUID endorsementId) {
        return provisionalCoverageRepository.findByEndorsementId(endorsementId);
    }

    // --- Insurer Configuration queries ---

    public List<InsurerConfiguration> findAllActiveInsurers() {
        return insurerRegistry.getAllActiveInsurers();
    }

    public InsurerConfiguration findInsurerById(UUID insurerId) {
        return insurerRegistry.getConfiguration(insurerId);
    }

    // --- Reconciliation queries ---

    public List<ReconciliationRun> findReconciliationRuns(UUID insurerId) {
        return reconciliationRepository.findRunsByInsurerId(insurerId);
    }

    public ReconciliationRun findReconciliationRunById(UUID runId) {
        return reconciliationRepository.findRunById(runId)
                .orElseThrow(() -> new RuntimeException("Reconciliation run not found: " + runId));
    }

    public List<ReconciliationItem> findReconciliationItems(UUID runId) {
        return reconciliationRepository.findItemsByRunId(runId);
    }
}
