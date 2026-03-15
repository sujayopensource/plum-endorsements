package com.plum.endorsements.api.controller;

import com.plum.endorsements.api.dto.BatchProgressResponse;
import com.plum.endorsements.api.dto.CreateEndorsementRequest;
import com.plum.endorsements.api.dto.EndorsementResponse;
import com.plum.endorsements.application.handler.CreateEndorsementHandler;
import com.plum.endorsements.application.handler.EndorsementQueryHandler;
import com.plum.endorsements.application.handler.ProcessEndorsementHandler;
import com.plum.endorsements.application.service.AnomalyDetectionService;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementBatch;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.model.ProvisionalCoverage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/endorsements")
@RequiredArgsConstructor
public class EndorsementController {

    private final CreateEndorsementHandler createHandler;
    private final ProcessEndorsementHandler processHandler;
    private final EndorsementQueryHandler queryHandler;
    private final AnomalyDetectionService anomalyDetectionService;

    @PostMapping
    public ResponseEntity<EndorsementResponse> createEndorsement(
            @Valid @RequestBody CreateEndorsementRequest request) {

        log.info("Creating endorsement for employer={}, employee={}, type={}",
                request.employerId(), request.employeeId(), request.type());

        String idempotencyKey = request.idempotencyKey();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = request.employerId() + "-" + request.employeeId()
                    + "-" + request.type() + "-" + request.coverageStartDate();
        }

        Endorsement endorsement = Endorsement.builder()
                .employerId(request.employerId())
                .employeeId(request.employeeId())
                .insurerId(request.insurerId())
                .policyId(request.policyId())
                .type(EndorsementType.valueOf(request.type()))
                .coverageStartDate(request.coverageStartDate())
                .coverageEndDate(request.coverageEndDate())
                .employeeData(request.employeeData())
                .premiumAmount(request.premiumAmount())
                .idempotencyKey(idempotencyKey)
                .build();

        Endorsement result = createHandler.handle(endorsement);

        // Trigger anomaly detection asynchronously after handler transaction commits,
        // so the REQUIRES_NEW transaction can see the committed data without blocking the response
        final UUID endorsementId = result.getId();
        CompletableFuture.runAsync(() -> {
            try {
                anomalyDetectionService.analyzeEndorsement(endorsementId);
            } catch (Exception e) {
                log.warn("Anomaly detection failed for endorsement {} (non-blocking): {}",
                        endorsementId, e.getMessage());
            }
        });

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EndorsementResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EndorsementResponse> getEndorsement(@PathVariable UUID id) {
        Endorsement endorsement = queryHandler.findById(id);
        return ResponseEntity.ok(EndorsementResponse.from(endorsement));
    }

    @GetMapping
    public ResponseEntity<Page<EndorsementResponse>> listEndorsements(
            @RequestParam UUID employerId,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Endorsement> result;

        if (statuses != null && !statuses.isEmpty()) {
            List<EndorsementStatus> statusList = statuses.stream()
                    .map(EndorsementStatus::valueOf)
                    .toList();
            result = queryHandler.findByEmployerIdAndStatuses(employerId, statusList, pageable);
        } else {
            result = queryHandler.findByEmployerId(employerId, pageable);
        }

        Page<EndorsementResponse> response = result.map(EndorsementResponse::from);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employers/{employerId}/batches")
    public ResponseEntity<Page<BatchProgressResponse>> getBatchProgress(
            @PathVariable UUID employerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<EndorsementBatch> result = queryHandler.findBatchesByEmployerId(employerId, pageable);
        return ResponseEntity.ok(result.map(BatchProgressResponse::from));
    }

    @GetMapping("/employers/{employerId}/outstanding")
    public ResponseEntity<Page<EndorsementResponse>> getOutstandingItems(
            @PathVariable UUID employerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Endorsement> result = queryHandler.findOutstandingByEmployerId(employerId, pageable);
        return ResponseEntity.ok(result.map(EndorsementResponse::from));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitToInsurer(@PathVariable UUID id) {
        log.info("Submitting endorsement {} to insurer", id);
        processHandler.submitToInsurer(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmEndorsement(
            @PathVariable UUID id,
            @RequestParam String insurerReference) {

        log.info("Confirming endorsement {} with insurer reference {}", id, insurerReference);
        processHandler.handleConfirmation(id, insurerReference);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectEndorsement(
            @PathVariable UUID id,
            @RequestParam String reason) {

        log.info("Rejecting endorsement {} with reason: {}", id, reason);
        processHandler.handleRejection(id, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/coverage")
    public ResponseEntity<ProvisionalCoverage> getProvisionalCoverage(@PathVariable UUID id) {
        Optional<ProvisionalCoverage> coverage = queryHandler.findProvisionalCoverage(id);
        return coverage
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
