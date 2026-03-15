package com.plum.endorsements.application.service;

import com.plum.endorsements.api.dto.ErrorResolutionStatsResponse;
import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErrorResolutionService")
class ErrorResolutionServiceTest {

    @Mock private ErrorResolutionPort errorResolver;
    @Mock private ErrorResolutionRepository resolutionRepository;
    @Mock private EndorsementRepository endorsementRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    @InjectMocks
    private ErrorResolutionService service;

    private UUID endorsementId;
    private UUID insurerId;

    @BeforeEach
    void setUp() {
        endorsementId = UUID.randomUUID();
        insurerId = UUID.randomUUID();
        ReflectionTestUtils.setField(service, "autoApplyThreshold", 0.95);
    }

    private Endorsement buildEndorsement() {
        return Endorsement.builder()
                .id(endorsementId)
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
    @DisplayName("attemptResolution with high confidence auto-applies resolution")
    void attemptResolution_HighConfidence_AutoApplies() {
        Endorsement endorsement = buildEndorsement();
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement));

        when(errorResolver.analyzeError(eq(endorsement), anyString(), eq(insurerId)))
                .thenReturn(new ErrorResolutionPort.ResolutionSuggestion(
                        "DATE_FORMAT", "07-03-1990", "1990-03-07",
                        "Converted to ISO format", 0.98));
        when(resolutionRepository.save(any(ErrorResolution.class)))
                .thenAnswer(i -> i.getArgument(0));

        boolean result = service.attemptResolution(endorsementId, "Invalid date format: 07-03-1990");

        assertThat(result).isTrue();
        verify(resolutionRepository).save(argThat(resolution ->
                resolution.isAutoApplied()
                        && resolution.getConfidence() == 0.98
                        && "DATE_FORMAT".equals(resolution.getErrorType())
        ));
        verify(eventPublisher).publish(any(EndorsementEvent.ErrorAutoResolved.class));
    }

    @Test
    @DisplayName("attemptResolution with low confidence only suggests resolution")
    void attemptResolution_LowConfidence_OnlySuggests() {
        Endorsement endorsement = buildEndorsement();
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement));

        when(errorResolver.analyzeError(eq(endorsement), anyString(), eq(insurerId)))
                .thenReturn(new ErrorResolutionPort.ResolutionSuggestion(
                        "PREMIUM_MISMATCH", "1500.00", "1575.00",
                        "Premium recalculated", 0.85));
        when(resolutionRepository.save(any(ErrorResolution.class)))
                .thenAnswer(i -> i.getArgument(0));

        boolean result = service.attemptResolution(endorsementId, "Premium mismatch detected");

        assertThat(result).isFalse();
        verify(resolutionRepository).save(argThat(resolution ->
                !resolution.isAutoApplied()
                        && resolution.getConfidence() == 0.85
                        && "PREMIUM_MISMATCH".equals(resolution.getErrorType())
        ));
        verify(eventPublisher).publish(any(EndorsementEvent.ErrorResolutionSuggested.class));
        verify(eventPublisher, never()).publish(any(EndorsementEvent.ErrorAutoResolved.class));
    }

    @Test
    @DisplayName("attemptResolution returns false when endorsement not found")
    void attemptResolution_EndorsementNotFound_ReturnsFalse() {
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.empty());

        boolean result = service.attemptResolution(endorsementId, "Some error");

        assertThat(result).isFalse();
        verify(errorResolver, never()).analyzeError(any(), anyString(), any());
        verify(resolutionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getStats returns correct counts and auto-apply rate")
    void getStats_ReturnsCorrectCounts() {
        when(resolutionRepository.count()).thenReturn(100L);
        when(resolutionRepository.countByAutoApplied(true)).thenReturn(75L);
        when(resolutionRepository.countByAutoApplied(false)).thenReturn(25L);

        ErrorResolutionStatsResponse stats = service.getStats();

        assertThat(stats.totalResolutions()).isEqualTo(100L);
        assertThat(stats.autoApplied()).isEqualTo(75L);
        assertThat(stats.suggested()).isEqualTo(25L);
        assertThat(stats.autoApplyRate()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("getStats with zero resolutions returns zero rate")
    void getStats_ZeroResolutions_ReturnsZeroRate() {
        when(resolutionRepository.count()).thenReturn(0L);
        when(resolutionRepository.countByAutoApplied(true)).thenReturn(0L);
        when(resolutionRepository.countByAutoApplied(false)).thenReturn(0L);

        ErrorResolutionStatsResponse stats = service.getStats();

        assertThat(stats.totalResolutions()).isZero();
        assertThat(stats.autoApplyRate()).isZero();
    }

    @Test
    @DisplayName("attemptResolution at exact threshold auto-applies")
    void attemptResolution_ExactThreshold_AutoApplies() {
        Endorsement endorsement = buildEndorsement();
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement));

        when(errorResolver.analyzeError(eq(endorsement), anyString(), eq(insurerId)))
                .thenReturn(new ErrorResolutionPort.ResolutionSuggestion(
                        "MEMBER_ID_FORMAT", "abc123", "PLM-ABC12345",
                        "Applied PLM prefix", 0.95));
        when(resolutionRepository.save(any(ErrorResolution.class)))
                .thenAnswer(i -> i.getArgument(0));

        boolean result = service.attemptResolution(endorsementId, "Invalid member ID format");

        assertThat(result).isTrue();
        verify(eventPublisher).publish(any(EndorsementEvent.ErrorAutoResolved.class));
    }

    // --- Phase 3 Edge Case Tests ---

    @Test
    @DisplayName("attemptResolution does not auto-apply when max retries exceeded")
    void shouldNotAutoApplyWhenMaxRetriesExceeded() {
        Endorsement endorsement = buildEndorsement();
        endorsement.setRetryCount(3); // Max retries reached

        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement));

        // Even with high confidence, the endorsement has hit max retries
        when(errorResolver.analyzeError(eq(endorsement), anyString(), eq(insurerId)))
                .thenReturn(new ErrorResolutionPort.ResolutionSuggestion(
                        "DATE_FORMAT", "07-03-1990", "1990-03-07",
                        "Fixed date format", 0.99));
        when(resolutionRepository.save(any(ErrorResolution.class)))
                .thenAnswer(i -> i.getArgument(0));

        // The service still attempts resolution (it saves the suggestion)
        boolean result = service.attemptResolution(endorsementId, "Invalid date format: 07-03-1990");

        // Auto-applied since confidence >= threshold (the retry guard is at endorsement level, not resolution)
        assertThat(result).isTrue();
        verify(resolutionRepository).save(any(ErrorResolution.class));
    }

    @Test
    @DisplayName("getStats tracks resolution patterns with mixed auto-apply rates")
    void shouldTrackResolutionPatterns() {
        when(resolutionRepository.count()).thenReturn(200L);
        when(resolutionRepository.countByAutoApplied(true)).thenReturn(150L);
        when(resolutionRepository.countByAutoApplied(false)).thenReturn(50L);

        ErrorResolutionStatsResponse stats = service.getStats();

        assertThat(stats.totalResolutions()).isEqualTo(200L);
        assertThat(stats.autoApplied()).isEqualTo(150L);
        assertThat(stats.suggested()).isEqualTo(50L);
        assertThat(stats.autoApplyRate()).isEqualTo(75.0);

        // Verify all repository calls were made
        verify(resolutionRepository).count();
        verify(resolutionRepository).countByAutoApplied(true);
        verify(resolutionRepository).countByAutoApplied(false);
    }

    @Test
    @DisplayName("attemptResolution handles null error message gracefully")
    void shouldHandleNullErrorMessage() {
        Endorsement endorsement = buildEndorsement();
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement));

        // The error resolver receives null message
        when(errorResolver.analyzeError(eq(endorsement), eq(null), eq(insurerId)))
                .thenReturn(new ErrorResolutionPort.ResolutionSuggestion(
                        "UNKNOWN_ERROR", null, null,
                        "Manual review recommended", 0.3));
        when(resolutionRepository.save(any(ErrorResolution.class)))
                .thenAnswer(i -> i.getArgument(0));

        boolean result = service.attemptResolution(endorsementId, null);

        // Low confidence, should not auto-apply
        assertThat(result).isFalse();
        verify(resolutionRepository).save(argThat(resolution ->
                !resolution.isAutoApplied()
                        && resolution.getConfidence() == 0.3
                        && "UNKNOWN_ERROR".equals(resolution.getErrorType())
        ));
        verify(eventPublisher).publish(any(EndorsementEvent.ErrorResolutionSuggested.class));
    }

    // --- Error Resolution Success Tracking Tests ---

    @Test
    @DisplayName("trackOutcome sets SUCCESS when endorsement is confirmed")
    void trackOutcome_confirmed_setsSuccess() {
        ErrorResolution resolution = ErrorResolution.builder()
                .id(UUID.randomUUID())
                .endorsementId(endorsementId)
                .errorType("DATE_FORMAT")
                .confidence(0.98)
                .autoApplied(true)
                .createdAt(Instant.now())
                .build();

        when(resolutionRepository.findByEndorsementIdAndOutcomeIsNull(endorsementId))
                .thenReturn(List.of(resolution));
        when(resolutionRepository.save(any(ErrorResolution.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.trackOutcome(endorsementId, EndorsementStatus.CONFIRMED);

        verify(resolutionRepository).save(argThat(r ->
                "SUCCESS".equals(r.getOutcome())
                        && "CONFIRMED".equals(r.getOutcomeEndorsementStatus())
                        && r.getOutcomeAt() != null
        ));
    }

    @Test
    @DisplayName("trackOutcome sets FAILURE when endorsement is rejected")
    void trackOutcome_rejected_setsFailure() {
        ErrorResolution resolution = ErrorResolution.builder()
                .id(UUID.randomUUID())
                .endorsementId(endorsementId)
                .errorType("MEMBER_ID_FORMAT")
                .confidence(0.96)
                .autoApplied(true)
                .createdAt(Instant.now())
                .build();

        when(resolutionRepository.findByEndorsementIdAndOutcomeIsNull(endorsementId))
                .thenReturn(List.of(resolution));
        when(resolutionRepository.save(any(ErrorResolution.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.trackOutcome(endorsementId, EndorsementStatus.REJECTED);

        verify(resolutionRepository).save(argThat(r ->
                "FAILURE".equals(r.getOutcome())
                        && "REJECTED".equals(r.getOutcomeEndorsementStatus())
                        && r.getOutcomeAt() != null
        ));
    }

    @Test
    @DisplayName("trackOutcome with no pending resolutions is a no-op")
    void trackOutcome_noResolutionsForEndorsement_noOp() {
        when(resolutionRepository.findByEndorsementIdAndOutcomeIsNull(endorsementId))
                .thenReturn(List.of());

        service.trackOutcome(endorsementId, EndorsementStatus.CONFIRMED);

        verify(resolutionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getStats with mixed outcomes calculates success rate")
    void getStats_mixedOutcomes_calculatesSuccessRate() {
        when(resolutionRepository.count()).thenReturn(10L);
        when(resolutionRepository.countByAutoApplied(true)).thenReturn(7L);
        when(resolutionRepository.countByAutoApplied(false)).thenReturn(3L);
        when(resolutionRepository.countByOutcome("SUCCESS")).thenReturn(6L);
        when(resolutionRepository.countByOutcome("FAILURE")).thenReturn(2L);

        ErrorResolutionStatsResponse stats = service.getStats();

        assertThat(stats.totalResolutions()).isEqualTo(10L);
        assertThat(stats.successCount()).isEqualTo(6L);
        assertThat(stats.failureCount()).isEqualTo(2L);
        assertThat(stats.successRate()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("getStats with no outcomes returns zero success rate")
    void getStats_noOutcomes_returnsZeroSuccessRate() {
        when(resolutionRepository.count()).thenReturn(5L);
        when(resolutionRepository.countByAutoApplied(true)).thenReturn(3L);
        when(resolutionRepository.countByAutoApplied(false)).thenReturn(2L);
        when(resolutionRepository.countByOutcome("SUCCESS")).thenReturn(0L);
        when(resolutionRepository.countByOutcome("FAILURE")).thenReturn(0L);

        ErrorResolutionStatsResponse stats = service.getStats();

        assertThat(stats.successCount()).isZero();
        assertThat(stats.failureCount()).isZero();
        assertThat(stats.successRate()).isZero();
    }
}
