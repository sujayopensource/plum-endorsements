package com.plum.endorsements.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.EndorsementType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventPublisher")
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private WebSocketEventBroadcaster webSocketBroadcaster;

    private KafkaEventPublisher publisher;

    private UUID employerId;
    private UUID endorsementId;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        publisher = new KafkaEventPublisher(kafkaTemplate, objectMapper, meterRegistry, webSocketBroadcaster);
        employerId = UUID.randomUUID();
        endorsementId = UUID.randomUUID();
    }

    @Test
    @DisplayName("uses employerId as partition key for Created events")
    void publish_CreatedEvent_UsesEmployerIdAsKey() {
        EndorsementEvent event = new EndorsementEvent.Created(
                endorsementId, Instant.now(), employerId,
                UUID.randomUUID(), EndorsementType.ADD);

        publisher.publish(event);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("endorsement-events"), keyCaptor.capture(), any());
        assertThat(keyCaptor.getValue()).isEqualTo(employerId.toString());
    }

    @Test
    @DisplayName("uses employerId as partition key for Confirmed events")
    void publish_ConfirmedEvent_UsesEmployerIdAsKey() {
        EndorsementEvent event = new EndorsementEvent.Confirmed(
                endorsementId, Instant.now(), employerId, "REF-123");

        publisher.publish(event);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("endorsement-events"), keyCaptor.capture(), any());
        assertThat(keyCaptor.getValue()).isEqualTo(employerId.toString());
    }

    @Test
    @DisplayName("uses employerId as partition key for Rejected events")
    void publish_RejectedEvent_UsesEmployerIdAsKey() {
        EndorsementEvent event = new EndorsementEvent.Rejected(
                endorsementId, Instant.now(), employerId, "Some reason");

        publisher.publish(event);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("endorsement-events"), keyCaptor.capture(), any());
        assertThat(keyCaptor.getValue()).isEqualTo(employerId.toString());
    }

    @Test
    @DisplayName("uses employerId as partition key for BalanceForecastAlert events")
    void publish_BalanceForecastAlert_UsesEmployerIdAsKey() {
        EndorsementEvent event = new EndorsementEvent.BalanceForecastAlert(
                endorsementId, Instant.now(), employerId,
                new java.math.BigDecimal("1500.00"));

        publisher.publish(event);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("endorsement-events"), keyCaptor.capture(), any());
        assertThat(keyCaptor.getValue()).isEqualTo(employerId.toString());
    }
}
