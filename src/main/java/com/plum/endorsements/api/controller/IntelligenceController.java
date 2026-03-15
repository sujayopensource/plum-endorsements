package com.plum.endorsements.api.controller;

import com.plum.endorsements.api.dto.*;
import com.plum.endorsements.application.service.*;
import com.plum.endorsements.domain.model.AnomalyStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {

    private final AnomalyDetectionService anomalyDetectionService;
    private final BalanceForecastService balanceForecastService;
    private final ErrorResolutionService errorResolutionService;
    private final ProcessMiningService processMiningService;
    private final EmployerHealthScoreService employerHealthScoreService;
    private final InsurerBenchmarkService insurerBenchmarkService;

    // --- Anomaly Detection ---

    @GetMapping("/anomalies")
    public ResponseEntity<List<AnomalyDetectionResponse>> listAnomalies(
            @RequestParam(required = false) UUID employerId,
            @RequestParam(required = false) String status) {

        List<AnomalyDetectionResponse> anomalies;
        if (employerId != null) {
            anomalies = anomalyDetectionService.findByEmployerId(employerId)
                    .stream().map(AnomalyDetectionResponse::from).toList();
        } else if (status != null) {
            anomalies = anomalyDetectionService.findByStatus(AnomalyStatus.valueOf(status))
                    .stream().map(AnomalyDetectionResponse::from).toList();
        } else {
            anomalies = anomalyDetectionService.findByStatus(AnomalyStatus.FLAGGED)
                    .stream().map(AnomalyDetectionResponse::from).toList();
        }

        return ResponseEntity.ok(anomalies);
    }

    @GetMapping("/anomalies/{id}")
    public ResponseEntity<AnomalyDetectionResponse> getAnomaly(@PathVariable UUID id) {
        return anomalyDetectionService.findByStatus(AnomalyStatus.FLAGGED).stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .map(a -> ResponseEntity.ok(AnomalyDetectionResponse.from(a)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/anomalies/{id}/review")
    public ResponseEntity<AnomalyDetectionResponse> reviewAnomaly(
            @PathVariable UUID id,
            @Valid @RequestBody AnomalyReviewRequest request) {

        var reviewed = anomalyDetectionService.reviewAnomaly(
                id, AnomalyStatus.valueOf(request.status()), request.notes());
        return ResponseEntity.ok(AnomalyDetectionResponse.from(reviewed));
    }

    // --- Balance Forecasting ---

    @GetMapping("/forecasts")
    public ResponseEntity<BalanceForecastResponse> getLatestForecast(
            @RequestParam UUID employerId,
            @RequestParam UUID insurerId) {

        return balanceForecastService.getLatestForecast(employerId, insurerId)
                .map(f -> ResponseEntity.ok(BalanceForecastResponse.from(f)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/forecasts/history")
    public ResponseEntity<List<ForecastHistoryResponse>> getForecastHistory(
            @RequestParam UUID employerId) {

        var history = balanceForecastService.getForecastHistory(employerId)
                .stream().map(ForecastHistoryResponse::from).toList();
        return ResponseEntity.ok(history);
    }

    @PostMapping("/forecasts/generate")
    public ResponseEntity<BalanceForecastResponse> generateForecast(
            @RequestParam UUID employerId,
            @RequestParam UUID insurerId) {

        var forecast = balanceForecastService.generateForecast(employerId, insurerId);
        return ResponseEntity.ok(BalanceForecastResponse.from(forecast));
    }

    // --- Error Resolution ---

    @GetMapping("/error-resolutions")
    public ResponseEntity<List<ErrorResolutionResponse>> listErrorResolutions(
            @RequestParam(required = false) UUID endorsementId) {

        var resolutions = errorResolutionService.findByEndorsementId(endorsementId)
                .stream().map(ErrorResolutionResponse::from).toList();
        return ResponseEntity.ok(resolutions);
    }

    @GetMapping("/error-resolutions/stats")
    public ResponseEntity<ErrorResolutionStatsResponse> getErrorResolutionStats() {
        return ResponseEntity.ok(errorResolutionService.getStats());
    }

    @PostMapping("/error-resolutions/resolve")
    public ResponseEntity<ErrorResolutionResponse> resolveError(
            @RequestParam UUID endorsementId,
            @RequestParam String errorMessage) {
        errorResolutionService.attemptResolution(endorsementId, errorMessage);
        var resolutions = errorResolutionService.findByEndorsementId(endorsementId);
        if (!resolutions.isEmpty()) {
            return ResponseEntity.ok(ErrorResolutionResponse.from(resolutions.get(resolutions.size() - 1)));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/error-resolutions/{id}/approve")
    public ResponseEntity<Void> approveResolution(@PathVariable UUID id) {
        errorResolutionService.approveResolution(id);
        return ResponseEntity.ok().build();
    }

    // --- Process Mining ---

    @GetMapping("/process-mining/metrics")
    public ResponseEntity<List<ProcessMiningMetricResponse>> getProcessMiningMetrics(
            @RequestParam(required = false) UUID insurerId) {

        var metrics = processMiningService.getMetrics(insurerId)
                .stream().map(ProcessMiningMetricResponse::from).toList();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/process-mining/insights")
    public ResponseEntity<List<ProcessMiningInsightResponse>> getInsights() {
        var insights = processMiningService.getLatestInsights();
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/process-mining/stp-rate")
    public ResponseEntity<StpRateResponse> getStpRate(
            @RequestParam(required = false) UUID insurerId) {
        return ResponseEntity.ok(processMiningService.getStpRate(insurerId));
    }

    @GetMapping("/process-mining/stp-rate/trend")
    public ResponseEntity<StpRateTrendResponse> getStpRateTrend(
            @RequestParam UUID insurerId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(processMiningService.getStpRateTrend(insurerId, days));
    }

    @PostMapping("/process-mining/analyze")
    public ResponseEntity<Void> triggerAnalysis() {
        processMiningService.generateInsights();
        return ResponseEntity.accepted().build();
    }

    // --- Employer Health Score ---

    @GetMapping("/employers/{employerId}/health-score")
    public ResponseEntity<HealthScoreResponse> getEmployerHealthScore(
            @PathVariable UUID employerId) {
        var score = employerHealthScoreService.calculateHealthScore(employerId);
        return ResponseEntity.ok(HealthScoreResponse.from(score));
    }

    // --- Cross-Insurer Benchmarking ---

    @GetMapping("/benchmarks")
    public ResponseEntity<List<InsurerBenchmarkResponse>> getInsurerBenchmarks() {
        var benchmarks = insurerBenchmarkService.generateBenchmarks().stream()
                .map(InsurerBenchmarkResponse::from).toList();
        return ResponseEntity.ok(benchmarks);
    }
}
