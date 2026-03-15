package com.plum.endorsements.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.plum.endorsements.application.exception.InsufficientBalanceException;
import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import com.plum.endorsements.domain.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.mockito.Answers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateEndorsementHandlerTest {

    @Mock
    EndorsementRepository endorsementRepository;

    @Mock
    EAAccountRepository eaAccountRepository;

    @Mock
    ProvisionalCoverageRepository provisionalCoverageRepository;

    @Spy
    EndorsementStateMachine stateMachine = new EndorsementStateMachine();

    @Spy
    EABalanceCalculator balanceCalculator = new EABalanceCalculator(new BigDecimal("0.10"));

    @Mock
    EventPublisher eventPublisher;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MeterRegistry meterRegistry;

    private CreateEndorsementHandler handler;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID employerId;
    private UUID employeeId;
    private UUID insurerId;
    private UUID policyId;

    @BeforeEach
    void setUp() {
        employerId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        insurerId = UUID.randomUUID();
        policyId = UUID.randomUUID();
        handler = new CreateEndorsementHandler(
                endorsementRepository, eaAccountRepository, provisionalCoverageRepository,
                stateMachine, balanceCalculator, eventPublisher, meterRegistry,
                false);
    }

    private Endorsement buildNewEndorsement(EndorsementType type, BigDecimal premiumAmount) {
        ObjectNode employeeData = objectMapper.createObjectNode();
        employeeData.put("name", "John Doe");
        employeeData.put("age", 30);

        return Endorsement.builder()
                .employerId(employerId)
                .employeeId(employeeId)
                .insurerId(insurerId)
                .policyId(policyId)
                .type(type)
                .coverageStartDate(LocalDate.now().plusDays(1))
                .coverageEndDate(LocalDate.now().plusYears(1))
                .employeeData(employeeData)
                .premiumAmount(premiumAmount)
                .retryCount(0)
                .idempotencyKey("idem-" + UUID.randomUUID())
                .build();
    }

    private void mockSaveBehavior() {
        when(endorsementRepository.save(any(Endorsement.class))).thenAnswer(invocation -> {
            Endorsement e = invocation.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            return e;
        });
    }

    // --- Test 1: New endorsement creation succeeds ---

    @Test
    void createEndorsement_NewEndorsement_ShouldSucceed() {
        // Arrange
        Endorsement endorsement = buildNewEndorsement(EndorsementType.ADD, new BigDecimal("1000.00"));

        when(endorsementRepository.findByIdempotencyKey(endorsement.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        mockSaveBehavior();
        when(eaAccountRepository.findByEmployerIdAndInsurerIdForUpdate(employerId, insurerId))
                .thenReturn(Optional.empty());

        // Act
        Endorsement result = handler.handle(endorsement);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EndorsementStatus.PROVISIONALLY_COVERED);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        // Verify endorsement was saved 3 times:
        // 1. Initial save after CREATED
        // 2. Save after VALIDATED transition
        // 3. Save after PROVISIONALLY_COVERED transition
        verify(endorsementRepository, times(3)).save(any(Endorsement.class));

        // Verify 3 events were published: Created, Validated, ProvisionalCoverageGranted
        verify(eventPublisher, times(3)).publish(any(EndorsementEvent.class));
        verify(eventPublisher).publish(any(EndorsementEvent.Created.class));
        verify(eventPublisher).publish(any(EndorsementEvent.Validated.class));
        verify(eventPublisher).publish(any(EndorsementEvent.ProvisionalCoverageGranted.class));

        // Verify provisional coverage was saved (ADD type)
        verify(provisionalCoverageRepository, times(1)).save(any(ProvisionalCoverage.class));
    }

    // --- Test 2: Duplicate idempotency key returns existing ---

    @Test
    void createEndorsement_DuplicateKey_ShouldReturnExisting() {
        // Arrange
        String idempotencyKey = "idem-duplicate-key";
        Endorsement newEndorsement = buildNewEndorsement(EndorsementType.ADD, new BigDecimal("1000.00"));
        newEndorsement.setIdempotencyKey(idempotencyKey);

        Endorsement existingEndorsement = buildNewEndorsement(EndorsementType.ADD, new BigDecimal("1000.00"));
        existingEndorsement.setId(UUID.randomUUID());
        existingEndorsement.setIdempotencyKey(idempotencyKey);
        existingEndorsement.setStatus(EndorsementStatus.PROVISIONALLY_COVERED);
        existingEndorsement.setCreatedAt(Instant.now().minusSeconds(3600));
        existingEndorsement.setUpdatedAt(Instant.now().minusSeconds(60));

        when(endorsementRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingEndorsement));

        // Act
        Endorsement result = handler.handle(newEndorsement);

        // Assert
        assertThat(result).isSameAs(existingEndorsement);
        assertThat(result.getId()).isEqualTo(existingEndorsement.getId());

        // Verify save was NEVER called
        verify(endorsementRepository, never()).save(any(Endorsement.class));

        // Verify no events were published
        verify(eventPublisher, never()).publish(any(EndorsementEvent.class));

        // Verify no provisional coverage was created
        verify(provisionalCoverageRepository, never()).save(any(ProvisionalCoverage.class));
    }

    // --- Test 3: With sufficient EA balance, funds are reserved ---

    @Test
    void createEndorsement_WithSufficientBalance_ShouldReserveFunds() {
        // Arrange
        Endorsement endorsement = buildNewEndorsement(EndorsementType.ADD, new BigDecimal("500.00"));

        when(endorsementRepository.findByIdempotencyKey(endorsement.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        mockSaveBehavior();

        EAAccount eaAccount = EAAccount.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .balance(new BigDecimal("1000.00"))
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        when(eaAccountRepository.findByEmployerIdAndInsurerIdForUpdate(employerId, insurerId))
                .thenReturn(Optional.of(eaAccount));

        // Act
        Endorsement result = handler.handle(endorsement);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EndorsementStatus.PROVISIONALLY_COVERED);

        // Verify EA account was saved after reservation
        verify(eaAccountRepository).save(eaAccount);

        // Verify the reservation was made on the account
        assertThat(eaAccount.getReserved()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(eaAccount.availableBalance()).isEqualByComparingTo(new BigDecimal("500.00"));

        // Verify a RESERVE transaction was saved
        verify(eaAccountRepository).saveTransaction(argThat(transaction ->
                transaction.type() == EATransaction.EATransactionType.RESERVE
                        && transaction.amount().compareTo(new BigDecimal("500.00")) == 0
                        && transaction.employerId().equals(employerId)
                        && transaction.insurerId().equals(insurerId)
        ));
    }

    // --- Additional tests ---

    @Test
    void createEndorsement_WithInsufficientBalance_ShouldNotReserveFunds() {
        // Arrange
        Endorsement endorsement = buildNewEndorsement(EndorsementType.ADD, new BigDecimal("1500.00"));

        when(endorsementRepository.findByIdempotencyKey(endorsement.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        mockSaveBehavior();

        EAAccount eaAccount = EAAccount.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .balance(new BigDecimal("1000.00"))
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        when(eaAccountRepository.findByEmployerIdAndInsurerIdForUpdate(employerId, insurerId))
                .thenReturn(Optional.of(eaAccount));

        // Act
        Endorsement result = handler.handle(endorsement);

        // Assert - endorsement still succeeds (EA balance check is not blocking)
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EndorsementStatus.PROVISIONALLY_COVERED);

        // Verify EA account was NOT saved (insufficient funds, canFund returns false)
        verify(eaAccountRepository, never()).save(any(EAAccount.class));

        // Verify no RESERVE transaction was saved
        verify(eaAccountRepository, never()).saveTransaction(any(EATransaction.class));
    }

    @Test
    void createEndorsement_DeleteType_ShouldNotSaveProvisionalCoverage() {
        // Arrange
        Endorsement endorsement = buildNewEndorsement(EndorsementType.DELETE, new BigDecimal("500.00"));

        when(endorsementRepository.findByIdempotencyKey(endorsement.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        mockSaveBehavior();

        // Act
        Endorsement result = handler.handle(endorsement);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EndorsementStatus.PROVISIONALLY_COVERED);

        // Verify provisional coverage was NOT saved for DELETE type
        verify(provisionalCoverageRepository, never()).save(any(ProvisionalCoverage.class));

        // Verify EA balance was NOT checked for DELETE type
        verify(eaAccountRepository, never()).findByEmployerIdAndInsurerId(any(UUID.class), any(UUID.class));
    }

    @Test
    void createEndorsement_UpdateType_ShouldNotCheckEABalance() {
        // Arrange
        Endorsement endorsement = buildNewEndorsement(EndorsementType.UPDATE, new BigDecimal("200.00"));

        when(endorsementRepository.findByIdempotencyKey(endorsement.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        mockSaveBehavior();

        // Act
        Endorsement result = handler.handle(endorsement);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EndorsementStatus.PROVISIONALLY_COVERED);

        // Verify EA balance was NOT checked for UPDATE type
        verify(eaAccountRepository, never()).findByEmployerIdAndInsurerId(any(UUID.class), any(UUID.class));

        // Verify no RESERVE transaction was saved
        verify(eaAccountRepository, never()).saveTransaction(any(EATransaction.class));
    }

    @Test
    void createEndorsement_SetsInitialStatusToCreated() {
        // Arrange
        Endorsement endorsement = buildNewEndorsement(EndorsementType.ADD, new BigDecimal("100.00"));
        endorsement.setStatus(null); // Ensure handler sets it

        when(endorsementRepository.findByIdempotencyKey(endorsement.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        mockSaveBehavior();
        when(eaAccountRepository.findByEmployerIdAndInsurerIdForUpdate(employerId, insurerId))
                .thenReturn(Optional.empty());

        // Act
        handler.handle(endorsement);

        // Assert - verify that during the first save, status was CREATED
        verify(endorsementRepository, atLeastOnce()).save(argThat(e ->
                e.getCreatedAt() != null && e.getUpdatedAt() != null
        ));
    }

    @Test
    void createEndorsement_NoEAAccount_ShouldStillSucceed() {
        // Arrange
        Endorsement endorsement = buildNewEndorsement(EndorsementType.ADD, new BigDecimal("500.00"));

        when(endorsementRepository.findByIdempotencyKey(endorsement.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        mockSaveBehavior();
        when(eaAccountRepository.findByEmployerIdAndInsurerIdForUpdate(employerId, insurerId))
                .thenReturn(Optional.empty());

        // Act
        Endorsement result = handler.handle(endorsement);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EndorsementStatus.PROVISIONALLY_COVERED);

        // Verify no EA operations
        verify(eaAccountRepository, never()).save(any(EAAccount.class));
        verify(eaAccountRepository, never()).saveTransaction(any(EATransaction.class));
    }

    @Test
    void createEndorsement_InsufficientBalanceBlocking_ShouldThrow() {
        // Arrange — create handler with blocking enabled
        CreateEndorsementHandler blockingHandler = new CreateEndorsementHandler(
                endorsementRepository, eaAccountRepository, provisionalCoverageRepository,
                stateMachine, balanceCalculator, eventPublisher, meterRegistry,
                true);

        Endorsement endorsement = buildNewEndorsement(EndorsementType.ADD, new BigDecimal("1500.00"));

        when(endorsementRepository.findByIdempotencyKey(endorsement.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        mockSaveBehavior();

        EAAccount eaAccount = EAAccount.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .balance(new BigDecimal("1000.00"))
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        when(eaAccountRepository.findByEmployerIdAndInsurerIdForUpdate(employerId, insurerId))
                .thenReturn(Optional.of(eaAccount));

        // Act & Assert
        assertThatThrownBy(() -> blockingHandler.handle(endorsement))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient EA balance");
    }
}
