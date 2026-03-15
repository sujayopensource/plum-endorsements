package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.EAAccount;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementPriority;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.port.BatchOptimizerPort;
import com.plum.endorsements.domain.port.InsurerPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "endorsement.intelligence.batch-optimizer.enabled", havingValue = "true", matchIfMissing = true)
public class ConstraintBatchOptimizer implements BatchOptimizerPort {

    private final MeterRegistry meterRegistry;

    private static final double URGENCY_WEIGHT = 0.6;
    private static final double EA_IMPACT_WEIGHT = 0.4;

    @Override
    public OptimizedBatchPlan optimizeBatch(List<Endorsement> queue, EAAccount account,
                                             InsurerPort.InsurerCapabilities capabilities) {
        long startTime = System.currentTimeMillis();

        // 1. Score each endorsement
        List<ScoredEndorsement> scored = queue.stream()
                .map(e -> new ScoredEndorsement(e, calculateCompositeScore(e, account)))
                .sorted(Comparator.comparingDouble(ScoredEndorsement::score).reversed())
                .toList();

        // 2. Apply DP knapsack for optimal packing with balance constraint
        List<Endorsement> optimized = new ArrayList<>();
        BigDecimal runningBalance = account != null ? account.availableBalance() : BigDecimal.valueOf(Long.MAX_VALUE);
        int maxBatchSize = capabilities.maxBatchSize();

        // Process deletions first (they free up balance — always beneficial)
        for (ScoredEndorsement se : scored) {
            if (optimized.size() >= maxBatchSize) break;
            if (se.endorsement.getType() == EndorsementType.DELETE) {
                optimized.add(se.endorsement);
                BigDecimal credit = se.endorsement.getPremiumAmount() != null
                        ? se.endorsement.getPremiumAmount() : BigDecimal.ZERO;
                runningBalance = runningBalance.add(credit);
            }
        }

        // Then use 0-1 knapsack DP for additions/updates to maximize total score within balance
        List<ScoredEndorsement> nonDeletes = scored.stream()
                .filter(se -> se.endorsement.getType() != EndorsementType.DELETE)
                .toList();

        int remainingSlots = maxBatchSize - optimized.size();
        if (!nonDeletes.isEmpty() && remainingSlots > 0) {
            List<Endorsement> knapsackResult = solveKnapsack(nonDeletes, runningBalance, remainingSlots);
            optimized.addAll(knapsackResult);
        }

        // Calculate savings vs naive approach
        BigDecimal naiveCost = calculateNaiveCost(queue, maxBatchSize);
        BigDecimal optimizedCost = calculateOptimizedCost(optimized);
        BigDecimal savings = naiveCost.subtract(optimizedCost).max(BigDecimal.ZERO);

        long duration = System.currentTimeMillis() - startTime;

        meterRegistry.summary("endorsement.batch.optimization.savings",
                "strategy", "constraint_based").record(savings.doubleValue());
        meterRegistry.timer("endorsement.batch.optimization.duration")
                .record(java.time.Duration.ofMillis(duration));

        String strategy = String.format("DP knapsack optimization: %d of %d endorsed, " +
                "deletions first, 0-1 knapsack for additions. Savings: ₹%s",
                optimized.size(), queue.size(), savings.toPlainString());

        log.info("Batch optimization: {} items from {} queue, savings=₹{}, took={}ms",
                optimized.size(), queue.size(), savings, duration);

        return new OptimizedBatchPlan(optimized, strategy, savings, duration);
    }

    /**
     * 0-1 knapsack DP: maximize total composite score subject to balance capacity and slot limit.
     * Uses integer pennies for the DP table to avoid floating-point precision issues.
     */
    private List<Endorsement> solveKnapsack(List<ScoredEndorsement> items, BigDecimal capacity, int maxItems) {
        int n = items.size();
        // Convert capacity to integer pennies for DP table indexing
        long capacityPennies = capacity.multiply(BigDecimal.valueOf(100)).longValue();
        // Cap DP table to prevent memory explosion on very large balances
        int dpCapacity = (int) Math.min(capacityPennies, 1_000_000);

        // dp[i] = best score achievable with exactly i pennies of capacity used
        double[] dp = new double[dpCapacity + 1];
        boolean[][] selected = new boolean[n][dpCapacity + 1];

        for (int i = 0; i < n; i++) {
            BigDecimal cost = items.get(i).endorsement.getPremiumAmount() != null
                    ? items.get(i).endorsement.getPremiumAmount() : BigDecimal.ZERO;
            int costPennies = cost.multiply(BigDecimal.valueOf(100)).intValue();
            double value = items.get(i).score;

            // Traverse capacity backwards (standard 0-1 knapsack)
            for (int w = dpCapacity; w >= costPennies; w--) {
                if (dp[w - costPennies] + value > dp[w]) {
                    dp[w] = dp[w - costPennies] + value;
                    selected[i][w] = true;
                }
            }
        }

        // Backtrack to find selected items
        List<Endorsement> result = new ArrayList<>();
        int w = dpCapacity;
        for (int i = n - 1; i >= 0 && result.size() < maxItems; i--) {
            if (selected[i][w]) {
                result.add(items.get(i).endorsement);
                BigDecimal cost = items.get(i).endorsement.getPremiumAmount() != null
                        ? items.get(i).endorsement.getPremiumAmount() : BigDecimal.ZERO;
                w -= cost.multiply(BigDecimal.valueOf(100)).intValue();
            }
        }

        return result;
    }

    private double calculateCompositeScore(Endorsement e, EAAccount account) {
        double urgencyScore = calculateUrgencyScore(e);
        double eaImpactScore = calculateEAImpactScore(e, account);
        return urgencyScore * URGENCY_WEIGHT + eaImpactScore * EA_IMPACT_WEIGHT;
    }

    private double calculateUrgencyScore(Endorsement e) {
        // Higher priority = higher score
        int rank = EndorsementPriority.classify(e).getRank();
        double priorityScore = (4 - rank) / 4.0; // P0=1.0, P1=0.75, P2=0.5, P3=0.25

        // Days until coverage start (urgency factor)
        if (e.getCoverageStartDate() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), e.getCoverageStartDate());
            double timePressure = Math.max(0, 1.0 - (daysUntil / 30.0));
            return (priorityScore + timePressure) / 2.0;
        }

        return priorityScore;
    }

    private double calculateEAImpactScore(Endorsement e, EAAccount account) {
        if (e.getType() == EndorsementType.DELETE) {
            return 1.0; // Deletions always beneficial (free up balance)
        }
        if (account == null || e.getPremiumAmount() == null) {
            return 0.5;
        }
        BigDecimal available = account.availableBalance();
        if (available.signum() <= 0) return 0.0;

        // Lower impact ratio = better (uses less of available balance)
        double impactRatio = e.getPremiumAmount().doubleValue() / available.doubleValue();
        return Math.max(0, 1.0 - impactRatio);
    }

    private BigDecimal calculateNaiveCost(List<Endorsement> queue, int maxBatchSize) {
        return queue.stream()
                .limit(maxBatchSize)
                .filter(e -> e.getType() == EndorsementType.ADD || e.getType() == EndorsementType.UPDATE)
                .map(e -> e.getPremiumAmount() != null ? e.getPremiumAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateOptimizedCost(List<Endorsement> optimized) {
        return optimized.stream()
                .filter(e -> e.getType() == EndorsementType.ADD || e.getType() == EndorsementType.UPDATE)
                .map(e -> e.getPremiumAmount() != null ? e.getPremiumAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record ScoredEndorsement(Endorsement endorsement, double score) {}
}
