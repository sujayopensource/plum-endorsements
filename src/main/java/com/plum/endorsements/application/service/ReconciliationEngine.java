package com.plum.endorsements.application.service;

import com.plum.endorsements.application.handler.ProcessEndorsementHandler;
import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import com.plum.endorsements.infrastructure.insurer.InsurerRouter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationEngine {

    private final EndorsementRepository endorsementRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final BatchRepository batchRepository;
    private final InsurerRouter insurerRouter;
    private final ProcessEndorsementHandler processHandler;
    private final EventPublisher eventPublisher;
    private final NotificationPort notificationPort;
    private final MeterRegistry meterRegistry;

    @Transactional
    public ReconciliationRun reconcileInsurer(UUID insurerId) {
        log.info("Starting reconciliation for insurer {}", insurerId);

        ReconciliationRun run = ReconciliationRun.builder()
                .insurerId(insurerId)
                .status("RUNNING")
                .startedAt(Instant.now())
                .build();
        run = reconciliationRepository.saveRun(run);

        List<Endorsement> processingEndorsements = endorsementRepository
                .findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId);

        InsurerPort insurerPort = insurerRouter.resolve(insurerId);

        for (Endorsement endorsement : processingEndorsements) {
            reconcileEndorsement(run, endorsement, insurerPort);
        }

        run.complete();
        run = reconciliationRepository.saveRun(run);

        int discrepancies = run.getPartialMatched() + run.getRejected() + run.getMissing();
        notificationPort.notifyReconciliationComplete(insurerId, run.getId(),
                run.getMatched(), discrepancies);

        meterRegistry.counter("endorsement.reconciliation.completed",
                "insurerId", insurerId.toString()).increment();
        meterRegistry.gauge("endorsement.reconciliation.matched", run.getMatched());
        meterRegistry.gauge("endorsement.reconciliation.discrepancies", discrepancies);

        log.info("Reconciliation for insurer {} completed: {} checked, {} matched, {} discrepancies",
                insurerId, run.getTotalChecked(), run.getMatched(), discrepancies);

        return run;
    }

    private void reconcileEndorsement(ReconciliationRun run, Endorsement endorsement,
                                       InsurerPort insurerPort) {
        String insurerRef = endorsement.getInsurerReference();

        if (insurerRef == null || insurerRef.isBlank()) {
            // No insurer reference — this is a missing confirmation
            run.incrementMissing();

            ReconciliationItem item = ReconciliationItem.builder()
                    .runId(run.getId())
                    .endorsementId(endorsement.getId())
                    .batchId(endorsement.getBatchId())
                    .insurerId(endorsement.getInsurerId())
                    .employerId(endorsement.getEmployerId())
                    .outcome(ReconciliationOutcome.MISSING)
                    .actionTaken("Flagged for manual review")
                    .build();
            reconciliationRepository.saveItem(item);

            eventPublisher.publish(new EndorsementEvent.ReconciliationMissing(
                    endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                    endorsement.getInsurerId()));

            notificationPort.notifyReconciliationDiscrepancy(endorsement.getInsurerId(),
                    endorsement.getId(), "No insurer reference found");

            log.warn("Reconciliation: endorsement {} missing insurer reference", endorsement.getId());
            return;
        }

        // Check batch status from insurer to verify endorsement state
        InsurerPort.InsurerCapabilities caps = insurerPort.getCapabilities();
        ReconciliationOutcome outcome;
        String actionTaken;

        if (caps.supportsRealTime()) {
            // For real-time insurers, if we have a reference it should be confirmed
            outcome = ReconciliationOutcome.MATCH;
            actionTaken = "Confirmed via insurer reference";

            processHandler.handleConfirmation(endorsement.getId(), insurerRef);
            run.incrementMatched();

            eventPublisher.publish(new EndorsementEvent.ReconciliationMatched(
                    endorsement.getId(), Instant.now(), endorsement.getEmployerId(), insurerRef));
        } else {
            // For batch insurers, verify against insurer records
            if (endorsement.getBatchId() != null) {
                var result = verifyWithInsurer(endorsement, insurerPort);
                outcome = result.outcome();
                actionTaken = result.actionTaken();

                switch (outcome) {
                    case MATCH -> {
                        processHandler.handleConfirmation(endorsement.getId(), insurerRef);
                        run.incrementMatched();
                        eventPublisher.publish(new EndorsementEvent.ReconciliationMatched(
                                endorsement.getId(), Instant.now(), endorsement.getEmployerId(), insurerRef));
                    }
                    case INSURER_REJECTED -> {
                        String reason = result.rejectionReason() != null
                                ? result.rejectionReason() : "Rejected by insurer during reconciliation";
                        processHandler.handleRejection(endorsement.getId(), reason);
                        run.incrementRejected();
                        eventPublisher.publish(new EndorsementEvent.ReconciliationDiscrepancy(
                                endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                                "Insurer rejected endorsement: " + reason));
                        notificationPort.notifyReconciliationDiscrepancy(endorsement.getInsurerId(),
                                endorsement.getId(), "Insurer rejected: " + reason);
                    }
                    case PARTIAL_MATCH -> {
                        run.incrementPartialMatched();
                        eventPublisher.publish(new EndorsementEvent.ReconciliationDiscrepancy(
                                endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                                "Endorsement sent but not found in insurer records"));
                        notificationPort.notifyReconciliationDiscrepancy(endorsement.getInsurerId(),
                                endorsement.getId(), "Not found in insurer batch results");
                    }
                    default -> run.incrementPartialMatched();
                }
            } else {
                outcome = ReconciliationOutcome.PARTIAL_MATCH;
                actionTaken = "Has reference but no batch — flagged for review";
                run.incrementPartialMatched();

                eventPublisher.publish(new EndorsementEvent.ReconciliationDiscrepancy(
                        endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                        "Has insurer reference but no batch association"));

                notificationPort.notifyReconciliationDiscrepancy(endorsement.getInsurerId(),
                        endorsement.getId(), "Partial match — has reference but missing batch");
            }
        }

        ReconciliationItem item = ReconciliationItem.builder()
                .runId(run.getId())
                .endorsementId(endorsement.getId())
                .batchId(endorsement.getBatchId())
                .insurerId(endorsement.getInsurerId())
                .employerId(endorsement.getEmployerId())
                .outcome(outcome)
                .actionTaken(actionTaken)
                .build();
        reconciliationRepository.saveItem(item);
    }

    private InsurerVerificationResult verifyWithInsurer(Endorsement endorsement, InsurerPort insurerPort) {
        try {
            var batch = batchRepository.findById(endorsement.getBatchId()).orElse(null);
            if (batch == null || batch.getInsurerBatchRef() == null) {
                return new InsurerVerificationResult(ReconciliationOutcome.MATCH,
                        "Matched via batch reference (no insurer batch ref to verify)", null);
            }

            InsurerPort.BatchStatusResult batchStatus = insurerPort.checkBatchStatus(batch.getInsurerBatchRef());
            if (batchStatus == null || batchStatus.results() == null) {
                return new InsurerVerificationResult(ReconciliationOutcome.MATCH,
                        "Matched via batch reference (insurer returned no detail)", null);
            }

            var match = batchStatus.results().stream()
                    .filter(r -> endorsement.getId().equals(r.endorsementId()))
                    .findFirst();

            if (match.isPresent()) {
                if (match.get().confirmed()) {
                    return new InsurerVerificationResult(ReconciliationOutcome.MATCH,
                            "Confirmed by insurer via checkBatchStatus", null);
                } else {
                    return new InsurerVerificationResult(ReconciliationOutcome.INSURER_REJECTED,
                            "Rejected by insurer: " + match.get().rejectionReason(),
                            match.get().rejectionReason());
                }
            } else {
                return new InsurerVerificationResult(ReconciliationOutcome.PARTIAL_MATCH,
                        "Endorsement sent but not found in insurer batch results", null);
            }
        } catch (Exception e) {
            log.warn("Failed to verify endorsement {} with insurer, falling back to local state: {}",
                    endorsement.getId(), e.getMessage());
            return new InsurerVerificationResult(ReconciliationOutcome.MATCH,
                    "Matched via batch reference (insurer check failed, local fallback)", null);
        }
    }

    private record InsurerVerificationResult(ReconciliationOutcome outcome, String actionTaken,
                                              String rejectionReason) {}
}
