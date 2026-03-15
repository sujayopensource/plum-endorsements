package com.plum.endorsements.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.plum.endorsements.domain.port.InsurerPort;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsurerConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;
    private UUID insurerId;
    private String insurerName;
    private String insurerCode;
    private String adapterType;
    private boolean supportsRealTime;
    private boolean supportsBatch;
    private int maxBatchSize;
    private long batchSlaHours;
    private int rateLimitPerMinute;
    private String apiBaseUrl;
    private String authType;
    private JsonNode authConfig;
    private String dataFormat;
    private int retryMaxAttempts;
    private long retryWaitMs;
    private JsonNode circuitBreakerConfig;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public InsurerPort.InsurerCapabilities toCapabilities() {
        return new InsurerPort.InsurerCapabilities(
                supportsRealTime, supportsBatch, maxBatchSize, batchSlaHours, rateLimitPerMinute);
    }
}
