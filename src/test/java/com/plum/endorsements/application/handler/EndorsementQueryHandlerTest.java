package com.plum.endorsements.application.handler;

import com.plum.endorsements.application.exception.EndorsementNotFoundException;
import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import com.plum.endorsements.domain.service.InsurerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EndorsementQueryHandler")
class EndorsementQueryHandlerTest {

    @Mock
    private EndorsementRepository endorsementRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private EAAccountRepository eaAccountRepository;

    @Mock
    private ProvisionalCoverageRepository provisionalCoverageRepository;

    @Mock
    private InsurerRegistry insurerRegistry;

    @Mock
    private ReconciliationRepository reconciliationRepository;

    @InjectMocks
    private EndorsementQueryHandler queryHandler;

    private static final UUID EMPLOYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSURER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns endorsement when it exists")
        void findById_WhenExists_ReturnsEndorsement() {
            UUID id = UUID.randomUUID();
            Endorsement endorsement = Endorsement.builder().id(id).employerId(EMPLOYER_ID).build();
            when(endorsementRepository.findById(id)).thenReturn(Optional.of(endorsement));

            Endorsement result = queryHandler.findById(id);

            assertThat(result.getId()).isEqualTo(id);
            verify(endorsementRepository).findById(id);
        }

        @Test
        @DisplayName("throws EndorsementNotFoundException when not found")
        void findById_WhenNotFound_Throws() {
            UUID id = UUID.randomUUID();
            when(endorsementRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryHandler.findById(id))
                    .isInstanceOf(EndorsementNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findOutstandingByEmployerId")
    class FindOutstanding {

        @Test
        @DisplayName("queries for non-terminal statuses only")
        void findOutstandingByEmployerId_QueriesNonTerminalStatuses() {
            Pageable pageable = PageRequest.of(0, 20);
            List<Endorsement> outstanding = List.of(
                    Endorsement.builder().id(UUID.randomUUID()).status(EndorsementStatus.CREATED).build(),
                    Endorsement.builder().id(UUID.randomUUID()).status(EndorsementStatus.VALIDATED).build()
            );
            Page<Endorsement> page = new PageImpl<>(outstanding, pageable, 2);

            when(endorsementRepository.findByEmployerIdAndStatusIn(eq(EMPLOYER_ID), any(), eq(pageable)))
                    .thenReturn(page);

            Page<Endorsement> result = queryHandler.findOutstandingByEmployerId(EMPLOYER_ID, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);

            // Verify the correct statuses are passed
            verify(endorsementRepository).findByEmployerIdAndStatusIn(
                    eq(EMPLOYER_ID),
                    argThat(statuses -> statuses.size() == 8
                            && statuses.contains(EndorsementStatus.CREATED)
                            && statuses.contains(EndorsementStatus.VALIDATED)
                            && statuses.contains(EndorsementStatus.PROVISIONALLY_COVERED)
                            && statuses.contains(EndorsementStatus.SUBMITTED_REALTIME)
                            && statuses.contains(EndorsementStatus.QUEUED_FOR_BATCH)
                            && statuses.contains(EndorsementStatus.BATCH_SUBMITTED)
                            && statuses.contains(EndorsementStatus.INSURER_PROCESSING)
                            && statuses.contains(EndorsementStatus.RETRY_PENDING)
                            && !statuses.contains(EndorsementStatus.CONFIRMED)
                            && !statuses.contains(EndorsementStatus.REJECTED)
                            && !statuses.contains(EndorsementStatus.FAILED_PERMANENT)),
                    eq(pageable));
        }

        @Test
        @DisplayName("returns empty page when no outstanding endorsements")
        void findOutstandingByEmployerId_WhenNone_ReturnsEmpty() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Endorsement> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(endorsementRepository.findByEmployerIdAndStatusIn(eq(EMPLOYER_ID), any(), eq(pageable)))
                    .thenReturn(emptyPage);

            Page<Endorsement> result = queryHandler.findOutstandingByEmployerId(EMPLOYER_ID, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findBatchesByEmployerId")
    class FindBatches {

        @Test
        @DisplayName("returns batches linked to employer")
        void findBatchesByEmployerId_ReturnsBatches() {
            Pageable pageable = PageRequest.of(0, 20);
            EndorsementBatch batch = EndorsementBatch.builder()
                    .id(UUID.randomUUID())
                    .insurerId(INSURER_ID)
                    .status(BatchStatus.SUBMITTED)
                    .endorsementCount(5)
                    .totalPremium(BigDecimal.valueOf(7500))
                    .createdAt(Instant.now())
                    .build();
            Page<EndorsementBatch> page = new PageImpl<>(List.of(batch), pageable, 1);

            when(batchRepository.findByEmployerId(EMPLOYER_ID, pageable)).thenReturn(page);

            Page<EndorsementBatch> result = queryHandler.findBatchesByEmployerId(EMPLOYER_ID, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(BatchStatus.SUBMITTED);
            assertThat(result.getContent().get(0).getEndorsementCount()).isEqualTo(5);
            verify(batchRepository).findByEmployerId(EMPLOYER_ID, pageable);
        }

        @Test
        @DisplayName("returns empty page when no batches found")
        void findBatchesByEmployerId_WhenNone_ReturnsEmpty() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<EndorsementBatch> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(batchRepository.findByEmployerId(EMPLOYER_ID, pageable)).thenReturn(emptyPage);

            Page<EndorsementBatch> result = queryHandler.findBatchesByEmployerId(EMPLOYER_ID, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findEAAccount")
    class FindEAAccount {

        @Test
        @DisplayName("returns EA account when it exists")
        void findEAAccount_WhenExists_ReturnsAccount() {
            EAAccount account = EAAccount.builder()
                    .employerId(EMPLOYER_ID)
                    .insurerId(INSURER_ID)
                    .balance(BigDecimal.valueOf(50000))
                    .build();
            when(eaAccountRepository.findByEmployerIdAndInsurerId(EMPLOYER_ID, INSURER_ID))
                    .thenReturn(Optional.of(account));

            Optional<EAAccount> result = queryHandler.findEAAccount(EMPLOYER_ID, INSURER_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        }

        @Test
        @DisplayName("returns empty when EA account not found")
        void findEAAccount_WhenNotFound_ReturnsEmpty() {
            when(eaAccountRepository.findByEmployerIdAndInsurerId(EMPLOYER_ID, INSURER_ID))
                    .thenReturn(Optional.empty());

            Optional<EAAccount> result = queryHandler.findEAAccount(EMPLOYER_ID, INSURER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findReconciliationRunById")
    class FindReconciliationRun {

        @Test
        @DisplayName("throws when reconciliation run not found")
        void findReconciliationRunById_WhenNotFound_Throws() {
            UUID runId = UUID.randomUUID();
            when(reconciliationRepository.findRunById(runId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryHandler.findReconciliationRunById(runId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reconciliation run not found");
        }
    }
}
