package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insurer_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurerConfigurationEntity {

    @Id
    @Column(name = "insurer_id")
    private UUID insurerId;

    @Column(name = "insurer_name", nullable = false, length = 100)
    private String insurerName;

    @Column(name = "insurer_code", nullable = false, unique = true, length = 20)
    private String insurerCode;

    @Column(name = "adapter_type", nullable = false, length = 30)
    private String adapterType;

    @Column(name = "supports_real_time", nullable = false)
    @Builder.Default
    private boolean supportsRealTime = false;

    @Column(name = "supports_batch", nullable = false)
    @Builder.Default
    private boolean supportsBatch = false;

    @Column(name = "max_batch_size", nullable = false)
    @Builder.Default
    private int maxBatchSize = 100;

    @Column(name = "batch_sla_hours", nullable = false)
    @Builder.Default
    private long batchSlaHours = 24;

    @Column(name = "rate_limit_per_min", nullable = false)
    @Builder.Default
    private int rateLimitPerMinute = 60;

    @Column(name = "api_base_url", length = 500)
    private String apiBaseUrl;

    @Column(name = "auth_type", length = 30)
    private String authType;

    @Column(name = "auth_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String authConfig;

    @Column(name = "data_format", nullable = false, length = 10)
    @Builder.Default
    private String dataFormat = "JSON";

    @Column(name = "retry_max_attempts", nullable = false)
    @Builder.Default
    private int retryMaxAttempts = 3;

    @Column(name = "retry_wait_ms", nullable = false)
    @Builder.Default
    private long retryWaitMs = 2000;

    @Column(name = "circuit_breaker_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String circuitBreakerConfig;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
