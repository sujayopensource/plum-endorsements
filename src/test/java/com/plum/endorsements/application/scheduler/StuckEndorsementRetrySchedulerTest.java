package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.handler.ProcessEndorsementHandler;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.port.EndorsementRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StuckEndorsementRetryScheduler")
class StuckEndorsementRetrySchedulerTest {

    @Mock private EndorsementRepository endorsementRepository;
    @Mock private ProcessEndorsementHandler processEndorsementHandler;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private StuckEndorsementRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new StuckEndorsementRetryScheduler(
                endorsementRepository, processEndorsementHandler, meterRegistry);
    }

    private Endorsement buildRetryPendingEndorsement(int retryCount) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.RETRY_PENDING)
                .coverageStartDate(LocalDate.now().plusDays(1))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(retryCount)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("resubmits all endorsements stuck in RETRY_PENDING")
    void retryStuckEndorsements_ResubmitsAll() {
        Endorsement e1 = buildRetryPendingEndorsement(1);
        Endorsement e2 = buildRetryPendingEndorsement(2);

        when(endorsementRepository.findByStatus(EndorsementStatus.RETRY_PENDING))
                .thenReturn(List.of(e1, e2));

        scheduler.retryStuckEndorsements();

        verify(processEndorsementHandler).submitToInsurer(e1.getId());
        verify(processEndorsementHandler).submitToInsurer(e2.getId());
    }

    @Test
    @DisplayName("does nothing when no RETRY_PENDING endorsements exist")
    void retryStuckEndorsements_NoPending_DoesNothing() {
        when(endorsementRepository.findByStatus(EndorsementStatus.RETRY_PENDING))
                .thenReturn(List.of());

        scheduler.retryStuckEndorsements();

        verify(processEndorsementHandler, never()).submitToInsurer(any());
    }

    @Test
    @DisplayName("continues processing remaining endorsements when one resubmission fails")
    void retryStuckEndorsements_OneFailure_ContinuesWithRest() {
        Endorsement e1 = buildRetryPendingEndorsement(1);
        Endorsement e2 = buildRetryPendingEndorsement(2);

        when(endorsementRepository.findByStatus(EndorsementStatus.RETRY_PENDING))
                .thenReturn(List.of(e1, e2));

        doThrow(new RuntimeException("Insurer timeout"))
                .when(processEndorsementHandler).submitToInsurer(e1.getId());

        scheduler.retryStuckEndorsements();

        verify(processEndorsementHandler).submitToInsurer(e1.getId());
        verify(processEndorsementHandler).submitToInsurer(e2.getId());
    }
}
