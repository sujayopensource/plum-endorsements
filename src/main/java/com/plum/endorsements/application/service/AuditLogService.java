package com.plum.endorsements.application.service;

import com.plum.endorsements.domain.model.AuditLog;
import com.plum.endorsements.infrastructure.persistence.entity.AuditLogEntity;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SpringDataAuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, String entityId, String actor, String details) {
        AuditLogEntity entity = AuditLogEntity.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actor(actor != null ? actor : "SYSTEM")
                .details(details)
                .createdAt(Instant.now())
                .build();
        repository.save(entity);
        log.debug("Audit log: action={}, entityType={}, entityId={}", action, entityType, entityId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByEntity(String entityType, String entityId, Pageable pageable) {
        return repository.findByEntityTypeAndEntityId(entityType, entityId, pageable).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByAction(String action, Pageable pageable) {
        return repository.findByAction(action, pageable).map(this::toDomain);
    }

    private AuditLog toDomain(AuditLogEntity entity) {
        return AuditLog.builder()
                .id(entity.getId())
                .action(entity.getAction())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .actor(entity.getActor())
                .details(entity.getDetails())
                .ipAddress(entity.getIpAddress())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
