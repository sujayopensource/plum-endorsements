package com.plum.endorsements.infrastructure.messaging;

import com.plum.endorsements.domain.model.EndorsementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(EndorsementEvent event) {
        UUID employerId = event.employerId();
        if (employerId == null) {
            log.debug("Skipping WebSocket broadcast for event {} — no employerId",
                    event.eventType());
            return;
        }

        Map<String, Object> payload = Map.of(
                "type", event.eventType(),
                "endorsementId", event.endorsementId().toString(),
                "employerId", employerId.toString(),
                "timestamp", event.occurredAt().toString()
        );

        String destination = "/topic/employer/" + employerId;
        messagingTemplate.convertAndSend(destination, payload);

        log.debug("Broadcast event [type={}, endorsementId={}] to {}",
                event.eventType(), event.endorsementId(), destination);
    }
}
