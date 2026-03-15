package com.plum.endorsements.domain.service;

import com.plum.endorsements.domain.model.EAAccount;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementPriority;
import com.plum.endorsements.domain.model.EndorsementType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class EABalanceCalculator {

    private final BigDecimal safetyMargin;

    public EABalanceCalculator(
            @Value("${endorsement.ea.safety-margin-pct:0.10}") BigDecimal safetyMargin) {
        this.safetyMargin = safetyMargin;
    }

    public BigDecimal calculateRequiredBalance(List<Endorsement> pendingAdditions) {
        return pendingAdditions.stream()
                .map(Endorsement::getPremiumAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateExpectedCredits(List<Endorsement> pendingDeletions) {
        return pendingDeletions.stream()
                .map(Endorsement::getPremiumAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean hasSufficientFunds(EAAccount account, BigDecimal requiredAmount) {
        return account.availableBalance().compareTo(requiredAmount) >= 0;
    }

    /**
     * Sort endorsements for optimal balance utilization:
     * P0 (deletions) first to free up balance, then P1 (cost-neutral),
     * then P2 (additions by coverage date), then P3 (premium updates).
     */
    public List<Endorsement> sequenceForOptimalBalance(List<Endorsement> endorsements) {
        List<Endorsement> sorted = new ArrayList<>(endorsements);
        sorted.sort(Comparator
                .comparingInt((Endorsement e) -> EndorsementPriority.classify(e).getRank())
                .thenComparing(e -> e.getCoverageStartDate() != null
                        ? e.getCoverageStartDate() : java.time.LocalDate.MAX));
        return sorted;
    }

    /**
     * Construct an optimized batch respecting balance constraints.
     * Process P0 (deletions) first to maximize available balance,
     * then include additions and updates that the balance can support.
     */
    public BatchPlan constructOptimizedBatch(List<Endorsement> endorsements,
                                              EAAccount account, int maxBatchSize) {
        List<Endorsement> sequenced = sequenceForOptimalBalance(endorsements);
        List<Endorsement> included = new ArrayList<>();
        List<Endorsement> deferred = new ArrayList<>();
        BigDecimal runningBalance = account.availableBalance();

        for (Endorsement e : sequenced) {
            if (included.size() >= maxBatchSize) {
                deferred.add(e);
                continue;
            }

            BigDecimal impact = calculateImpact(e);

            if (impact.signum() <= 0) {
                // Deletions and cost-neutral: always include, they free up balance
                included.add(e);
                runningBalance = runningBalance.subtract(impact); // subtract negative = add
            } else if (runningBalance.compareTo(impact) >= 0) {
                // Additions/updates: only if balance supports it
                included.add(e);
                runningBalance = runningBalance.subtract(impact);
            } else {
                deferred.add(e);
            }
        }

        return new BatchPlan(included, deferred, runningBalance);
    }

    /**
     * Forecast the balance after processing a list of endorsements.
     * Includes a 10% safety margin on the required minimum.
     */
    public BalanceForecast forecastBalance(EAAccount account, List<Endorsement> endorsements) {
        BigDecimal totalRequired = BigDecimal.ZERO;

        for (Endorsement e : endorsements) {
            BigDecimal impact = calculateImpact(e);
            if (impact.signum() > 0) {
                totalRequired = totalRequired.add(impact);
            }
        }

        BigDecimal expectedCredits = BigDecimal.ZERO;
        for (Endorsement e : endorsements) {
            BigDecimal impact = calculateImpact(e);
            if (impact.signum() < 0) {
                expectedCredits = expectedCredits.add(impact.negate());
            }
        }

        BigDecimal netRequired = totalRequired.subtract(expectedCredits);
        BigDecimal safetyAmount = netRequired.multiply(safetyMargin).setScale(2, RoundingMode.CEILING);
        BigDecimal requiredMinimum = netRequired.add(safetyAmount);

        BigDecimal shortfall = requiredMinimum.subtract(account.availableBalance());
        boolean topUpRequired = shortfall.signum() > 0;

        return new BalanceForecast(requiredMinimum, shortfall.max(BigDecimal.ZERO), topUpRequired);
    }

    private BigDecimal calculateImpact(Endorsement e) {
        BigDecimal amount = e.getPremiumAmount() != null ? e.getPremiumAmount() : BigDecimal.ZERO;
        return switch (e.getType()) {
            case DELETE -> amount.negate();  // Deletions free up balance
            case ADD -> amount;             // Additions consume balance
            case UPDATE -> amount;          // Updates may consume balance
        };
    }

    public record BatchPlan(List<Endorsement> included, List<Endorsement> deferred,
                            BigDecimal projectedBalance) {}

    public record BalanceForecast(BigDecimal requiredMinimum, BigDecimal shortfall,
                                  boolean topUpRequired) {}
}
