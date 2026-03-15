package com.plum.endorsements.api.controller;

import com.plum.endorsements.api.dto.CreateInsurerConfigurationRequest;
import com.plum.endorsements.api.dto.InsurerConfigurationResponse;
import com.plum.endorsements.api.dto.UpdateInsurerConfigurationRequest;
import com.plum.endorsements.application.handler.EndorsementQueryHandler;
import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.port.InsurerConfigurationRepository;
import com.plum.endorsements.domain.service.InsurerRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insurers")
@RequiredArgsConstructor
public class InsurerConfigurationController {

    private final EndorsementQueryHandler queryHandler;
    private final InsurerRegistry insurerRegistry;

    @GetMapping
    public ResponseEntity<List<InsurerConfigurationResponse>> listInsurers() {
        List<InsurerConfigurationResponse> insurers = queryHandler.findAllActiveInsurers().stream()
                .map(InsurerConfigurationResponse::from)
                .toList();
        return ResponseEntity.ok(insurers);
    }

    @GetMapping("/{insurerId}")
    public ResponseEntity<InsurerConfigurationResponse> getInsurer(@PathVariable UUID insurerId) {
        InsurerConfiguration config = queryHandler.findInsurerById(insurerId);
        return ResponseEntity.ok(InsurerConfigurationResponse.from(config));
    }

    @GetMapping("/{insurerId}/capabilities")
    public ResponseEntity<InsurerConfigurationResponse.CapabilitiesResponse> getCapabilities(
            @PathVariable UUID insurerId) {
        InsurerConfiguration config = queryHandler.findInsurerById(insurerId);
        return ResponseEntity.ok(InsurerConfigurationResponse.CapabilitiesResponse.from(
                config.toCapabilities()));
    }

    @PostMapping
    public ResponseEntity<InsurerConfigurationResponse> createInsurer(
            @Valid @RequestBody CreateInsurerConfigurationRequest request) {
        Instant now = Instant.now();
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(UUID.randomUUID())
                .insurerName(request.insurerName())
                .insurerCode(request.insurerCode())
                .adapterType(request.adapterType())
                .supportsRealTime(request.supportsRealTime())
                .supportsBatch(request.supportsBatch())
                .maxBatchSize(request.maxBatchSize())
                .batchSlaHours(request.batchSlaHours())
                .rateLimitPerMinute(request.rateLimitPerMinute())
                .apiBaseUrl(request.apiBaseUrl())
                .authType(request.authType())
                .dataFormat(request.dataFormat() != null ? request.dataFormat() : "JSON")
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        InsurerConfiguration saved = insurerRegistry.createConfiguration(config);
        return ResponseEntity
                .created(URI.create("/api/v1/insurers/" + saved.getInsurerId()))
                .body(InsurerConfigurationResponse.from(saved));
    }

    @PutMapping("/{insurerId}")
    public ResponseEntity<InsurerConfigurationResponse> updateInsurer(
            @PathVariable UUID insurerId,
            @Valid @RequestBody UpdateInsurerConfigurationRequest request) {
        InsurerConfiguration existing = insurerRegistry.getConfiguration(insurerId);

        if (request.insurerName() != null) existing.setInsurerName(request.insurerName());
        if (request.supportsRealTime() != null) existing.setSupportsRealTime(request.supportsRealTime());
        if (request.supportsBatch() != null) existing.setSupportsBatch(request.supportsBatch());
        if (request.maxBatchSize() != null) existing.setMaxBatchSize(request.maxBatchSize());
        if (request.batchSlaHours() != null) existing.setBatchSlaHours(request.batchSlaHours());
        if (request.rateLimitPerMinute() != null) existing.setRateLimitPerMinute(request.rateLimitPerMinute());
        if (request.apiBaseUrl() != null) existing.setApiBaseUrl(request.apiBaseUrl());
        if (request.authType() != null) existing.setAuthType(request.authType());
        if (request.dataFormat() != null) existing.setDataFormat(request.dataFormat());
        if (request.active() != null) existing.setActive(request.active());
        existing.setUpdatedAt(Instant.now());

        InsurerConfiguration updated = insurerRegistry.updateConfiguration(existing);
        return ResponseEntity.ok(InsurerConfigurationResponse.from(updated));
    }
}
