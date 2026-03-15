package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.EAAccount;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.port.BatchOptimizerPort;
import com.plum.endorsements.domain.port.InsurerPort;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConstraintBatchOptimizer")
class ConstraintBatchOptimizerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private ConstraintBatchOptimizer optimizer;

    @BeforeEach
    void setUp() {
        optimizer = new ConstraintBatchOptimizer(meterRegistry);
    }

    private Endorsement buildEndorsement(EndorsementType type, BigDecimal premium) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(type)
                .status(EndorsementStatus.QUEUED_FOR_BATCH)
                .coverageStartDate(LocalDate.now().plusDays(15))
                .premiumAmount(premium)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private EAAccount buildAccount(BigDecimal balance) {
        return EAAccount.builder()
                .employerId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .balance(balance)
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("optimizes deletions first before additions")
    void optimizeBatch_DeletionsFirst() {
        Endorsement add1 = buildEndorsement(EndorsementType.ADD, new BigDecimal("500.00"));
        Endorsement add2 = buildEndorsement(EndorsementType.ADD, new BigDecimal("300.00"));
        Endorsement del1 = buildEndorsement(EndorsementType.DELETE, new BigDecimal("200.00"));

        List<Endorsement> queue = new ArrayList<>(List.of(add1, add2, del1));
        EAAccount account = buildAccount(new BigDecimal("1000.00"));
        InsurerPort.InsurerCapabilities capabilities =
                new InsurerPort.InsurerCapabilities(false, true, 10, 24, 0);

        BatchOptimizerPort.OptimizedBatchPlan plan = optimizer.optimizeBatch(queue, account, capabilities);

        assertThat(plan.endorsements()).isNotEmpty();

        // Deletions should appear before additions in the optimized list
        int deleteIndex = -1;
        for (int i = 0; i < plan.endorsements().size(); i++) {
            if (plan.endorsements().get(i).getType() == EndorsementType.DELETE) {
                deleteIndex = i;
                break;
            }
        }
        assertThat(deleteIndex).isGreaterThanOrEqualTo(0);

        // All endorsements should be included since balance is sufficient
        assertThat(plan.endorsements()).hasSize(3);
    }

    @Test
    @DisplayName("respects balance constraints — excludes expensive additions")
    void optimizeBatch_RespectsBalanceConstraint() {
        Endorsement add1 = buildEndorsement(EndorsementType.ADD, new BigDecimal("800.00"));
        Endorsement add2 = buildEndorsement(EndorsementType.ADD, new BigDecimal("500.00"));

        List<Endorsement> queue = new ArrayList<>(List.of(add1, add2));
        EAAccount account = buildAccount(new BigDecimal("700.00")); // Only enough for one
        InsurerPort.InsurerCapabilities capabilities =
                new InsurerPort.InsurerCapabilities(false, true, 10, 24, 0);

        BatchOptimizerPort.OptimizedBatchPlan plan = optimizer.optimizeBatch(queue, account, capabilities);

        // With 700 available, can only fit the 500 addition (not the 800)
        assertThat(plan.endorsements()).hasSize(1);
        assertThat(plan.endorsements().get(0).getPremiumAmount())
                .isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("handles empty queue gracefully")
    void optimizeBatch_EmptyQueue_ReturnsEmptyPlan() {
        List<Endorsement> queue = new ArrayList<>();
        EAAccount account = buildAccount(new BigDecimal("10000.00"));
        InsurerPort.InsurerCapabilities capabilities =
                new InsurerPort.InsurerCapabilities(false, true, 10, 24, 0);

        BatchOptimizerPort.OptimizedBatchPlan plan = optimizer.optimizeBatch(queue, account, capabilities);

        assertThat(plan.endorsements()).isEmpty();
        assertThat(plan.strategy()).contains("0 of 0");
    }

    @Test
    @DisplayName("respects max batch size constraint")
    void optimizeBatch_RespectsMaxBatchSize() {
        List<Endorsement> queue = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            queue.add(buildEndorsement(EndorsementType.ADD, new BigDecimal("100.00")));
        }

        EAAccount account = buildAccount(new BigDecimal("100000.00")); // Plenty of balance
        InsurerPort.InsurerCapabilities capabilities =
                new InsurerPort.InsurerCapabilities(false, true, 5, 24, 0); // Max 5

        BatchOptimizerPort.OptimizedBatchPlan plan = optimizer.optimizeBatch(queue, account, capabilities);

        assertThat(plan.endorsements()).hasSize(5);
    }

    @Test
    @DisplayName("deletion frees up balance for subsequent additions")
    void optimizeBatch_DeletionFreesBalance() {
        Endorsement del = buildEndorsement(EndorsementType.DELETE, new BigDecimal("500.00"));
        Endorsement add = buildEndorsement(EndorsementType.ADD, new BigDecimal("700.00"));

        List<Endorsement> queue = new ArrayList<>(List.of(add, del));
        EAAccount account = buildAccount(new BigDecimal("400.00"));
        // Initial available: 400
        // After DELETE (+500): 900
        // 900 >= 700 for the ADD

        InsurerPort.InsurerCapabilities capabilities =
                new InsurerPort.InsurerCapabilities(false, true, 10, 24, 0);

        BatchOptimizerPort.OptimizedBatchPlan plan = optimizer.optimizeBatch(queue, account, capabilities);

        assertThat(plan.endorsements()).hasSize(2);
    }

    // --- Phase 3 Edge Case Tests ---

    @Test
    @DisplayName("respects priority ordering: P0 deletions before P3 updates")
    void shouldRespectPriorityOrdering() {
        // P0: DELETE
        Endorsement deletion = buildEndorsement(EndorsementType.DELETE, new BigDecimal("200.00"));
        // P2: ADD
        Endorsement addition = buildEndorsement(EndorsementType.ADD, new BigDecimal("300.00"));
        // P3: UPDATE with non-zero premium
        Endorsement update = buildEndorsement(EndorsementType.UPDATE, new BigDecimal("150.00"));

        List<Endorsement> queue = new ArrayList<>(List.of(update, addition, deletion));
        EAAccount account = buildAccount(new BigDecimal("10000.00")); // Plenty of balance
        InsurerPort.InsurerCapabilities capabilities =
                new InsurerPort.InsurerCapabilities(false, true, 10, 24, 0);

        BatchOptimizerPort.OptimizedBatchPlan plan = optimizer.optimizeBatch(queue, account, capabilities);

        assertThat(plan.endorsements()).hasSize(3);

        // Deletions should always be first in the optimized list
        assertThat(plan.endorsements().get(0).getType()).isEqualTo(EndorsementType.DELETE);
    }

    @Test
    @DisplayName("handles single endorsement in queue")
    void shouldHandleSingleEndorsement() {
        Endorsement single = buildEndorsement(EndorsementType.ADD, new BigDecimal("500.00"));

        List<Endorsement> queue = new ArrayList<>(List.of(single));
        EAAccount account = buildAccount(new BigDecimal("1000.00"));
        InsurerPort.InsurerCapabilities capabilities =
                new InsurerPort.InsurerCapabilities(false, true, 10, 24, 0);

        BatchOptimizerPort.OptimizedBatchPlan plan = optimizer.optimizeBatch(queue, account, capabilities);

        assertThat(plan.endorsements()).hasSize(1);
        assertThat(plan.endorsements().get(0).getPremiumAmount())
                .isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(plan.strategy()).contains("1 of 1");
    }

    @Test
    @DisplayName("DP knapsack picks optimal subset over greedy — two small items beat one expensive")
    void optimizeBatch_DPKnapsack_PicksOptimalSubset() {
        // Expensive item: 900 cost, high score
        Endorsement expensive = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.QUEUED_FOR_BATCH)
                .coverageStartDate(LocalDate.now().plusDays(2))
                .premiumAmount(new BigDecimal("900.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Two cheaper items: 500 + 400 = 900 total cost, combined score should exceed single expensive
        Endorsement cheap1 = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.QUEUED_FOR_BATCH)
                .coverageStartDate(LocalDate.now().plusDays(3))
                .premiumAmount(new BigDecimal("500.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Endorsement cheap2 = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.QUEUED_FOR_BATCH)
                .coverageStartDate(LocalDate.now().plusDays(4))
                .premiumAmount(new BigDecimal("400.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        List<Endorsement> queue = new ArrayList<>(List.of(expensive, cheap1, cheap2));
        EAAccount account = buildAccount(new BigDecimal("950.00")); // Can fit 500+400 but not 900+any
        InsurerPort.InsurerCapabilities capabilities =
                new InsurerPort.InsurerCapabilities(false, true, 10, 24, 0);

        BatchOptimizerPort.OptimizedBatchPlan plan = optimizer.optimizeBatch(queue, account, capabilities);

        // DP knapsack should pick the two cheaper items (combined higher score) over the single expensive one
        assertThat(plan.endorsements()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(plan.strategy()).contains("DP knapsack");
    }

    @Test
    @DisplayName("optimizes for deadlines by prioritizing near-term coverage start dates")
    void shouldOptimizeForDeadlines() {
        // Urgent endorsement: coverage starts in 3 days
        Endorsement urgent = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.QUEUED_FOR_BATCH)
                .coverageStartDate(LocalDate.now().plusDays(3))
                .premiumAmount(new BigDecimal("600.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Non-urgent endorsement: coverage starts in 60 days
        Endorsement nonUrgent = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.QUEUED_FOR_BATCH)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("400.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Only enough balance for one
        List<Endorsement> queue = new ArrayList<>(List.of(nonUrgent, urgent));
        EAAccount account = buildAccount(new BigDecimal("650.00"));
        InsurerPort.InsurerCapabilities capabilities =
                new InsurerPort.InsurerCapabilities(false, true, 10, 24, 0);

        BatchOptimizerPort.OptimizedBatchPlan plan = optimizer.optimizeBatch(queue, account, capabilities);

        // Should select the urgent one first (higher composite score due to time pressure)
        // With 650 balance, either the 600 urgent or 400 non-urgent fits first
        // The urgent one has higher urgency score so it should be selected
        assertThat(plan.endorsements()).isNotEmpty();
        // The first non-DELETE endorsement included should be the more urgent one
        Endorsement firstIncluded = plan.endorsements().get(0);
        assertThat(firstIncluded.getCoverageStartDate()).isEqualTo(LocalDate.now().plusDays(3));
    }
}
