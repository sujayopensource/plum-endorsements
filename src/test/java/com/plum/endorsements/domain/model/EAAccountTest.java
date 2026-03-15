package com.plum.endorsements.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class EAAccountTest {

    private EAAccount buildAccount(BigDecimal balance, BigDecimal reserved) {
        return EAAccount.builder()
                .employerId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .balance(balance)
                .reserved(reserved)
                .updatedAt(Instant.now())
                .build();
    }

    // --- availableBalance tests ---

    @Test
    void availableBalance_returnsBalanceMinusReserved() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), new BigDecimal("200.00"));

        assertThat(account.availableBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    void availableBalance_returnsFullBalanceWhenNothingReserved() {
        EAAccount account = buildAccount(new BigDecimal("5000.00"), BigDecimal.ZERO);

        assertThat(account.availableBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void availableBalance_returnsZeroWhenFullyReserved() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), new BigDecimal("1000.00"));

        assertThat(account.availableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- canFund tests ---

    @Test
    void canFund_returnsTrueWhenAvailableBalanceIsGreaterThanAmount() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), BigDecimal.ZERO);

        assertThat(account.canFund(new BigDecimal("500.00"))).isTrue();
    }

    @Test
    void canFund_returnsTrueWhenAvailableBalanceEqualsAmount() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), BigDecimal.ZERO);

        assertThat(account.canFund(new BigDecimal("1000.00"))).isTrue();
    }

    @Test
    void canFund_returnsFalseWhenAvailableBalanceIsLessThanAmount() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), BigDecimal.ZERO);

        assertThat(account.canFund(new BigDecimal("1500.00"))).isFalse();
    }

    @Test
    void canFund_accountsForReservedAmount() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), new BigDecimal("600.00"));

        assertThat(account.canFund(new BigDecimal("500.00"))).isFalse();
        assertThat(account.canFund(new BigDecimal("400.00"))).isTrue();
    }

    // --- reserve tests ---

    @Test
    void reserve_increasesReservedAmount() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), BigDecimal.ZERO);
        Instant beforeReserve = account.getUpdatedAt();

        account.reserve(new BigDecimal("300.00"));

        assertThat(account.getReserved()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(account.availableBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(account.getUpdatedAt()).isAfterOrEqualTo(beforeReserve);
    }

    @Test
    void reserve_multipleReservations_accumulateCorrectly() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), BigDecimal.ZERO);

        account.reserve(new BigDecimal("200.00"));
        account.reserve(new BigDecimal("300.00"));

        assertThat(account.getReserved()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(account.availableBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void reserve_throwsIllegalStateException_whenInsufficientFunds() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), new BigDecimal("800.00"));

        assertThatThrownBy(() -> account.reserve(new BigDecimal("300.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient available balance to reserve 300.00");
    }

    @Test
    void reserve_throwsIllegalStateException_doesNotChangeReserved() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), new BigDecimal("800.00"));
        BigDecimal originalReserved = account.getReserved();

        assertThatThrownBy(() -> account.reserve(new BigDecimal("300.00")))
                .isInstanceOf(IllegalStateException.class);

        // Reserved should be rolled back to original value
        assertThat(account.getReserved()).isEqualByComparingTo(originalReserved);
    }

    @Test
    void reserve_exactlyAvailableBalance_shouldSucceed() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), new BigDecimal("500.00"));

        account.reserve(new BigDecimal("500.00"));

        assertThat(account.getReserved()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(account.availableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- releaseReservation tests ---

    @Test
    void releaseReservation_decreasesReservedAmount() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), new BigDecimal("500.00"));
        Instant beforeRelease = account.getUpdatedAt();

        account.releaseReservation(new BigDecimal("200.00"));

        assertThat(account.getReserved()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(account.availableBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(account.getUpdatedAt()).isAfterOrEqualTo(beforeRelease);
    }

    @Test
    void releaseReservation_fullRelease() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), new BigDecimal("500.00"));

        account.releaseReservation(new BigDecimal("500.00"));

        assertThat(account.getReserved()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.availableBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    // --- debit tests ---

    @Test
    void debit_decreasesBalance() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), BigDecimal.ZERO);
        Instant beforeDebit = account.getUpdatedAt();

        account.debit(new BigDecimal("250.00"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(account.getUpdatedAt()).isAfterOrEqualTo(beforeDebit);
    }

    @Test
    void debit_fullBalance() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), BigDecimal.ZERO);

        account.debit(new BigDecimal("1000.00"));

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- credit tests ---

    @Test
    void credit_increasesBalance() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), BigDecimal.ZERO);
        Instant beforeCredit = account.getUpdatedAt();

        account.credit(new BigDecimal("500.00"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(account.getUpdatedAt()).isAfterOrEqualTo(beforeCredit);
    }

    @Test
    void credit_onZeroBalance() {
        EAAccount account = buildAccount(BigDecimal.ZERO, BigDecimal.ZERO);

        account.credit(new BigDecimal("750.00"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
    }

    // --- version tests ---

    @Test
    void version_settableAndGettableViaBuilder() {
        EAAccount account = EAAccount.builder()
                .employerId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .balance(new BigDecimal("1000.00"))
                .reserved(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .version(5L)
                .build();

        assertThat(account.getVersion()).isEqualTo(5L);
    }

    // --- Combined scenario tests ---

    @Test
    void reserveThenReleaseThenDebit_maintainsCorrectState() {
        EAAccount account = buildAccount(new BigDecimal("1000.00"), BigDecimal.ZERO);

        account.reserve(new BigDecimal("300.00"));
        assertThat(account.availableBalance()).isEqualByComparingTo(new BigDecimal("700.00"));

        account.releaseReservation(new BigDecimal("300.00"));
        assertThat(account.availableBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));

        account.debit(new BigDecimal("300.00"));
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(account.availableBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
    }
}
