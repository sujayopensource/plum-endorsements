package com.plum.endorsements.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private UUID id;
    private String action;
    private String entityType;
    private String entityId;
    private String actor;
    private String details;
    private String ipAddress;
    private Instant createdAt;
}
