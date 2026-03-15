package com.plum.endorsements.application.handler;

import com.plum.endorsements.application.exception.EndorsementNotFoundException;
import com.plum.endorsements.application.service.ErrorResolutionService;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.ProvisionalCoverage;
import com.plum.endorsements.domain.port.EAAccountRepository;
import com.plum.endorsements.domain.port.EndorsementRepository;
import com.plum.endorsements.domain.port.EventPublisher;
import com.plum.endorsements.domain.port.InsurerPort;
import com.plum.endorsements.domain.port.InsurerPort.InsurerCapabilities;
import com.plum.endorsements.domain.port.InsurerPort.SubmissionResult;
import com.plum.endorsements.domain.port.NotificationPort;
import com.plum.endorsements.domain.port.ProvisionalCoverageRepository;
import com.plum.endorsements.domain.service.EndorsementStateMachine;
import com.plum.endorsements.infrastructure.insurer.InsurerRouter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessEndorsementHandler {

    private final EndorsementRepository endorsementRepository;
    private final EndorsementStateMachine stateMachine;
    private final InsurerRouter insurerRouter;
    private final EventPublisher eventPublisher;
    private final NotificationPort notificationPort;
    private final ProvisionalCoverageRepository provisionalCoverageRepository;
    private final EAAccountRepository eaAccountRepository;
    private final ErrorResolutionService errorResolutionService;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void submitToInsurer(UUID endorsementId) {
        try {
            Endorsement endorsement = endorsementRepository.findById(endorsementId)
                    .orElseThrow(() -> new EndorsementNotFoundException(endorsementId));

            MDC.put("endorsementId", endorsementId.toString());
            MDC.put("employerId", endorsement.getEmployerId().toString());

            InsurerPort insurerPort = insurerRouter.resolve(endorsement.getInsurerId());
            InsurerCapabilities capabilities = insurerPort.getCapabilities();

            if (capabilities.supportsRealTime()) {
                EndorsementStatus previousStatus = endorsement.getStatus();
                stateMachine.transition(endorsement, EndorsementStatus.SUBMITTED_REALTIME);
                meterRegistry.counter("endorsement.state.transition",
                        "from", previousStatus.name(), "to", "SUBMITTED_REALTIME").increment();

                endorsement = endorsementRepository.save(endorsement);
                log.info("Endorsement {} submitted in real-time to insurer", endorsementId);

                eventPublisher.publish(new EndorsementEvent.SubmittedRealtime(
                        endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                        endorsement.getInsurerId()));

                Timer.Sample sample = Timer.start(meterRegistry);
                SubmissionResult result = insurerPort.submitRealTime(
                        endorsementId, Map.of("endorsementId", endorsementId.toString()));
                sample.stop(meterRegistry.timer("endorsement.insurer.submission.duration",
                        "mode", "realtime", "result", result.success() ? "success" : "failure"));

                if (result.success()) {
                    endorsement.setInsurerReference(result.insurerReference());
                    stateMachine.transition(endorsement, EndorsementStatus.INSURER_PROCESSING);
                    endorsement = endorsementRepository.save(endorsement);

                    eventPublisher.publish(new EndorsementEvent.InsurerProcessing(
                            endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                            result.insurerReference()));

                    stateMachine.transition(endorsement, EndorsementStatus.CONFIRMED);
                    meterRegistry.counter("endorsement.state.transition",
                            "from", "INSURER_PROCESSING", "to", "CONFIRMED").increment();
                    endorsement = endorsementRepository.save(endorsement);
                    log.info("Endorsement {} confirmed by insurer with reference {}",
                            endorsementId, result.insurerReference());

                    eventPublisher.publish(new EndorsementEvent.Confirmed(
                            endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                            result.insurerReference()));

                    errorResolutionService.trackOutcome(endorsementId, EndorsementStatus.CONFIRMED);
                    confirmProvisionalCoverage(endorsementId);
                    notificationPort.notifyEndorsementConfirmed(
                            endorsement.getEmployerId(), endorsementId);
                } else {
                    // Attempt automated error resolution before rejecting
                    boolean autoResolved = false;
                    try {
                        autoResolved = errorResolutionService.attemptResolution(
                                endorsementId, result.errorMessage());
                    } catch (Exception e) {
                        log.warn("Error resolution failed for endorsement {}: {}", endorsementId, e.getMessage());
                    }

                    if (autoResolved) {
                        log.info("Endorsement {} error auto-resolved, will retry", endorsementId);
                        // Reset to allow resubmission
                        endorsement.setFailureReason(null);
                        endorsementRepository.save(endorsement);
                    } else {
                        endorsement.setFailureReason(result.errorMessage());
                        stateMachine.transition(endorsement, EndorsementStatus.REJECTED);
                        meterRegistry.counter("endorsement.state.transition",
                                "from", "SUBMITTED_REALTIME", "to", "REJECTED").increment();
                        endorsement = endorsementRepository.save(endorsement);
                        log.info("Endorsement {} rejected by insurer: {}", endorsementId, result.errorMessage());

                        eventPublisher.publish(new EndorsementEvent.Rejected(
                                endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                                result.errorMessage()));

                        errorResolutionService.trackOutcome(endorsementId, EndorsementStatus.REJECTED);
                        notificationPort.notifyEndorsementRejected(
                                endorsement.getEmployerId(), endorsementId, result.errorMessage());
                    }
                }
            } else {
                EndorsementStatus previousStatus = endorsement.getStatus();
                stateMachine.transition(endorsement, EndorsementStatus.QUEUED_FOR_BATCH);
                meterRegistry.counter("endorsement.state.transition",
                        "from", previousStatus.name(), "to", "QUEUED_FOR_BATCH").increment();
                endorsement = endorsementRepository.save(endorsement);
                log.info("Endorsement {} queued for batch processing", endorsementId);

                eventPublisher.publish(new EndorsementEvent.QueuedForBatch(
                        endorsement.getId(), Instant.now(), endorsement.getEmployerId()));
            }
        } finally {
            MDC.remove("endorsementId");
            MDC.remove("employerId");
        }
    }

    @Transactional
    public void handleConfirmation(UUID endorsementId, String insurerReference) {
        try {
            Endorsement endorsement = endorsementRepository.findById(endorsementId)
                    .orElseThrow(() -> new EndorsementNotFoundException(endorsementId));

            MDC.put("endorsementId", endorsementId.toString());
            MDC.put("employerId", endorsement.getEmployerId().toString());

            EndorsementStatus previousStatus = endorsement.getStatus();
            endorsement.setInsurerReference(insurerReference);
            stateMachine.transition(endorsement, EndorsementStatus.CONFIRMED);
            meterRegistry.counter("endorsement.state.transition",
                    "from", previousStatus.name(), "to", "CONFIRMED").increment();
            endorsement = endorsementRepository.save(endorsement);
            log.info("Endorsement {} confirmed with insurer reference {}", endorsementId, insurerReference);

            errorResolutionService.trackOutcome(endorsementId, EndorsementStatus.CONFIRMED);
            confirmProvisionalCoverage(endorsementId);
            notificationPort.notifyEndorsementConfirmed(endorsement.getEmployerId(), endorsementId);

            eventPublisher.publish(new EndorsementEvent.Confirmed(
                    endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                    insurerReference));
        } finally {
            MDC.remove("endorsementId");
            MDC.remove("employerId");
        }
    }

    @Transactional
    public void handleRejection(UUID endorsementId, String reason) {
        try {
            Endorsement endorsement = endorsementRepository.findById(endorsementId)
                    .orElseThrow(() -> new EndorsementNotFoundException(endorsementId));

            MDC.put("endorsementId", endorsementId.toString());
            MDC.put("employerId", endorsement.getEmployerId().toString());

            // Attempt automated error resolution before standard retry/fail flow
            boolean autoResolved = false;
            try {
                autoResolved = errorResolutionService.attemptResolution(endorsementId, reason);
            } catch (Exception e) {
                log.warn("Error resolution failed for endorsement {}: {}", endorsementId, e.getMessage());
            }

            if (autoResolved) {
                log.info("Endorsement {} error auto-resolved via handleRejection, will retry", endorsementId);
                endorsement.setFailureReason(null);
                endorsementRepository.save(endorsement);
                return;
            }

            endorsement.setFailureReason(reason);

            if (endorsement.canRetry()) {
                endorsement.incrementRetry();
                meterRegistry.counter("endorsement.state.transition",
                        "from", endorsement.getStatus().name(), "to", "RETRY_PENDING").increment();
                endorsement = endorsementRepository.save(endorsement);
                log.info("Endorsement {} scheduled for retry, attempt {}", endorsementId, endorsement.getRetryCount());

                eventPublisher.publish(new EndorsementEvent.RetryScheduled(
                        endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                        endorsement.getRetryCount()));
            } else {
                EndorsementStatus previousStatus = endorsement.getStatus();
                stateMachine.transition(endorsement, EndorsementStatus.FAILED_PERMANENT);
                meterRegistry.counter("endorsement.state.transition",
                        "from", previousStatus.name(), "to", "FAILED_PERMANENT").increment();
                endorsement = endorsementRepository.save(endorsement);
                log.info("Endorsement {} permanently failed: {}", endorsementId, reason);

                eventPublisher.publish(new EndorsementEvent.FailedPermanent(
                        endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                        reason));

                notificationPort.notifyEndorsementRejected(
                        endorsement.getEmployerId(), endorsementId, reason);

                errorResolutionService.trackOutcome(endorsementId, EndorsementStatus.FAILED_PERMANENT);

                // Expire provisional coverage and notify employer (Gap 1 fix)
                expireProvisionalCoverage(endorsement);
            }
        } finally {
            MDC.remove("endorsementId");
            MDC.remove("employerId");
        }
    }

    private void confirmProvisionalCoverage(UUID endorsementId) {
        Optional<ProvisionalCoverage> coverageOpt = provisionalCoverageRepository.findByEndorsementId(endorsementId);
        if (coverageOpt.isPresent()) {
            ProvisionalCoverage coverage = coverageOpt.get();
            coverage.confirm(Instant.now());
            provisionalCoverageRepository.save(coverage);
            log.info("Provisional coverage confirmed for endorsement {}", endorsementId);

            eventPublisher.publish(new EndorsementEvent.ProvisionalCoverageConfirmed(
                    endorsementId, Instant.now(), coverage.getEmployerId(), coverage.getEmployeeId()));
        }
    }

    private void expireProvisionalCoverage(Endorsement endorsement) {
        Optional<ProvisionalCoverage> coverageOpt = provisionalCoverageRepository
                .findByEndorsementId(endorsement.getId());
        if (coverageOpt.isPresent()) {
            ProvisionalCoverage coverage = coverageOpt.get();
            coverage.expire(Instant.now());
            provisionalCoverageRepository.save(coverage);
            log.warn("Provisional coverage expired for permanently failed endorsement {}", endorsement.getId());

            eventPublisher.publish(new EndorsementEvent.ProvisionalCoverageExpired(
                    endorsement.getId(), Instant.now(), endorsement.getEmployerId(),
                    endorsement.getEmployeeId()));

            notificationPort.notifyCoverageAtRisk(endorsement.getEmployerId(), endorsement.getId(),
                    "Endorsement permanently failed after all retries exhausted. Employee coverage requires manual intervention.");
        }
    }
}
