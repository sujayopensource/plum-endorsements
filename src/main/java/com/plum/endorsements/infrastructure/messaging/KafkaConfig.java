package com.plum.endorsements.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic endorsementEventsTopic() {
        return TopicBuilder.name("endorsement-events")
                .partitions(32)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic endorsementCommandsTopic() {
        return TopicBuilder.name("endorsement-commands")
                .partitions(32)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic endorsementNotificationsTopic() {
        return TopicBuilder.name("endorsement-notifications")
                .partitions(8)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic endorsementReconciliationTopic() {
        return TopicBuilder.name("endorsement-reconciliation")
                .partitions(16)
                .replicas(1)
                .build();
    }
}
