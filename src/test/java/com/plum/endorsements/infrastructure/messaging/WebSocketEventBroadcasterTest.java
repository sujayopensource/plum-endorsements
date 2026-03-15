package com.plum.endorsements.infrastructure.messaging;

import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.EndorsementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketEventBroadcaster")
class WebSocketEventBroadcasterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketEventBroadcaster broadcaster;

    private UUID employerId;
    private UUID endorsementId;

    @BeforeEach
    void setUp() {
        broadcaster = new WebSocketEventBroadcaster(messagingTemplate);
        employerId = UUID.randomUUID();
        endorsementId = UUID.randomUUID();
    }

    @Test
    @DisplayName("broadcasts Created event to employer topic")
    void broadcast_createdEvent_sendsToEmployerTopic() {
        EndorsementEvent event = new EndorsementEvent.Created(
                endorsementId, Instant.now(), employerId,
                UUID.randomUUID(), EndorsementType.ADD);

        broadcaster.broadcast(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/employer/" + employerId), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("type")).isEqualTo("ENDORSEMENT_CREATED");
        assertThat(payload.get("endorsementId")).isEqualTo(endorsementId.toString());
        assertThat(payload.get("employerId")).isEqualTo(employerId.toString());
        assertThat(payload.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("broadcasts Confirmed event to correct employer topic")
    void broadcast_confirmedEvent_sendsToCorrectTopic() {
        EndorsementEvent event = new EndorsementEvent.Confirmed(
                endorsementId, Instant.now(), employerId, "REF-123");

        broadcaster.broadcast(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/employer/" + employerId), any(Map.class));
    }

    @Test
    @DisplayName("skips broadcast when employerId is null")
    void broadcast_nullEmployerId_skipsGracefully() {
        EndorsementEvent event = new EndorsementEvent.BatchOptimized(
                endorsementId, Instant.now(), null,
                UUID.randomUUID(), "PRIORITY", BigDecimal.TEN);

        broadcaster.broadcast(event);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
