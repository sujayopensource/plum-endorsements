package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.handler.ProcessEndorsementHandler;
import com.plum.endorsements.domain.model.BatchStatus;
import com.plum.endorsements.domain.model.EndorsementBatch;
import com.plum.endorsements.domain.port.BatchRepository;
import com.plum.endorsements.domain.port.InsurerPort;
import com.plum.endorsements.domain.port.NotificationPort;
import com.plum.endorsements.infrastructure.insurer.InsurerRouter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchStatusPollerScheduler (multi-insurer)")
class BatchStatusPollerSchedulerTest {

    @Mock private BatchRepository batchRepository;
    @Mock private InsurerRouter insurerRouter;
    @Mock private ProcessEndorsementHandler processHandler;
    @Mock private NotificationPort notificationPort;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private BatchStatusPollerScheduler scheduler;

    private UUID nivaInsurerId;
    private UUID bajajInsurerId;

    @BeforeEach
    void setUp() {
        scheduler = new BatchStatusPollerScheduler(
                batchRepository, insurerRouter, processHandler,
                notificationPort, meterRegistry);
        nivaInsurerId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        bajajInsurerId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    }

    private EndorsementBatch buildBatch(UUID insurerId, String ref) {
        return EndorsementBatch.builder()
                .id(UUID.randomUUID())
                .insurerId(insurerId)
                .status(BatchStatus.SUBMITTED)
                .endorsementCount(5)
                .submittedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .slaDeadline(Instant.now().plus(23, ChronoUnit.HOURS))
                .insurerBatchRef(ref)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("polls each batch using correct insurer adapter")
    void pollBatchStatuses_UsesCorrectAdapterPerBatch() {
        EndorsementBatch nivaBatch = buildBatch(nivaInsurerId, "NIVA-BATCH-001");
        EndorsementBatch bajajBatch = buildBatch(bajajInsurerId, "BAJAJ-BATCH-001");

        when(batchRepository.findByStatus(BatchStatus.SUBMITTED))
                .thenReturn(List.of(nivaBatch, bajajBatch));
        when(batchRepository.findByStatus(BatchStatus.PROCESSING))
                .thenReturn(List.of());

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.checkBatchStatus("NIVA-BATCH-001"))
                .thenReturn(new InsurerPort.BatchStatusResult("PROCESSING", List.of()));
        when(insurerRouter.resolve(nivaInsurerId)).thenReturn(nivaPort);

        InsurerPort bajajPort = mock(InsurerPort.class);
        when(bajajPort.checkBatchStatus("BAJAJ-BATCH-001"))
                .thenReturn(new InsurerPort.BatchStatusResult("COMPLETED", List.of()));
        when(insurerRouter.resolve(bajajInsurerId)).thenReturn(bajajPort);

        scheduler.pollBatchStatuses();

        verify(insurerRouter).resolve(nivaInsurerId);
        verify(insurerRouter).resolve(bajajInsurerId);
        verify(nivaPort).checkBatchStatus("NIVA-BATCH-001");
        verify(bajajPort).checkBatchStatus("BAJAJ-BATCH-001");
    }

    @Test
    @DisplayName("handles completed batch results with confirmations")
    void pollBatchStatuses_CompletedBatch_HandlesConfirmations() {
        UUID endorsementId = UUID.randomUUID();
        EndorsementBatch batch = buildBatch(bajajInsurerId, "BAJAJ-BATCH-002");

        when(batchRepository.findByStatus(BatchStatus.SUBMITTED))
                .thenReturn(List.of(batch));
        when(batchRepository.findByStatus(BatchStatus.PROCESSING))
                .thenReturn(List.of());

        InsurerPort bajajPort = mock(InsurerPort.class);
        when(bajajPort.checkBatchStatus("BAJAJ-BATCH-002"))
                .thenReturn(new InsurerPort.BatchStatusResult("COMPLETED", List.of(
                        new InsurerPort.EndorsementResult(endorsementId, true, "BAJAJ-REF-001", null)
                )));
        when(insurerRouter.resolve(bajajInsurerId)).thenReturn(bajajPort);

        scheduler.pollBatchStatuses();

        verify(processHandler).handleConfirmation(endorsementId, "BAJAJ-REF-001");
        verify(batchRepository).save(any(EndorsementBatch.class));
    }

    @Test
    @DisplayName("no-op when no active batches")
    void pollBatchStatuses_NoActiveBatches_NoOp() {
        when(batchRepository.findByStatus(BatchStatus.SUBMITTED)).thenReturn(List.of());
        when(batchRepository.findByStatus(BatchStatus.PROCESSING)).thenReturn(List.of());

        scheduler.pollBatchStatuses();

        verify(insurerRouter, never()).resolve(any());
    }

    @Test
    @DisplayName("notifies on SLA breach")
    void pollBatchStatuses_SlaBreached_Notifies() {
        EndorsementBatch batch = buildBatch(nivaInsurerId, "NIVA-BATCH-SLA");
        batch.setSlaDeadline(Instant.now().minus(1, ChronoUnit.HOURS)); // Already breached

        when(batchRepository.findByStatus(BatchStatus.SUBMITTED))
                .thenReturn(List.of(batch));
        when(batchRepository.findByStatus(BatchStatus.PROCESSING))
                .thenReturn(List.of());

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.checkBatchStatus("NIVA-BATCH-SLA"))
                .thenReturn(new InsurerPort.BatchStatusResult("PROCESSING", List.of()));
        when(insurerRouter.resolve(nivaInsurerId)).thenReturn(nivaPort);

        scheduler.pollBatchStatuses();

        verify(notificationPort).notifyBatchSlaBreached(batch.getId(), nivaInsurerId);
    }
}
