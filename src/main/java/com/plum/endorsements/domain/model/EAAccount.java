package com.plum.endorsements.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EAAccount {

    private UUID employerId;
    private UUID insurerId;
    private BigDecimal balance;
    private BigDecimal reserved;
    private Instant updatedAt;
    private Long version;

    /**
     * Returns the effective available balance (balance minus reserved).
     */
    public BigDecimal availableBalance() {
        return balance.subtract(reserved);
    }

    /**
     * Returns {@code true} if the available balance is sufficient to fund
     * the given amount.
     */
    public boolean canFund(BigDecimal amount) {
        return availableBalance().compareTo(amount) >= 0;
    }

    /**
     * Reserves the specified amount, adding it to the reserved total.
     *
     * @param amount the amount to reserve
     * @throws IllegalStateException if the reservation would cause the
     *                               available balance to become negative
     */
    public void reserve(BigDecimal amount) {
        this.reserved = this.reserved.add(amount);
        if (availableBalance().compareTo(BigDecimal.ZERO) < 0) {
            this.reserved = this.reserved.subtract(amount);
            throw new IllegalStateException(
                    "Insufficient available balance to reserve " + amount);
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Releases a previously held reservation, subtracting the amount from
     * the reserved total.
     */
    public void releaseReservation(BigDecimal amount) {
        this.reserved = this.reserved.subtract(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Debits (subtracts) the specified amount from the balance.
     */
    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Credits (adds) the specified amount to the balance.
     */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }
}
