package com.plum.endorsements.application.service;

import com.plum.endorsements.application.handler.ProcessEndorsementHandler;
import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import com.plum.endorsements.infrastructure.insurer.InsurerRouter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationEngine")
class ReconciliationEngineTest {

    @Mock private EndorsementRepository endorsementRepository;
    @Mock private ReconciliationRepository reconciliationRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private InsurerRouter insurerRouter;
    @Mock private ProcessEndorsementHandler processHandler;
    @Mock private EventPublisher eventPublisher;
    @Mock private NotificationPort notificationPort;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private ReconciliationEngine engine;
    private UUID insurerId;

    @BeforeEach
    void setUp() {
        engine = new ReconciliationEngine(endorsementRepository, reconciliationRepository,
                batchRepository, insurerRouter, processHandler, eventPublisher, notificationPort, meterRegistry);
        insurerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    }

    private Endorsement buildProcessingEndorsement(String insurerRef, UUID batchId) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(insurerId)
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.INSURER_PROCESSING)
                .coverageStartDate(LocalDate.now().plusDays(1))
                .premiumAmount(new BigDecimal("1000.00"))
                .insurerReference(insurerRef)
                .batchId(batchId)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("MATCH: real-time insurer with valid reference confirms endorsement")
    void reconcileInsurer_RealTimeMatch_ConfirmsEndorsement() {
        Endorsement endorsement = buildProcessingEndorsement("ICICI-12345678", null);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(endorsement));

        InsurerPort iciciPort = mock(InsurerPort.class);
        when(iciciPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(true, false, 0, 0, 120));
        when(insurerRouter.resolve(insurerId)).thenReturn(iciciPort);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> {
            ReconciliationItem item = i.getArgument(0);
            if (item.getId() == null) item.setId(UUID.randomUUID());
            return item;
        });

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        assertThat(result.getMatched()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(processHandler).handleConfirmation(endorsement.getId(), "ICICI-12345678");
        verify(eventPublisher).publish(any(EndorsementEvent.ReconciliationMatched.class));
    }

    @Test
    @DisplayName("MISSING: endorsement without insurer reference flagged")
    void reconcileInsurer_MissingReference_FlagsEndorsement() {
        Endorsement endorsement = buildProcessingEndorsement(null, null);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(endorsement));

        InsurerPort port = mock(InsurerPort.class);
        when(insurerRouter.resolve(insurerId)).thenReturn(port);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> i.getArgument(0));

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        assertThat(result.getMissing()).isEqualTo(1);
        verify(eventPublisher).publish(any(EndorsementEvent.ReconciliationMissing.class));
        verify(notificationPort).notifyReconciliationDiscrepancy(eq(insurerId),
                eq(endorsement.getId()), any());
    }

    @Test
    @DisplayName("PARTIAL_MATCH: batch insurer with reference but no batch ID")
    void reconcileInsurer_PartialMatch_FlagsForReview() {
        Endorsement endorsement = buildProcessingEndorsement("NIVA-REF-001", null);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(endorsement));

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(false, true, 500, 24, 0));
        when(insurerRouter.resolve(insurerId)).thenReturn(nivaPort);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> i.getArgument(0));

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        assertThat(result.getPartialMatched()).isEqualTo(1);
        verify(eventPublisher).publish(any(EndorsementEvent.ReconciliationDiscrepancy.class));
    }

    @Test
    @DisplayName("MATCH: batch insurer confirmed by insurer checkBatchStatus")
    void reconcileInsurer_BatchMatch_ConfirmedByInsurer() {
        UUID batchId = UUID.randomUUID();
        Endorsement endorsement = buildProcessingEndorsement("NIVA-REF-002", batchId);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(endorsement));

        // Set up batch with insurer ref
        EndorsementBatch batch = EndorsementBatch.builder()
                .id(batchId).insurerId(insurerId).insurerBatchRef("INSURER-BATCH-REF-001")
                .status(BatchStatus.SUBMITTED).endorsementCount(1).build();
        when(batchRepository.findById(batchId)).thenReturn(java.util.Optional.of(batch));

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(false, true, 500, 24, 0));
        // Insurer confirms the endorsement
        when(nivaPort.checkBatchStatus("INSURER-BATCH-REF-001"))
                .thenReturn(new InsurerPort.BatchStatusResult("COMPLETE",
                        List.of(new InsurerPort.EndorsementResult(
                                endorsement.getId(), true, "NIVA-REF-002", null))));
        when(insurerRouter.resolve(insurerId)).thenReturn(nivaPort);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> i.getArgument(0));

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        assertThat(result.getMatched()).isEqualTo(1);
        verify(nivaPort).checkBatchStatus("INSURER-BATCH-REF-001");
        verify(processHandler).handleConfirmation(endorsement.getId(), "NIVA-REF-002");
        verify(eventPublisher).publish(any(EndorsementEvent.ReconciliationMatched.class));
    }

    @Test
    @DisplayName("INSURER_REJECTED: batch insurer rejects endorsement via checkBatchStatus")
    void reconcileInsurer_BatchInsurer_RejectedByInsurer() {
        UUID batchId = UUID.randomUUID();
        Endorsement endorsement = buildProcessingEndorsement("NIVA-REF-003", batchId);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(endorsement));

        EndorsementBatch batch = EndorsementBatch.builder()
                .id(batchId).insurerId(insurerId).insurerBatchRef("INSURER-BATCH-REF-002")
                .status(BatchStatus.SUBMITTED).endorsementCount(1).build();
        when(batchRepository.findById(batchId)).thenReturn(java.util.Optional.of(batch));

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(false, true, 500, 24, 0));
        when(nivaPort.checkBatchStatus("INSURER-BATCH-REF-002"))
                .thenReturn(new InsurerPort.BatchStatusResult("COMPLETE",
                        List.of(new InsurerPort.EndorsementResult(
                                endorsement.getId(), false, null, "Invalid member data"))));
        when(insurerRouter.resolve(insurerId)).thenReturn(nivaPort);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> i.getArgument(0));

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        assertThat(result.getRejected()).isEqualTo(1);
        verify(processHandler).handleRejection(endorsement.getId(), "Invalid member data");
        verify(eventPublisher).publish(any(EndorsementEvent.ReconciliationDiscrepancy.class));
    }

    @Test
    @DisplayName("PARTIAL_MATCH: endorsement not found in insurer batch results")
    void reconcileInsurer_BatchInsurer_NotFoundInInsurerResults() {
        UUID batchId = UUID.randomUUID();
        Endorsement endorsement = buildProcessingEndorsement("NIVA-REF-004", batchId);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(endorsement));

        EndorsementBatch batch = EndorsementBatch.builder()
                .id(batchId).insurerId(insurerId).insurerBatchRef("INSURER-BATCH-REF-003")
                .status(BatchStatus.SUBMITTED).endorsementCount(1).build();
        when(batchRepository.findById(batchId)).thenReturn(java.util.Optional.of(batch));

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(false, true, 500, 24, 0));
        // Insurer returns results but this endorsement is not in the list
        when(nivaPort.checkBatchStatus("INSURER-BATCH-REF-003"))
                .thenReturn(new InsurerPort.BatchStatusResult("COMPLETE",
                        List.of(new InsurerPort.EndorsementResult(
                                UUID.randomUUID(), true, "OTHER-REF", null))));
        when(insurerRouter.resolve(insurerId)).thenReturn(nivaPort);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> i.getArgument(0));

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        assertThat(result.getPartialMatched()).isEqualTo(1);
        verify(notificationPort).notifyReconciliationDiscrepancy(eq(insurerId),
                eq(endorsement.getId()), any());
    }

    @Test
    @DisplayName("falls back to local state when checkBatchStatus throws")
    void reconcileInsurer_BatchInsurer_CheckBatchStatusFails_FallsBack() {
        UUID batchId = UUID.randomUUID();
        Endorsement endorsement = buildProcessingEndorsement("NIVA-REF-005", batchId);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(endorsement));

        EndorsementBatch batch = EndorsementBatch.builder()
                .id(batchId).insurerId(insurerId).insurerBatchRef("INSURER-BATCH-REF-004")
                .status(BatchStatus.SUBMITTED).endorsementCount(1).build();
        when(batchRepository.findById(batchId)).thenReturn(java.util.Optional.of(batch));

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(false, true, 500, 24, 0));
        // checkBatchStatus throws exception
        when(nivaPort.checkBatchStatus("INSURER-BATCH-REF-004"))
                .thenThrow(new RuntimeException("Insurer API timeout"));
        when(insurerRouter.resolve(insurerId)).thenReturn(nivaPort);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> i.getArgument(0));

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        // Falls back to MATCH (local state) when insurer check fails
        assertThat(result.getMatched()).isEqualTo(1);
        verify(processHandler).handleConfirmation(endorsement.getId(), "NIVA-REF-005");
    }

    @Test
    @DisplayName("MISSING: endorsement with blank insurer reference treated as missing")
    void reconcileInsurer_BlankReference_FlagsAsMissing() {
        Endorsement endorsement = buildProcessingEndorsement("  ", null);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(endorsement));

        InsurerPort port = mock(InsurerPort.class);
        when(insurerRouter.resolve(insurerId)).thenReturn(port);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> i.getArgument(0));

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        assertThat(result.getMissing()).isEqualTo(1);
        verify(eventPublisher).publish(any(EndorsementEvent.ReconciliationMissing.class));
    }

    @Test
    @DisplayName("reconciles multiple endorsements with mixed outcomes in single run")
    void reconcileInsurer_MultipleEndorsements_MixedOutcomes() {
        Endorsement matched = buildProcessingEndorsement("ICICI-MATCH", null);
        Endorsement missing = buildProcessingEndorsement(null, null);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(matched, missing));

        InsurerPort iciciPort = mock(InsurerPort.class);
        when(iciciPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(true, false, 0, 0, 120));
        when(insurerRouter.resolve(insurerId)).thenReturn(iciciPort);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> i.getArgument(0));

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        assertThat(result.getMatched()).isEqualTo(1);
        assertThat(result.getMissing()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(reconciliationRepository, times(2)).saveItem(any(ReconciliationItem.class));
    }

    @Test
    @DisplayName("MATCH: batch insurer with batch but no insurer batch ref falls back to match")
    void reconcileInsurer_BatchInsurer_NoBatchRef_FallsBackToMatch() {
        UUID batchId = UUID.randomUUID();
        Endorsement endorsement = buildProcessingEndorsement("NIVA-REF-006", batchId);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of(endorsement));

        // Batch exists but has no insurer batch ref
        EndorsementBatch batch = EndorsementBatch.builder()
                .id(batchId).insurerId(insurerId).insurerBatchRef(null)
                .status(BatchStatus.SUBMITTED).endorsementCount(1).build();
        when(batchRepository.findById(batchId)).thenReturn(java.util.Optional.of(batch));

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(false, true, 500, 24, 0));
        when(insurerRouter.resolve(insurerId)).thenReturn(nivaPort);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(reconciliationRepository.saveItem(any())).thenAnswer(i -> i.getArgument(0));

        ReconciliationRun result = engine.reconcileInsurer(insurerId);

        // Falls back to MATCH because batch has no insurer batch ref to verify against
        assertThat(result.getMatched()).isEqualTo(1);
        verify(processHandler).handleConfirmation(endorsement.getId(), "NIVA-REF-006");
    }

    @Test
    @DisplayName("notifies on reconciliation complete with summary")
    void reconcileInsurer_NotifiesOnComplete() {
        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.INSURER_PROCESSING, insurerId))
                .thenReturn(List.of());

        InsurerPort port = mock(InsurerPort.class);
        when(insurerRouter.resolve(insurerId)).thenReturn(port);

        when(reconciliationRepository.saveRun(any())).thenAnswer(i -> {
            ReconciliationRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });

        engine.reconcileInsurer(insurerId);

        verify(notificationPort).notifyReconciliationComplete(eq(insurerId), any(), eq(0), eq(0));
    }
}
