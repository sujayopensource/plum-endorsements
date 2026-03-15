package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.port.ErrorResolutionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SimulatedErrorResolver")
class SimulatedErrorResolverTest {

    private SimulatedErrorResolver resolver;
    private Endorsement endorsement;
    private UUID insurerId;

    @BeforeEach
    void setUp() {
        resolver = new SimulatedErrorResolver();
        insurerId = UUID.randomUUID();
        endorsement = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(insurerId)
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.REJECTED)
                .coverageStartDate(LocalDate.now().plusDays(10))
                .premiumAmount(new BigDecimal("1500.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("resolves date format error with high confidence (0.98)")
    void analyzeError_DateFormatError_HighConfidence() {
        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Invalid date format: DOB 07-03-1990 is not accepted", insurerId);

        assertThat(result.errorType()).isEqualTo("DATE_FORMAT");
        assertThat(result.confidence()).isEqualTo(0.98);
        assertThat(result.correctedValue()).isNotNull();
        assertThat(result.resolution()).contains("date_format_mismatch");
    }

    @Test
    @DisplayName("resolves missing field error with moderate confidence (0.90)")
    void analyzeError_MissingFieldError_ModerateConfidence() {
        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Required field 'email' is missing", insurerId);

        assertThat(result.errorType()).isEqualTo("MISSING_FIELD");
        assertThat(result.confidence()).isEqualTo(0.90);
        assertThat(result.originalValue()).isEqualTo("email");
        assertThat(result.correctedValue()).isEqualTo("not-provided@employer.com");
        assertThat(result.resolution()).contains("required_field_missing");
    }

    @Test
    @DisplayName("resolves member ID format error with high confidence (0.96)")
    void analyzeError_MemberIdError_HighConfidence() {
        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Invalid member id: not recognized by insurer system", insurerId);

        assertThat(result.errorType()).isEqualTo("MEMBER_ID_FORMAT");
        assertThat(result.confidence()).isEqualTo(0.96);
        assertThat(result.correctedValue()).startsWith("PLM-");
        assertThat(result.resolution()).contains("invalid_member_id");
    }

    @Test
    @DisplayName("resolves premium mismatch error with moderate confidence (0.85)")
    void analyzeError_PremiumMismatchError_ModerateConfidence() {
        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Premium amount mismatch detected", insurerId);

        assertThat(result.errorType()).isEqualTo("PREMIUM_MISMATCH");
        assertThat(result.confidence()).isEqualTo(0.85);
        assertThat(result.originalValue()).isEqualTo("1500.00");
        // Corrected value = 1500 * 1.05 = 1575.00
        assertThat(new BigDecimal(result.correctedValue()))
                .isEqualByComparingTo(new BigDecimal("1575.0000"));
        assertThat(result.resolution()).contains("premium_mismatch");
    }

    @Test
    @DisplayName("returns unknown error with low confidence (0.3) for unrecognized patterns")
    void analyzeError_UnknownError_LowConfidence() {
        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Some completely unknown error type xyz", insurerId);

        assertThat(result.errorType()).isEqualTo("UNKNOWN_ERROR");
        assertThat(result.confidence()).isEqualTo(0.3);
        assertThat(result.correctedValue()).isNull();
        assertThat(result.resolution()).contains("Manual review recommended");
    }

    @Test
    @DisplayName("handles null premium amount for premium mismatch error")
    void analyzeError_NullPremium_PremiumMismatch() {
        endorsement.setPremiumAmount(null);

        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Premium amount mismatch", insurerId);

        assertThat(result.errorType()).isEqualTo("PREMIUM_MISMATCH");
        assertThat(result.originalValue()).isEqualTo("0");
        assertThat(result.correctedValue()).isEqualTo("0");
    }

    // --- Phase 3 Edge Case Tests ---

    @Test
    @DisplayName("handles null premium amount gracefully for non-premium errors")
    void shouldHandleNullPremiumAmount() {
        endorsement.setPremiumAmount(null);

        // Test with a date format error when premium is null (should still work)
        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Invalid date format: DOB 15/06/1985", insurerId);

        assertThat(result.errorType()).isEqualTo("DATE_FORMAT");
        assertThat(result.confidence()).isEqualTo(0.98);
        // Date resolution should work regardless of premium being null
        assertThat(result.correctedValue()).isNotNull();
    }

    @Test
    @DisplayName("handles empty employee ID in member ID resolution")
    void shouldHandleEmptyEmployeeId() {
        // Set employee ID to a specific UUID to test the prefix logic
        UUID specificEmployeeId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        endorsement.setEmployeeId(specificEmployeeId);

        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Invalid member id: not recognized", insurerId);

        assertThat(result.errorType()).isEqualTo("MEMBER_ID_FORMAT");
        assertThat(result.confidence()).isEqualTo(0.96);
        assertThat(result.correctedValue()).startsWith("PLM-");
        // Should handle the UUID toString properly
        assertThat(result.correctedValue()).hasSize(12); // "PLM-" + 8 chars
    }

    @Test
    @DisplayName("extracts date from DD/MM/YYYY format and converts to ISO")
    void shouldExtractDateFromVariousFormats_SlashSeparated() {
        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Invalid date format: DOB 15/06/1985 is not accepted", insurerId);

        assertThat(result.errorType()).isEqualTo("DATE_FORMAT");
        assertThat(result.originalValue()).isEqualTo("15/06/1985");
        assertThat(result.correctedValue()).isEqualTo("1985-06-15");
        assertThat(result.confidence()).isEqualTo(0.98);
    }

    @Test
    @DisplayName("extracts date from DD-MM-YYYY format and converts to ISO")
    void shouldExtractDateFromVariousFormats_DashSeparated() {
        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Invalid date format: DOB 25-12-1992 is not accepted", insurerId);

        assertThat(result.errorType()).isEqualTo("DATE_FORMAT");
        assertThat(result.originalValue()).isEqualTo("25-12-1992");
        assertThat(result.correctedValue()).isEqualTo("1992-12-25");
        assertThat(result.confidence()).isEqualTo(0.98);
    }
}
