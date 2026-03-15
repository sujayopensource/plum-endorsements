package com.plum.endorsements.infrastructure.notification;

import com.plum.endorsements.domain.port.NotificationPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "endorsement.notifications.webhook.enabled", havingValue = "true")
public class WebhookNotificationAdapter implements NotificationPort {

    private final RestTemplate restTemplate;
    private final String webhookUrl;

    public WebhookNotificationAdapter(
            @Value("${endorsement.notifications.webhook.url}") String webhookUrl) {
        this.restTemplate = new RestTemplate();
        this.webhookUrl = webhookUrl;
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyEndorsementConfirmed(UUID employerId, UUID endorsementId) {
        post("endorsement.confirmed", Map.of(
                "employerId", employerId.toString(),
                "endorsementId", endorsementId.toString()));
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyEndorsementRejected(UUID employerId, UUID endorsementId, String reason) {
        post("endorsement.rejected", Map.of(
                "employerId", employerId.toString(),
                "endorsementId", endorsementId.toString(),
                "reason", reason));
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyInsufficientBalance(UUID employerId, BigDecimal required, BigDecimal available) {
        post("balance.insufficient", Map.of(
                "employerId", employerId.toString(),
                "required", required.toPlainString(),
                "available", available.toPlainString()));
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyBatchSlaBreached(UUID batchId, UUID insurerId) {
        post("batch.sla_breached", Map.of(
                "batchId", batchId.toString(),
                "insurerId", insurerId.toString()));
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyReconciliationDiscrepancy(UUID insurerId, UUID endorsementId, String details) {
        post("reconciliation.discrepancy", Map.of(
                "insurerId", insurerId.toString(),
                "endorsementId", endorsementId.toString(),
                "details", details));
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyReconciliationComplete(UUID insurerId, UUID runId, int matched, int discrepancies) {
        post("reconciliation.complete", Map.of(
                "insurerId", insurerId.toString(),
                "runId", runId.toString(),
                "matched", String.valueOf(matched),
                "discrepancies", String.valueOf(discrepancies)));
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyAnomalyDetected(UUID employerId, String anomalyType, double score, String explanation) {
        post("anomaly.detected", Map.of(
                "employerId", employerId.toString(),
                "anomalyType", anomalyType,
                "score", String.valueOf(score),
                "explanation", explanation));
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyForecastShortfall(UUID employerId, BigDecimal shortfall, int daysUntil) {
        post("forecast.shortfall", Map.of(
                "employerId", employerId.toString(),
                "shortfall", shortfall.toPlainString(),
                "daysUntil", String.valueOf(daysUntil)));
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyCoverageAtRisk(UUID employerId, UUID endorsementId, String reason) {
        post("coverage.at_risk", Map.of(
                "employerId", employerId.toString(),
                "endorsementId", endorsementId.toString(),
                "reason", reason));
    }

    @Override
    @CircuitBreaker(name = "webhookNotification", fallbackMethod = "fallback")
    @Retry(name = "webhookNotification")
    public void notifyCoverageExpired(UUID employerId, UUID employeeId, String reason) {
        post("coverage.expired", Map.of(
                "employerId", employerId.toString(),
                "employeeId", employeeId.toString(),
                "reason", reason));
    }

    private void post(String eventType, Map<String, String> payload) {
        Map<String, Object> envelope = Map.of(
                "eventType", eventType,
                "timestamp", Instant.now().toString(),
                "data", payload);
        restTemplate.postForEntity(webhookUrl, envelope, Void.class);
        log.debug("Webhook notification sent: type={}", eventType);
    }

    @SuppressWarnings("unused")
    private void fallback(Throwable t) {
        log.warn("Webhook notification failed (circuit breaker fallback): {}", t.getMessage());
    }
}
