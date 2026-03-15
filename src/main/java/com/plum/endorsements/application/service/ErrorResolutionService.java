package com.plum.endorsements.application.service;

import com.plum.endorsements.api.dto.ErrorResolutionStatsResponse;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.ErrorResolution;
import com.plum.endorsements.domain.port.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorResolutionService {

    private final ErrorResolutionPort errorResolver;
    private final ErrorResolutionRepository resolutionRepository;
    private final EndorsementRepository endorsementRepository;
    private final EventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    @Value("${endorsement.intelligence.error-resolution.auto-apply-threshold:0.95}")
    private double autoApplyThreshold;

    @Transactional
    public boolean attemptResolution(UUID endorsementId, String errorMessage) {
        Endorsement endorsement = endorsementRepository.findById(endorsementId).orElse(null);
        if (endorsement == null) {
            log.warn("Cannot resolve error for missing endorsement: {}", endorsementId);
            return false;
        }

        ErrorResolutionPort.ResolutionSuggestion suggestion = errorResolver.analyzeError(
                endorsement, errorMessage, endorsement.getInsurerId());

        boolean autoApplied = suggestion.confidence() >= autoApplyThreshold;

        ErrorResolution resolution = ErrorResolution.builder()
                .endorsementId(endorsementId)
                .errorType(suggestion.errorType())
                .originalValue(suggestion.originalValue())
                .correctedValue(suggestion.correctedValue())
                .resolution(suggestion.resolution())
                .confidence(suggestion.confidence())
                .autoApplied(autoApplied)
                .createdAt(Instant.now())
                .build();

        resolutionRepository.save(resolution);

        meterRegistry.summary("endorsement.error.resolution.confidence",
                "errorType", suggestion.errorType()).record(suggestion.confidence());

        if (autoApplied) {
            meterRegistry.counter("endorsement.error.auto_resolved",
                    "errorType", suggestion.errorType(),
                    "insurerId", endorsement.getInsurerId().toString()).increment();

            eventPublisher.publish(new EndorsementEvent.ErrorAutoResolved(
                    endorsementId, Instant.now(), endorsement.getEmployerId(),
                    suggestion.errorType(), suggestion.resolution(), true));

            log.info("Auto-resolved error for endorsement {}: type={}, confidence={:.2f}",
                    endorsementId, suggestion.errorType(), suggestion.confidence());

            return true;
        } else {
            meterRegistry.counter("endorsement.error.suggested",
                    "errorType", suggestion.errorType()).increment();

            eventPublisher.publish(new EndorsementEvent.ErrorResolutionSuggested(
                    endorsementId, Instant.now(), endorsement.getEmployerId(),
                    suggestion.errorType(), suggestion.resolution(), suggestion.confidence()));

            log.info("Suggested resolution for endorsement {}: type={}, confidence={:.2f} (below threshold {})",
                    endorsementId, suggestion.errorType(), suggestion.confidence(), autoApplyThreshold);

            return false;
        }
    }

    public List<ErrorResolution> findByEndorsementId(UUID endorsementId) {
        if (endorsementId != null) {
            return resolutionRepository.findByEndorsementId(endorsementId);
        }
        return List.of();
    }

    public ErrorResolutionStatsResponse getStats() {
        long total = resolutionRepository.count();
        long autoApplied = resolutionRepository.countByAutoApplied(true);
        long suggested = resolutionRepository.countByAutoApplied(false);
        double autoApplyRate = total > 0 ? (double) autoApplied / total * 100 : 0;

        long successCount = resolutionRepository.countByOutcome("SUCCESS");
        long failureCount = resolutionRepository.countByOutcome("FAILURE");
        long totalWithOutcome = successCount + failureCount;
        double successRate = totalWithOutcome > 0 ? (double) successCount / totalWithOutcome * 100 : 0;

        return new ErrorResolutionStatsResponse(total, autoApplied, suggested, autoApplyRate,
                successCount, failureCount, successRate);
    }

    @Transactional
    public void trackOutcome(UUID endorsementId, EndorsementStatus status) {
        List<ErrorResolution> pending = resolutionRepository
                .findByEndorsementIdAndOutcomeIsNull(endorsementId);

        if (pending.isEmpty()) {
            return;
        }

        String outcome;
        if (status == EndorsementStatus.CONFIRMED) {
            outcome = "SUCCESS";
        } else {
            outcome = "FAILURE";
        }

        for (ErrorResolution resolution : pending) {
            resolution.recordOutcome(outcome, status.name());
            resolutionRepository.save(resolution);
        }

        log.info("Tracked {} outcome for {} resolutions on endorsement {}",
                outcome, pending.size(), endorsementId);
    }

    @Transactional
    public void approveResolution(UUID resolutionId) {
        resolutionRepository.findById(resolutionId).ifPresent(resolution -> {
            resolution.setAutoApplied(true);
            resolutionRepository.save(resolution);
            log.info("Resolution {} approved for endorsement {}", resolutionId, resolution.getEndorsementId());
        });
    }
}
