package com.plum.endorsements.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.port.EventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private static final String TOPIC = "endorsement-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    @Nullable
    private final WebSocketEventBroadcaster webSocketBroadcaster;

    @Override
    public void publish(EndorsementEvent event) {
        try {
            MDC.put("kafkaEventType", event.eventType());
            String key = event.employerId() != null
                    ? event.employerId().toString()
                    : event.endorsementId().toString();
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(TOPIC, key, payload);

            meterRegistry.counter("endorsement.kafka.publish",
                    "result", "success", "eventType", event.eventType()).increment();

            // Broadcast to WebSocket subscribers for real-time UI updates
            if (webSocketBroadcaster != null) {
                try {
                    webSocketBroadcaster.broadcast(event);
                } catch (Exception wsEx) {
                    log.warn("WebSocket broadcast failed for event [type={}]: {}",
                            event.eventType(), wsEx.getMessage());
                }
            }

            log.debug("Published event [type={}, endorsementId={}] to topic {}",
                    event.eventType(), event.endorsementId(), TOPIC);
        } catch (Exception e) {
            meterRegistry.counter("endorsement.kafka.publish",
                    "result", "failure", "eventType", event.eventType()).increment();
            log.error("Failed to publish event [type={}, endorsementId={}]: {}",
                    event.eventType(), event.endorsementId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish endorsement event", e);
        } finally {
            MDC.remove("kafkaEventType");
        }
    }
}
