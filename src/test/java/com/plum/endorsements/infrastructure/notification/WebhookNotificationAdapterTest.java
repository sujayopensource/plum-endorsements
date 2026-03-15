package com.plum.endorsements.infrastructure.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookNotificationAdapter")
class WebhookNotificationAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private WebhookNotificationAdapter adapter;

    private static final String WEBHOOK_URL = "http://localhost:9090/webhooks";

    @BeforeEach
    void setUp() {
        adapter = new WebhookNotificationAdapter(WEBHOOK_URL);
        ReflectionTestUtils.setField(adapter, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("notifyEndorsementConfirmed posts to webhook URL")
    void notifyEndorsementConfirmed_PostsToWebhookUrl() {
        UUID employerId = UUID.randomUUID();
        UUID endorsementId = UUID.randomUUID();

        adapter.notifyEndorsementConfirmed(employerId, endorsementId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(restTemplate).postForEntity(eq(WEBHOOK_URL), captor.capture(), eq(Void.class));

        Map<String, Object> envelope = captor.getValue();
        assertThat(envelope.get("eventType")).isEqualTo("endorsement.confirmed");
        assertThat(envelope).containsKey("timestamp");

        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) envelope.get("data");
        assertThat(data.get("employerId")).isEqualTo(employerId.toString());
        assertThat(data.get("endorsementId")).isEqualTo(endorsementId.toString());
    }

    @Test
    @DisplayName("notifyAnomalyDetected posts anomaly details to webhook")
    void notifyAnomalyDetected_PostsAnomalyDetails() {
        UUID employerId = UUID.randomUUID();

        adapter.notifyAnomalyDetected(employerId, "VOLUME_SPIKE", 0.85, "Spike detected");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(restTemplate).postForEntity(eq(WEBHOOK_URL), captor.capture(), eq(Void.class));

        Map<String, Object> envelope = captor.getValue();
        assertThat(envelope.get("eventType")).isEqualTo("anomaly.detected");

        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) envelope.get("data");
        assertThat(data.get("anomalyType")).isEqualTo("VOLUME_SPIKE");
        assertThat(data.get("score")).isEqualTo("0.85");
    }

    @Test
    @DisplayName("notifyCoverageAtRisk posts coverage risk notification")
    void notifyCoverageAtRisk_PostsCoverageRiskNotification() {
        UUID employerId = UUID.randomUUID();
        UUID endorsementId = UUID.randomUUID();

        adapter.notifyCoverageAtRisk(employerId, endorsementId, "Coverage expiring in 2 days");

        verify(restTemplate).postForEntity(eq(WEBHOOK_URL), any(), eq(Void.class));
    }

    @Test
    @DisplayName("webhook failure throws exception for circuit breaker to catch")
    void post_WebhookFailure_ThrowsException() {
        when(restTemplate.postForEntity(eq(WEBHOOK_URL), any(), eq(Void.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThatThrownBy(() ->
                adapter.notifyEndorsementConfirmed(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(RestClientException.class);
    }
}
