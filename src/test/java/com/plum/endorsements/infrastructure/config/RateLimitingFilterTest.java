package com.plum.endorsements.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitingFilter.TokenBucket")
class RateLimitingFilterTest {

    @Test
    @DisplayName("requests within limit pass through")
    void tryConsume_WithinLimit_ReturnsTrue() {
        var bucket = new RateLimitingFilter.TokenBucket(10, 10);

        for (int i = 0; i < 10; i++) {
            assertThat(bucket.tryConsume()).isTrue();
        }
    }

    @Test
    @DisplayName("requests exceeding burst size are rejected")
    void tryConsume_ExceedingBurstSize_ReturnsFalse() {
        var bucket = new RateLimitingFilter.TokenBucket(5, 5);

        // Consume all tokens
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume()).isTrue();
        }

        // Next request should be rejected
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    @DisplayName("tokens replenish after time passes")
    void tryConsume_AfterWaiting_TokensReplenish() throws InterruptedException {
        var bucket = new RateLimitingFilter.TokenBucket(5, 100);

        // Consume all tokens
        for (int i = 0; i < 5; i++) {
            bucket.tryConsume();
        }

        assertThat(bucket.tryConsume()).isFalse();

        // Wait for tokens to replenish (100 tokens/sec = 1 token per 10ms)
        Thread.sleep(50);

        assertThat(bucket.tryConsume()).isTrue();
    }

    @Test
    @DisplayName("tokens do not exceed max burst size after long wait")
    void availableTokens_NeverExceedsMax() throws InterruptedException {
        var bucket = new RateLimitingFilter.TokenBucket(10, 100);

        Thread.sleep(200);

        assertThat(bucket.availableTokens()).isLessThanOrEqualTo(10.0);
    }
}
