package com.plum.endorsements.api.controller;

import com.plum.endorsements.api.dto.AuditLogResponse;
import com.plum.endorsements.application.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String action,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AuditLogResponse> logs;
        if (entityType != null && entityId != null) {
            logs = auditLogService.findByEntity(entityType, entityId, pageable)
                    .map(AuditLogResponse::from);
        } else if (action != null) {
            logs = auditLogService.findByAction(action, pageable)
                    .map(AuditLogResponse::from);
        } else {
            logs = auditLogService.findAll(pageable)
                    .map(AuditLogResponse::from);
        }
        return ResponseEntity.ok(logs);
    }
}
