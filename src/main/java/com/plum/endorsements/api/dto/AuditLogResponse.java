package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String action,
        String entityType,
        String entityId,
        String actor,
        String details,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getActor(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
