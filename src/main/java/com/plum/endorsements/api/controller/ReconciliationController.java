package com.plum.endorsements.api.controller;

import com.plum.endorsements.api.dto.ReconciliationItemResponse;
import com.plum.endorsements.api.dto.ReconciliationRunResponse;
import com.plum.endorsements.application.handler.EndorsementQueryHandler;
import com.plum.endorsements.application.service.ReconciliationEngine;
import com.plum.endorsements.domain.model.ReconciliationRun;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final EndorsementQueryHandler queryHandler;
    private final ReconciliationEngine reconciliationEngine;

    @GetMapping("/runs")
    public ResponseEntity<List<ReconciliationRunResponse>> getRuns(
            @RequestParam UUID insurerId) {
        List<ReconciliationRunResponse> runs = queryHandler.findReconciliationRuns(insurerId).stream()
                .map(ReconciliationRunResponse::from)
                .toList();
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/runs/{runId}/items")
    public ResponseEntity<List<ReconciliationItemResponse>> getRunItems(@PathVariable UUID runId) {
        List<ReconciliationItemResponse> items = queryHandler.findReconciliationItems(runId).stream()
                .map(ReconciliationItemResponse::from)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/trigger")
    public ResponseEntity<ReconciliationRunResponse> triggerReconciliation(
            @RequestParam UUID insurerId) {
        ReconciliationRun run = reconciliationEngine.reconcileInsurer(insurerId);
        return ResponseEntity.ok(ReconciliationRunResponse.from(run));
    }
}
