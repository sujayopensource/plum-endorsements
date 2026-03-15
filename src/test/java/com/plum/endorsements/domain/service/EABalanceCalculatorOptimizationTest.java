package com.plum.endorsements.domain.service;

import com.plum.endorsements.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EABalanceCalculator — Optimization")
class EABalanceCalculatorOptimizationTest {

    private EABalanceCalculator calculator;
    private UUID employerId;
    private UUID insurerId;

    @BeforeEach
    void setUp() {
        calculator = new EABalanceCalculator(new BigDecimal("0.10"));
        employerId = UUID.randomUUID();
        insurerId = UUID.randomUUID();
    }

    private Endorsement buildEndorsement(EndorsementType type, BigDecimal premium, LocalDate coverageStart) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(insurerId)
                .policyId(UUID.randomUUID())
                .type(type)
                .premiumAmount(premium)
                .coverageStartDate(coverageStart)
                .status(EndorsementStatus.QUEUED_FOR_BATCH)
                .retryCount(0)
                .build();
    }

    private EAAccount buildAccount(BigDecimal balance) {
        return EAAccount.builder()
                .employerId(employerId)
                .insurerId(insurerId)
                .balance(balance)
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();
    }

    // --- sequenceForOptimalBalance tests ---

    @Test
    @DisplayName("deletions are sequenced before additions")
    void sequenceForOptimalBalance_DeletionsFirst() {
        Endorsement add = buildEndorsement(EndorsementType.ADD, new BigDecimal("500"), LocalDate.now().plusDays(1));
        Endorsement del = buildEndorsement(EndorsementType.DELETE, new BigDecimal("300"), LocalDate.now().plusDays(1));

        List<Endorsement> sequenced = calculator.sequenceForOptimalBalance(List.of(add, del));

        assertThat(sequenced.get(0).getType()).isEqualTo(EndorsementType.DELETE);
        assertThat(sequenced.get(1).getType()).isEqualTo(EndorsementType.ADD);
    }

    @Test
    @DisplayName("additions sorted by coverage start date")
    void sequenceForOptimalBalance_AdditionsSortedByDate() {
        Endorsement add1 = buildEndorsement(EndorsementType.ADD, new BigDecimal("500"), LocalDate.now().plusDays(5));
        Endorsement add2 = buildEndorsement(EndorsementType.ADD, new BigDecimal("500"), LocalDate.now().plusDays(1));

        List<Endorsement> sequenced = calculator.sequenceForOptimalBalance(List.of(add1, add2));

        assertThat(sequenced.get(0).getCoverageStartDate()).isBefore(sequenced.get(1).getCoverageStartDate());
    }

    @Test
    @DisplayName("full priority order: DELETE, cost-neutral UPDATE, ADD, premium UPDATE")
    void sequenceForOptimalBalance_FullPriorityOrder() {
        Endorsement del = buildEndorsement(EndorsementType.DELETE, new BigDecimal("300"), LocalDate.now());
        Endorsement costNeutral = buildEndorsement(EndorsementType.UPDATE, BigDecimal.ZERO, LocalDate.now());
        Endorsement add = buildEndorsement(EndorsementType.ADD, new BigDecimal("500"), LocalDate.now());
        Endorsement premiumUpdate = buildEndorsement(EndorsementType.UPDATE, new BigDecimal("200"), LocalDate.now());

        List<Endorsement> sequenced = calculator.sequenceForOptimalBalance(
                List.of(premiumUpdate, add, costNeutral, del));

        assertThat(sequenced.get(0).getType()).isEqualTo(EndorsementType.DELETE);
        assertThat(sequenced.get(1).getType()).isEqualTo(EndorsementType.UPDATE); // cost neutral
        assertThat(sequenced.get(1).getPremiumAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sequenced.get(2).getType()).isEqualTo(EndorsementType.ADD);
        assertThat(sequenced.get(3).getType()).isEqualTo(EndorsementType.UPDATE); // premium update
        assertThat(sequenced.get(3).getPremiumAmount()).isEqualByComparingTo(new BigDecimal("200"));
    }

    // --- constructOptimizedBatch tests ---

    @Test
    @DisplayName("deletions always included in batch")
    void constructOptimizedBatch_DeletionsAlwaysIncluded() {
        EAAccount account = buildAccount(BigDecimal.ZERO); // zero balance
        Endorsement del = buildEndorsement(EndorsementType.DELETE, new BigDecimal("500"), LocalDate.now());

        EABalanceCalculator.BatchPlan plan = calculator.constructOptimizedBatch(
                List.of(del), account, 100);

        assertThat(plan.included()).hasSize(1);
        assertThat(plan.deferred()).isEmpty();
    }

    @Test
    @DisplayName("additions deferred when balance insufficient")
    void constructOptimizedBatch_InsufficientBalance_AdditionsDeferred() {
        EAAccount account = buildAccount(new BigDecimal("300")); // not enough for 500
        Endorsement add = buildEndorsement(EndorsementType.ADD, new BigDecimal("500"), LocalDate.now());

        EABalanceCalculator.BatchPlan plan = calculator.constructOptimizedBatch(
                List.of(add), account, 100);

        assertThat(plan.included()).isEmpty();
        assertThat(plan.deferred()).hasSize(1);
    }

    @Test
    @DisplayName("deletions free up balance for subsequent additions")
    void constructOptimizedBatch_DeletionsFreeUpBalance() {
        EAAccount account = buildAccount(new BigDecimal("200")); // 200 available
        Endorsement del = buildEndorsement(EndorsementType.DELETE, new BigDecimal("400"), LocalDate.now());
        Endorsement add = buildEndorsement(EndorsementType.ADD, new BigDecimal("500"), LocalDate.now());

        EABalanceCalculator.BatchPlan plan = calculator.constructOptimizedBatch(
                List.of(add, del), account, 100); // del processed first (priority)

        assertThat(plan.included()).hasSize(2); // 200 + 400 = 600 >= 500
        assertThat(plan.deferred()).isEmpty();
    }

    @Test
    @DisplayName("respects max batch size")
    void constructOptimizedBatch_RespectsMaxBatchSize() {
        EAAccount account = buildAccount(new BigDecimal("10000"));
        List<Endorsement> endorsements = List.of(
                buildEndorsement(EndorsementType.ADD, new BigDecimal("100"), LocalDate.now()),
                buildEndorsement(EndorsementType.ADD, new BigDecimal("100"), LocalDate.now()),
                buildEndorsement(EndorsementType.ADD, new BigDecimal("100"), LocalDate.now())
        );

        EABalanceCalculator.BatchPlan plan = calculator.constructOptimizedBatch(endorsements, account, 2);

        assertThat(plan.included()).hasSize(2);
        assertThat(plan.deferred()).hasSize(1);
    }

    // --- forecastBalance tests ---

    @Test
    @DisplayName("forecast with sufficient balance shows no top-up required")
    void forecastBalance_SufficientBalance_NoTopUp() {
        EAAccount account = buildAccount(new BigDecimal("10000"));
        List<Endorsement> endorsements = List.of(
                buildEndorsement(EndorsementType.ADD, new BigDecimal("1000"), LocalDate.now())
        );

        EABalanceCalculator.BalanceForecast forecast = calculator.forecastBalance(account, endorsements);

        assertThat(forecast.topUpRequired()).isFalse();
        assertThat(forecast.shortfall()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("forecast with insufficient balance shows shortfall")
    void forecastBalance_InsufficientBalance_ShowsShortfall() {
        EAAccount account = buildAccount(new BigDecimal("500"));
        List<Endorsement> endorsements = List.of(
                buildEndorsement(EndorsementType.ADD, new BigDecimal("2000"), LocalDate.now())
        );

        EABalanceCalculator.BalanceForecast forecast = calculator.forecastBalance(account, endorsements);

        assertThat(forecast.topUpRequired()).isTrue();
        assertThat(forecast.shortfall()).isPositive();
        // Required minimum includes 10% safety margin: 2000 * 1.1 = 2200, shortfall = 2200 - 500 = 1700
        assertThat(forecast.requiredMinimum()).isEqualByComparingTo(new BigDecimal("2200.00"));
    }

    @Test
    @DisplayName("forecast accounts for deletions reducing required balance")
    void forecastBalance_DeletionsReduceRequired() {
        EAAccount account = buildAccount(new BigDecimal("500"));
        List<Endorsement> endorsements = List.of(
                buildEndorsement(EndorsementType.DELETE, new BigDecimal("800"), LocalDate.now()),
                buildEndorsement(EndorsementType.ADD, new BigDecimal("1000"), LocalDate.now())
        );

        EABalanceCalculator.BalanceForecast forecast = calculator.forecastBalance(account, endorsements);

        // Net required = 1000 - 800 = 200, with 10% margin = 220, available = 500
        assertThat(forecast.topUpRequired()).isFalse();
    }

    @Test
    @DisplayName("optimized batch uses less capital than FIFO")
    void comparativeTest_OptimizedVsFifo() {
        EAAccount account = buildAccount(new BigDecimal("1000"));

        Endorsement del1 = buildEndorsement(EndorsementType.DELETE, new BigDecimal("500"), LocalDate.now());
        Endorsement add1 = buildEndorsement(EndorsementType.ADD, new BigDecimal("800"), LocalDate.now());
        Endorsement add2 = buildEndorsement(EndorsementType.ADD, new BigDecimal("600"), LocalDate.now().plusDays(1));

        List<Endorsement> endorsements = List.of(add1, add2, del1);

        // Optimized: del first frees up balance
        EABalanceCalculator.BatchPlan optimized = calculator.constructOptimizedBatch(
                endorsements, account, 100);

        // FIFO would try add1 first (800 <= 1000, ok), then add2 (600 > 200, deferred), del (always ok)
        // Optimized processes: del (1000+500=1500), add1 (1500-800=700), add2 (700-600=100)
        // So optimized includes all 3, FIFO would only include 2
        assertThat(optimized.included()).hasSize(3);
        assertThat(optimized.deferred()).isEmpty();
    }
}
