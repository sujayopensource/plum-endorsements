package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import com.plum.endorsements.domain.service.EndorsementStateMachine;
import com.plum.endorsements.infrastructure.insurer.InsurerRouter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchAssemblyScheduler (multi-insurer)")
class BatchAssemblySchedulerTest {

    @Mock private EndorsementRepository endorsementRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private EAAccountRepository eaAccountRepository;
    @Mock private InsurerRouter insurerRouter;
    @Spy private EndorsementStateMachine stateMachine = new EndorsementStateMachine();
    @Mock private EventPublisher eventPublisher;
    @Mock private BatchOptimizerPort batchOptimizer;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private BatchAssemblyScheduler scheduler;

    private UUID nivaInsurerId;
    private UUID bajajInsurerId;

    @BeforeEach
    void setUp() {
        scheduler = new BatchAssemblyScheduler(
                endorsementRepository, batchRepository, eaAccountRepository,
                insurerRouter, stateMachine, eventPublisher, batchOptimizer, meterRegistry);
        nivaInsurerId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        bajajInsurerId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    }

    private Endorsement buildQueuedEndorsement(UUID insurerId) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(insurerId)
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.QUEUED_FOR_BATCH)
                .coverageStartDate(LocalDate.now().plusDays(1))
                .premiumAmount(new BigDecimal("500.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("assembles separate batches per insurer")
    void assembleAndSubmitBatches_SeparateBatchesPerInsurer() {
        List<Endorsement> nivaEndorsements = List.of(
                buildQueuedEndorsement(nivaInsurerId),
                buildQueuedEndorsement(nivaInsurerId)
        );
        List<Endorsement> bajajEndorsements = List.of(
                buildQueuedEndorsement(bajajInsurerId)
        );

        List<Endorsement> allQueued = new ArrayList<>();
        allQueued.addAll(nivaEndorsements);
        allQueued.addAll(bajajEndorsements);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.QUEUED_FOR_BATCH, null))
                .thenReturn(allQueued);

        // No active batches for either insurer
        when(batchRepository.existsByInsurerIdAndStatusIn(any(), any())).thenReturn(false);

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(false, true, 500, 24, 0));
        when(nivaPort.submitBatch(any(), any())).thenReturn("NIVA-BATCH-001");
        when(insurerRouter.resolve(nivaInsurerId)).thenReturn(nivaPort);

        InsurerPort bajajPort = mock(InsurerPort.class);
        when(bajajPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(true, true, 200, 4, 30));
        when(bajajPort.submitBatch(any(), any())).thenReturn("BAJAJ-BATCH-001");
        when(insurerRouter.resolve(bajajInsurerId)).thenReturn(bajajPort);

        when(batchRepository.save(any())).thenAnswer(i -> {
            EndorsementBatch b = i.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.assembleAndSubmitBatches();

        verify(insurerRouter).resolve(nivaInsurerId);
        verify(insurerRouter).resolve(bajajInsurerId);
        verify(nivaPort).submitBatch(any(), any());
        verify(bajajPort).submitBatch(any(), any());
        // 2 batches created (1 per insurer), each saved twice (assembling + submitted)
        verify(batchRepository, times(4)).save(any(EndorsementBatch.class));
    }

    @Test
    @DisplayName("respects max batch size per insurer")
    void assembleAndSubmitBatches_RespectsMaxBatchSize() {
        // Create 3 endorsements for bajaj (max batch size = 2 for this test)
        List<Endorsement> bajajEndorsements = List.of(
                buildQueuedEndorsement(bajajInsurerId),
                buildQueuedEndorsement(bajajInsurerId),
                buildQueuedEndorsement(bajajInsurerId)
        );

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.QUEUED_FOR_BATCH, null))
                .thenReturn(bajajEndorsements);

        // No active batch
        when(batchRepository.existsByInsurerIdAndStatusIn(any(), any())).thenReturn(false);

        InsurerPort bajajPort = mock(InsurerPort.class);
        when(bajajPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(true, true, 2, 4, 30));
        when(bajajPort.submitBatch(any(), any())).thenReturn("BAJAJ-BATCH-REF");
        when(insurerRouter.resolve(bajajInsurerId)).thenReturn(bajajPort);

        when(batchRepository.save(any())).thenAnswer(i -> {
            EndorsementBatch b = i.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.assembleAndSubmitBatches();

        // Should create 2 batches (2 + 1)
        verify(bajajPort, times(2)).submitBatch(any(), any());
    }

    @Test
    @DisplayName("no-op when no queued endorsements")
    void assembleAndSubmitBatches_NoQueuedEndorsements_NoOp() {
        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.QUEUED_FOR_BATCH, null))
                .thenReturn(List.of());

        scheduler.assembleAndSubmitBatches();

        verify(insurerRouter, never()).resolve(any());
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("skips insurer with active batch, processes others")
    void assembleAndSubmitBatches_SkipsInsurerWithActiveBatch() {
        // Arrange: both insurers have queued endorsements
        List<Endorsement> nivaEndorsements = List.of(buildQueuedEndorsement(nivaInsurerId));
        List<Endorsement> bajajEndorsements = List.of(buildQueuedEndorsement(bajajInsurerId));

        List<Endorsement> allQueued = new ArrayList<>();
        allQueued.addAll(nivaEndorsements);
        allQueued.addAll(bajajEndorsements);

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.QUEUED_FOR_BATCH, null))
                .thenReturn(allQueued);

        // Niva has an active SUBMITTED batch — should be skipped
        when(batchRepository.existsByInsurerIdAndStatusIn(eq(nivaInsurerId), any())).thenReturn(true);
        // Bajaj has no active batch — should proceed
        when(batchRepository.existsByInsurerIdAndStatusIn(eq(bajajInsurerId), any())).thenReturn(false);

        InsurerPort bajajPort = mock(InsurerPort.class);
        when(bajajPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(true, true, 200, 4, 30));
        when(bajajPort.submitBatch(any(), any())).thenReturn("BAJAJ-BATCH-001");
        when(insurerRouter.resolve(bajajInsurerId)).thenReturn(bajajPort);

        when(batchRepository.save(any())).thenAnswer(i -> {
            EndorsementBatch b = i.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        scheduler.assembleAndSubmitBatches();

        // Assert: only bajaj's batch was submitted
        verify(insurerRouter, never()).resolve(nivaInsurerId);
        verify(insurerRouter).resolve(bajajInsurerId);
        verify(bajajPort).submitBatch(any(), any());
    }

    @Test
    @DisplayName("proceeds when previous batch is complete or failed")
    void assembleAndSubmitBatches_ProceedsWhenPreviousBatchComplete() {
        // Arrange: insurer has only COMPLETE/FAILED batches (not in active statuses)
        List<Endorsement> nivaEndorsements = List.of(buildQueuedEndorsement(nivaInsurerId));

        when(endorsementRepository.findByStatusAndInsurerId(EndorsementStatus.QUEUED_FOR_BATCH, null))
                .thenReturn(nivaEndorsements);

        // No active batch (existsBy returns false)
        when(batchRepository.existsByInsurerIdAndStatusIn(eq(nivaInsurerId), any())).thenReturn(false);

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(false, true, 500, 24, 0));
        when(nivaPort.submitBatch(any(), any())).thenReturn("NIVA-BATCH-001");
        when(insurerRouter.resolve(nivaInsurerId)).thenReturn(nivaPort);

        when(batchRepository.save(any())).thenAnswer(i -> {
            EndorsementBatch b = i.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        scheduler.assembleAndSubmitBatches();

        // Assert: batch was assembled and submitted
        verify(nivaPort).submitBatch(any(), any());
        verify(batchRepository, atLeast(2)).save(any(EndorsementBatch.class));
    }
}
