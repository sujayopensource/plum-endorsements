package com.plum.endorsements.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ErrorResolution domain model")
class ErrorResolutionTest {

    private ErrorResolution buildResolution(double confidence) {
        return ErrorResolution.builder()
                .id(UUID.randomUUID())
                .endorsementId(UUID.randomUUID())
                .errorType("DATE_FORMAT")
                .originalValue("07-03-1990")
                .correctedValue("1990-03-07")
                .resolution("Converted DD-MM-YYYY to YYYY-MM-DD")
                .confidence(confidence)
                .autoApplied(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("shouldAutoApply returns true when confidence >= threshold")
    void shouldAutoApply_ConfidenceAboveThreshold_ReturnsTrue() {
        ErrorResolution resolution = buildResolution(0.98);

        assertThat(resolution.shouldAutoApply(0.95)).isTrue();
    }

    @Test
    @DisplayName("shouldAutoApply returns true when confidence equals threshold exactly")
    void shouldAutoApply_ConfidenceEqualsThreshold_ReturnsTrue() {
        ErrorResolution resolution = buildResolution(0.95);

        assertThat(resolution.shouldAutoApply(0.95)).isTrue();
    }

    @Test
    @DisplayName("shouldAutoApply returns false when confidence < threshold")
    void shouldAutoApply_ConfidenceBelowThreshold_ReturnsFalse() {
        ErrorResolution resolution = buildResolution(0.85);

        assertThat(resolution.shouldAutoApply(0.95)).isFalse();
    }

    @Test
    @DisplayName("shouldAutoApply with zero confidence and zero threshold returns true")
    void shouldAutoApply_ZeroConfidenceZeroThreshold_ReturnsTrue() {
        ErrorResolution resolution = buildResolution(0.0);

        assertThat(resolution.shouldAutoApply(0.0)).isTrue();
    }

    @Test
    @DisplayName("shouldAutoApply with low threshold accepts moderate confidence")
    void shouldAutoApply_LowThreshold_AcceptsModerateConfidence() {
        ErrorResolution resolution = buildResolution(0.5);

        assertThat(resolution.shouldAutoApply(0.3)).isTrue();
        assertThat(resolution.shouldAutoApply(0.7)).isFalse();
    }

    @Test
    @DisplayName("builder sets all properties correctly")
    void builder_SetsAllProperties() {
        UUID id = UUID.randomUUID();
        UUID endorsementId = UUID.randomUUID();
        Instant now = Instant.now();

        ErrorResolution resolution = ErrorResolution.builder()
                .id(id)
                .endorsementId(endorsementId)
                .errorType("MISSING_FIELD")
                .originalValue("email")
                .correctedValue("not-provided@employer.com")
                .resolution("Applied default value")
                .confidence(0.90)
                .autoApplied(true)
                .createdAt(now)
                .build();

        assertThat(resolution.getId()).isEqualTo(id);
        assertThat(resolution.getEndorsementId()).isEqualTo(endorsementId);
        assertThat(resolution.getErrorType()).isEqualTo("MISSING_FIELD");
        assertThat(resolution.getOriginalValue()).isEqualTo("email");
        assertThat(resolution.getCorrectedValue()).isEqualTo("not-provided@employer.com");
        assertThat(resolution.getResolution()).isEqualTo("Applied default value");
        assertThat(resolution.getConfidence()).isEqualTo(0.90);
        assertThat(resolution.isAutoApplied()).isTrue();
        assertThat(resolution.getCreatedAt()).isEqualTo(now);
    }
}
