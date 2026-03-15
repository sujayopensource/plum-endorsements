package com.plum.endorsements.application.service;

import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final AnomalyDetectionPort anomalyDetector;
    private final AnomalyDetectionRepository anomalyRepository;
    private final EndorsementRepository endorsementRepository;
    private final EventPublisher eventPublisher;
    private final NotificationPort notificationPort;
    private final MeterRegistry meterRegistry;

    @Value("${endorsement.intelligence.anomaly-detection.min-anomaly-score:0.7}")
    private double minAnomalyScore;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void analyzeEndorsement(UUID endorsementId) {
        endorsementRepository.findById(endorsementId).ifPresent(endorsement -> {
            List<Endorsement> recentHistory = new ArrayList<>(endorsementRepository.findByStatus(EndorsementStatus.CREATED));
            recentHistory.addAll(endorsementRepository.findByStatus(EndorsementStatus.VALIDATED));
            recentHistory.addAll(endorsementRepository.findByStatus(EndorsementStatus.PROVISIONALLY_COVERED));
            recentHistory.addAll(endorsementRepository.findByStatus(EndorsementStatus.QUEUED_FOR_BATCH));
            recentHistory.addAll(endorsementRepository.findByStatus(EndorsementStatus.BATCH_SUBMITTED));
            recentHistory.addAll(endorsementRepository.findByStatus(EndorsementStatus.CONFIRMED));

            AnomalyDetectionPort.AnomalyResult result = anomalyDetector.analyzeEndorsement(endorsement, recentHistory);

            meterRegistry.summary("endorsement.anomaly.score", "anomalyType", result.anomalyType())
                    .record(result.score());

            if (result.isFlagged(minAnomalyScore)) {
                AnomalyDetection anomaly = AnomalyDetection.builder()
                        .endorsementId(endorsementId)
                        .employerId(endorsement.getEmployerId())
                        .anomalyType(AnomalyType.valueOf(result.anomalyType()))
                        .score(result.score())
                        .explanation(result.explanation())
                        .flaggedAt(Instant.now())
                        .status(AnomalyStatus.FLAGGED)
                        .build();

                anomaly = anomalyRepository.save(anomaly);

                meterRegistry.counter("endorsement.anomaly.detected",
                        "anomalyType", result.anomalyType(),
                        "employerId", endorsement.getEmployerId().toString()).increment();

                eventPublisher.publish(new EndorsementEvent.AnomalyDetected(
                        endorsementId, Instant.now(), endorsement.getEmployerId(),
                        result.anomalyType(), result.score(), result.explanation()));

                notificationPort.notifyAnomalyDetected(endorsement.getEmployerId(),
                        result.anomalyType(), result.score(), result.explanation());

                log.warn("Anomaly detected for endorsement {}: type={}, score={}, explanation={}",
                        endorsementId, result.anomalyType(), result.score(), result.explanation());
            }
        });
    }

    @Transactional
    public void runBatchAnalysis() {
        Instant since = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<Endorsement> recent = new ArrayList<>(endorsementRepository.findByStatus(EndorsementStatus.CREATED));
        recent.addAll(endorsementRepository.findByStatus(EndorsementStatus.VALIDATED));
        recent.addAll(endorsementRepository.findByStatus(EndorsementStatus.PROVISIONALLY_COVERED));

        List<Endorsement> allHistory = new ArrayList<>(endorsementRepository.findByStatus(EndorsementStatus.CONFIRMED));
        allHistory.addAll(recent);

        int analyzed = 0;
        for (Endorsement endorsement : recent) {
            if (endorsement.getCreatedAt() != null && endorsement.getCreatedAt().isAfter(since)) {
                analyzeEndorsement(endorsement.getId());
                analyzed++;
            }
        }

        log.info("Batch anomaly analysis completed: {} endorsements analyzed", analyzed);
    }

    @Transactional
    public AnomalyDetection reviewAnomaly(UUID anomalyId, AnomalyStatus newStatus, String notes) {
        AnomalyDetection anomaly = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + anomalyId));

        anomaly.review(newStatus, notes);
        AnomalyDetection saved = anomalyRepository.save(anomaly);

        // Feedback loop: record review outcome and compute false positive rate per anomaly type
        if (newStatus.isTerminal()) {
            String outcome = newStatus == AnomalyStatus.DISMISSED ? "false_positive" : "true_positive";
            meterRegistry.counter("endorsement.anomaly.review",
                    "anomalyType", anomaly.getAnomalyType().name(),
                    "outcome", outcome).increment();

            long totalReviewed = anomalyRepository.countByAnomalyTypeAndStatus(
                    anomaly.getAnomalyType(), AnomalyStatus.DISMISSED)
                    + anomalyRepository.countByAnomalyTypeAndStatus(
                    anomaly.getAnomalyType(), AnomalyStatus.CONFIRMED_FRAUD);
            long dismissed = anomalyRepository.countByAnomalyTypeAndStatus(
                    anomaly.getAnomalyType(), AnomalyStatus.DISMISSED);

            if (totalReviewed > 0) {
                double falsePositiveRate = (double) dismissed / totalReviewed;
                meterRegistry.gauge("endorsement.anomaly.false_positive_rate."
                        + anomaly.getAnomalyType().name().toLowerCase(), falsePositiveRate);
                log.info("Anomaly feedback: type={}, outcome={}, falsePositiveRate={:.2f} ({}/{})",
                        anomaly.getAnomalyType(), outcome, falsePositiveRate, dismissed, totalReviewed);
            }
        }

        return saved;
    }

    public List<AnomalyDetection> findByEmployerId(UUID employerId) {
        return anomalyRepository.findByEmployerId(employerId);
    }

    public List<AnomalyDetection> findByStatus(AnomalyStatus status) {
        return anomalyRepository.findByStatus(status);
    }
}
