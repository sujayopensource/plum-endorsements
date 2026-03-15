package com.plum.endorsements.application.handler;

import com.plum.endorsements.application.service.ErrorResolutionService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessEndorsementHandler (multi-insurer)")
class ProcessEndorsementHandlerTest {

    @Mock private EndorsementRepository endorsementRepository;
    @Spy private EndorsementStateMachine stateMachine = new EndorsementStateMachine();
    @Mock private InsurerRouter insurerRouter;
    @Mock private EventPublisher eventPublisher;
    @Mock private NotificationPort notificationPort;
    @Mock private ProvisionalCoverageRepository provisionalCoverageRepository;
    @Mock private EAAccountRepository eaAccountRepository;
    @Mock private ErrorResolutionService errorResolutionService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private ProcessEndorsementHandler handler;

    private UUID mockInsurerId;
    private UUID iciciInsurerId;
    private UUID nivaInsurerId;

    @BeforeEach
    void setUp() {
        handler = new ProcessEndorsementHandler(
                endorsementRepository, stateMachine, insurerRouter,
                eventPublisher, notificationPort, provisionalCoverageRepository,
                eaAccountRepository, errorResolutionService, meterRegistry);
        mockInsurerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        iciciInsurerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        nivaInsurerId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    }

    private Endorsement buildEndorsement(UUID insurerId, EndorsementStatus status) {
        Endorsement e = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(insurerId)
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(status)
                .coverageStartDate(LocalDate.now().plusDays(1))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return e;
    }

    @Test
    @DisplayName("submitToInsurer routes to ICICI adapter for real-time submission")
    void submitToInsurer_IciciInsurer_RoutesToIciciAdapter() {
        Endorsement endorsement = buildEndorsement(iciciInsurerId, EndorsementStatus.PROVISIONALLY_COVERED);
        when(endorsementRepository.findById(endorsement.getId())).thenReturn(Optional.of(endorsement));

        InsurerPort iciciPort = mock(InsurerPort.class);
        when(iciciPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(true, false, 0, 0, 120));
        when(iciciPort.submitRealTime(eq(endorsement.getId()), any()))
                .thenReturn(new InsurerPort.SubmissionResult(true, "ICICI-12345678", null));
        when(insurerRouter.resolve(iciciInsurerId)).thenReturn(iciciPort);
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(provisionalCoverageRepository.findByEndorsementId(any())).thenReturn(Optional.empty());

        handler.submitToInsurer(endorsement.getId());

        verify(insurerRouter).resolve(iciciInsurerId);
        verify(iciciPort).submitRealTime(eq(endorsement.getId()), any());
        verify(notificationPort).notifyEndorsementConfirmed(any(), eq(endorsement.getId()));
    }

    @Test
    @DisplayName("submitToInsurer routes to Niva Bupa and queues for batch")
    void submitToInsurer_NivaInsurer_QueuedForBatch() {
        Endorsement endorsement = buildEndorsement(nivaInsurerId, EndorsementStatus.PROVISIONALLY_COVERED);
        when(endorsementRepository.findById(endorsement.getId())).thenReturn(Optional.of(endorsement));

        InsurerPort nivaPort = mock(InsurerPort.class);
        when(nivaPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(false, true, 500, 24, 0));
        when(insurerRouter.resolve(nivaInsurerId)).thenReturn(nivaPort);
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        handler.submitToInsurer(endorsement.getId());

        verify(insurerRouter).resolve(nivaInsurerId);
        verify(nivaPort, never()).submitRealTime(any(), any());
        verify(eventPublisher).publish(any(EndorsementEvent.QueuedForBatch.class));
    }

    @Test
    @DisplayName("submitToInsurer routes to mock adapter for mock insurer")
    void submitToInsurer_MockInsurer_RoutesToMockAdapter() {
        Endorsement endorsement = buildEndorsement(mockInsurerId, EndorsementStatus.PROVISIONALLY_COVERED);
        when(endorsementRepository.findById(endorsement.getId())).thenReturn(Optional.of(endorsement));

        InsurerPort mockPort = mock(InsurerPort.class);
        when(mockPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(true, true, 100, 24, 60));
        when(mockPort.submitRealTime(eq(endorsement.getId()), any()))
                .thenReturn(new InsurerPort.SubmissionResult(true, "INS-RT-ABCD1234", null));
        when(insurerRouter.resolve(mockInsurerId)).thenReturn(mockPort);
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(provisionalCoverageRepository.findByEndorsementId(any())).thenReturn(Optional.empty());

        handler.submitToInsurer(endorsement.getId());

        verify(insurerRouter).resolve(mockInsurerId);
        verify(mockPort).submitRealTime(eq(endorsement.getId()), any());
    }

    @Test
    @DisplayName("submitToInsurer handles rejection from insurer")
    void submitToInsurer_InsurerRejects_TransitionsToRejected() {
        Endorsement endorsement = buildEndorsement(iciciInsurerId, EndorsementStatus.PROVISIONALLY_COVERED);
        when(endorsementRepository.findById(endorsement.getId())).thenReturn(Optional.of(endorsement));

        InsurerPort iciciPort = mock(InsurerPort.class);
        when(iciciPort.getCapabilities())
                .thenReturn(new InsurerPort.InsurerCapabilities(true, false, 0, 0, 120));
        when(iciciPort.submitRealTime(eq(endorsement.getId()), any()))
                .thenReturn(new InsurerPort.SubmissionResult(false, null, "Invalid policy number"));
        when(insurerRouter.resolve(iciciInsurerId)).thenReturn(iciciPort);
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        handler.submitToInsurer(endorsement.getId());

        verify(eventPublisher).publish(any(EndorsementEvent.Rejected.class));
        verify(notificationPort).notifyEndorsementRejected(any(), eq(endorsement.getId()), eq("Invalid policy number"));
    }

    @Test
    @DisplayName("handleConfirmation works independently of insurer routing")
    void handleConfirmation_WorksWithoutRouting() {
        Endorsement endorsement = buildEndorsement(iciciInsurerId, EndorsementStatus.INSURER_PROCESSING);
        when(endorsementRepository.findById(endorsement.getId())).thenReturn(Optional.of(endorsement));
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(provisionalCoverageRepository.findByEndorsementId(any())).thenReturn(Optional.empty());

        handler.handleConfirmation(endorsement.getId(), "ICICI-CONFIRMED-123");

        verify(eventPublisher).publish(any(EndorsementEvent.Confirmed.class));
        verify(notificationPort).notifyEndorsementConfirmed(any(), eq(endorsement.getId()));
    }

    @Test
    @DisplayName("handleRejection with retries available schedules retry")
    void handleRejection_RetriesAvailable_SchedulesRetry() {
        Endorsement endorsement = buildEndorsement(iciciInsurerId, EndorsementStatus.REJECTED);
        endorsement.setRetryCount(0);
        when(endorsementRepository.findById(endorsement.getId())).thenReturn(Optional.of(endorsement));
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        handler.handleRejection(endorsement.getId(), "Temporary failure");

        verify(eventPublisher).publish(any(EndorsementEvent.RetryScheduled.class));
    }

    @Test
    @DisplayName("handleRejection with retries exhausted expires provisional coverage and notifies")
    void handleRejection_RetriesExhausted_ExpiresCoverageAndNotifies() {
        Endorsement endorsement = buildEndorsement(iciciInsurerId, EndorsementStatus.REJECTED);
        endorsement.setRetryCount(3); // max retries exhausted
        when(endorsementRepository.findById(endorsement.getId())).thenReturn(Optional.of(endorsement));
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProvisionalCoverage coverage = ProvisionalCoverage.builder()
                .id(UUID.randomUUID())
                .endorsementId(endorsement.getId())
                .employeeId(endorsement.getEmployeeId())
                .employerId(endorsement.getEmployerId())
                .coverageStart(LocalDate.now())
                .build();
        when(provisionalCoverageRepository.findByEndorsementId(endorsement.getId()))
                .thenReturn(Optional.of(coverage));

        handler.handleRejection(endorsement.getId(), "Permanent failure");

        verify(eventPublisher).publish(any(EndorsementEvent.FailedPermanent.class));
        verify(eventPublisher).publish(any(EndorsementEvent.ProvisionalCoverageExpired.class));
        verify(provisionalCoverageRepository).save(coverage);
        verify(notificationPort).notifyCoverageAtRisk(eq(endorsement.getEmployerId()),
                eq(endorsement.getId()), any());
    }

    @Test
    @DisplayName("handleConfirmation publishes ProvisionalCoverageConfirmed event")
    void handleConfirmation_PublishesCoverageConfirmedEvent() {
        Endorsement endorsement = buildEndorsement(iciciInsurerId, EndorsementStatus.INSURER_PROCESSING);
        when(endorsementRepository.findById(endorsement.getId())).thenReturn(Optional.of(endorsement));
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProvisionalCoverage coverage = ProvisionalCoverage.builder()
                .id(UUID.randomUUID())
                .endorsementId(endorsement.getId())
                .employeeId(endorsement.getEmployeeId())
                .employerId(endorsement.getEmployerId())
                .coverageStart(LocalDate.now())
                .build();
        when(provisionalCoverageRepository.findByEndorsementId(endorsement.getId()))
                .thenReturn(Optional.of(coverage));

        handler.handleConfirmation(endorsement.getId(), "ICICI-CONFIRMED-456");

        verify(eventPublisher).publish(any(EndorsementEvent.Confirmed.class));
        verify(eventPublisher).publish(any(EndorsementEvent.ProvisionalCoverageConfirmed.class));
    }
}
