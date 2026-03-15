package com.plum.endorsements.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plum.endorsements.api.dto.*;
import com.plum.endorsements.application.service.*;
import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.infrastructure.config.SecurityConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IntelligenceController.class)
@Import(SecurityConfig.class)
@DisplayName("IntelligenceController")
class IntelligenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnomalyDetectionService anomalyDetectionService;

    @MockitoBean
    private BalanceForecastService balanceForecastService;

    @MockitoBean
    private ErrorResolutionService errorResolutionService;

    @MockitoBean
    private ProcessMiningService processMiningService;

    @MockitoBean
    private EmployerHealthScoreService employerHealthScoreService;

    @MockitoBean
    private InsurerBenchmarkService insurerBenchmarkService;

    @MockitoBean(answers = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;

    @MockitoBean
    private Tracer tracer;

    // --- Helper builders ---

    private AnomalyDetection buildAnomaly(UUID id, UUID endorsementId, UUID employerId,
                                           AnomalyType type, double score, AnomalyStatus status) {
        return AnomalyDetection.builder()
                .id(id)
                .endorsementId(endorsementId)
                .employerId(employerId)
                .anomalyType(type)
                .score(score)
                .explanation("Test anomaly explanation")
                .flaggedAt(Instant.now())
                .status(status)
                .build();
    }

    private BalanceForecastRecord buildForecastRecord(UUID employerId, UUID insurerId) {
        return BalanceForecastRecord.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .insurerId(insurerId)
                .forecastDate(LocalDate.now().plusDays(30))
                .forecastedAmount(new BigDecimal("50000.00"))
                .narrative("Forecast narrative for testing")
                .createdAt(Instant.now())
                .build();
    }

    private ErrorResolution buildErrorResolution(UUID id, UUID endorsementId) {
        return ErrorResolution.builder()
                .id(id)
                .endorsementId(endorsementId)
                .errorType("DATE_FORMAT")
                .originalValue("07-03-1990")
                .correctedValue("1990-03-07")
                .resolution("Converted to ISO format")
                .confidence(0.98)
                .autoApplied(true)
                .createdAt(Instant.now())
                .build();
    }

    private ProcessMiningMetric buildMetric(UUID insurerId) {
        return ProcessMiningMetric.builder()
                .id(UUID.randomUUID())
                .insurerId(insurerId)
                .fromStatus("CREATED")
                .toStatus("VALIDATED")
                .avgDurationMs(5000L)
                .p95DurationMs(10000L)
                .p99DurationMs(15000L)
                .sampleCount(50)
                .happyPathPct(new BigDecimal("85.00"))
                .calculatedAt(Instant.now())
                .build();
    }

    // ==================== Anomaly Detection Endpoints ====================

    @Nested
    @DisplayName("Anomaly Detection Endpoints")
    class AnomalyDetectionEndpoints {

        @Test
        @DisplayName("GET /anomalies returns 200 with list of flagged anomalies (default)")
        void listAnomalies_DefaultFlagged_Returns200WithList() throws Exception {
            UUID anomalyId = UUID.randomUUID();
            UUID endorsementId = UUID.randomUUID();
            UUID employerId = UUID.randomUUID();

            AnomalyDetection anomaly = buildAnomaly(anomalyId, endorsementId, employerId,
                    AnomalyType.VOLUME_SPIKE, 0.85, AnomalyStatus.FLAGGED);

            when(anomalyDetectionService.findByStatus(AnomalyStatus.FLAGGED))
                    .thenReturn(List.of(anomaly));

            mockMvc.perform(get("/api/v1/intelligence/anomalies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(anomalyId.toString()))
                    .andExpect(jsonPath("$[0].endorsementId").value(endorsementId.toString()))
                    .andExpect(jsonPath("$[0].employerId").value(employerId.toString()))
                    .andExpect(jsonPath("$[0].anomalyType").value("VOLUME_SPIKE"))
                    .andExpect(jsonPath("$[0].score").value(0.85))
                    .andExpect(jsonPath("$[0].status").value("FLAGGED"));

            verify(anomalyDetectionService).findByStatus(AnomalyStatus.FLAGGED);
        }

        @Test
        @DisplayName("GET /anomalies with status filter returns anomalies with given status")
        void listAnomalies_WithStatusFilter_ReturnsFilteredList() throws Exception {
            UUID anomalyId = UUID.randomUUID();
            AnomalyDetection anomaly = buildAnomaly(anomalyId, UUID.randomUUID(), UUID.randomUUID(),
                    AnomalyType.SUSPICIOUS_TIMING, 0.75, AnomalyStatus.UNDER_REVIEW);

            when(anomalyDetectionService.findByStatus(AnomalyStatus.UNDER_REVIEW))
                    .thenReturn(List.of(anomaly));

            mockMvc.perform(get("/api/v1/intelligence/anomalies")
                            .param("status", "UNDER_REVIEW"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("UNDER_REVIEW"))
                    .andExpect(jsonPath("$[0].anomalyType").value("SUSPICIOUS_TIMING"));

            verify(anomalyDetectionService).findByStatus(AnomalyStatus.UNDER_REVIEW);
        }

        @Test
        @DisplayName("GET /anomalies with employerId filter returns anomalies for that employer")
        void listAnomalies_WithEmployerIdFilter_ReturnsFilteredList() throws Exception {
            UUID employerId = UUID.randomUUID();
            AnomalyDetection anomaly = buildAnomaly(UUID.randomUUID(), UUID.randomUUID(), employerId,
                    AnomalyType.ADD_DELETE_CYCLING, 0.90, AnomalyStatus.FLAGGED);

            when(anomalyDetectionService.findByEmployerId(employerId))
                    .thenReturn(List.of(anomaly));

            mockMvc.perform(get("/api/v1/intelligence/anomalies")
                            .param("employerId", employerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].employerId").value(employerId.toString()))
                    .andExpect(jsonPath("$[0].anomalyType").value("ADD_DELETE_CYCLING"));

            verify(anomalyDetectionService).findByEmployerId(employerId);
        }

        @Test
        @DisplayName("GET /anomalies/{id} returns 200 when anomaly found")
        void getAnomaly_Found_Returns200() throws Exception {
            UUID anomalyId = UUID.randomUUID();
            UUID endorsementId = UUID.randomUUID();
            UUID employerId = UUID.randomUUID();

            AnomalyDetection anomaly = buildAnomaly(anomalyId, endorsementId, employerId,
                    AnomalyType.UNUSUAL_PREMIUM, 0.72, AnomalyStatus.FLAGGED);

            when(anomalyDetectionService.findByStatus(AnomalyStatus.FLAGGED))
                    .thenReturn(List.of(anomaly));

            mockMvc.perform(get("/api/v1/intelligence/anomalies/{id}", anomalyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(anomalyId.toString()))
                    .andExpect(jsonPath("$.anomalyType").value("UNUSUAL_PREMIUM"))
                    .andExpect(jsonPath("$.score").value(0.72));
        }

        @Test
        @DisplayName("GET /anomalies/{id} returns 404 when anomaly not found")
        void getAnomaly_NotFound_Returns404() throws Exception {
            UUID anomalyId = UUID.randomUUID();

            when(anomalyDetectionService.findByStatus(AnomalyStatus.FLAGGED))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/intelligence/anomalies/{id}", anomalyId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT /anomalies/{id}/review returns 200 with updated anomaly")
        void reviewAnomaly_Valid_Returns200() throws Exception {
            UUID anomalyId = UUID.randomUUID();
            UUID endorsementId = UUID.randomUUID();
            UUID employerId = UUID.randomUUID();

            AnomalyDetection reviewed = buildAnomaly(anomalyId, endorsementId, employerId,
                    AnomalyType.VOLUME_SPIKE, 0.85, AnomalyStatus.UNDER_REVIEW);
            reviewed.setReviewerNotes("Investigating the spike");
            reviewed.setReviewedAt(Instant.now());

            when(anomalyDetectionService.reviewAnomaly(eq(anomalyId), eq(AnomalyStatus.UNDER_REVIEW),
                    eq("Investigating the spike")))
                    .thenReturn(reviewed);

            AnomalyReviewRequest request = new AnomalyReviewRequest("UNDER_REVIEW", "Investigating the spike");

            mockMvc.perform(put("/api/v1/intelligence/anomalies/{id}/review", anomalyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(anomalyId.toString()))
                    .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                    .andExpect(jsonPath("$.reviewerNotes").value("Investigating the spike"));

            verify(anomalyDetectionService).reviewAnomaly(anomalyId, AnomalyStatus.UNDER_REVIEW,
                    "Investigating the spike");
        }

        @Test
        @DisplayName("PUT /anomalies/{id}/review returns 400 when anomaly not found")
        void reviewAnomaly_NotFound_ReturnsError() throws Exception {
            UUID anomalyId = UUID.randomUUID();

            when(anomalyDetectionService.reviewAnomaly(eq(anomalyId), any(), any()))
                    .thenThrow(new IllegalArgumentException("Anomaly not found: " + anomalyId));

            AnomalyReviewRequest request = new AnomalyReviewRequest("DISMISSED", "Not relevant");

            // IllegalArgumentException is caught by the IllegalArgument handler, returning 400
            mockMvc.perform(put("/api/v1/intelligence/anomalies/{id}/review", anomalyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Balance Forecast Endpoints ====================

    @Nested
    @DisplayName("Balance Forecast Endpoints")
    class BalanceForecastEndpoints {

        @Test
        @DisplayName("GET /forecasts returns 200 with forecast when found")
        void getLatestForecast_Found_Returns200() throws Exception {
            UUID employerId = UUID.randomUUID();
            UUID insurerId = UUID.randomUUID();

            BalanceForecastRecord record = buildForecastRecord(employerId, insurerId);

            when(balanceForecastService.getLatestForecast(employerId, insurerId))
                    .thenReturn(Optional.of(record));

            mockMvc.perform(get("/api/v1/intelligence/forecasts")
                            .param("employerId", employerId.toString())
                            .param("insurerId", insurerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employerId").value(employerId.toString()))
                    .andExpect(jsonPath("$.insurerId").value(insurerId.toString()))
                    .andExpect(jsonPath("$.forecastedAmount").value(50000.00))
                    .andExpect(jsonPath("$.narrative").value("Forecast narrative for testing"));

            verify(balanceForecastService).getLatestForecast(employerId, insurerId);
        }

        @Test
        @DisplayName("POST /forecasts/generate returns 200 with generated forecast")
        void generateForecast_Returns200() throws Exception {
            UUID employerId = UUID.randomUUID();
            UUID insurerId = UUID.randomUUID();

            BalanceForecastRecord record = buildForecastRecord(employerId, insurerId);

            when(balanceForecastService.generateForecast(employerId, insurerId))
                    .thenReturn(record);

            mockMvc.perform(post("/api/v1/intelligence/forecasts/generate")
                            .param("employerId", employerId.toString())
                            .param("insurerId", insurerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employerId").value(employerId.toString()))
                    .andExpect(jsonPath("$.forecastedAmount").value(50000.00));

            verify(balanceForecastService).generateForecast(employerId, insurerId);
        }

        @Test
        @DisplayName("GET /forecasts/history returns 200 with forecast history list")
        void getForecastHistory_Returns200() throws Exception {
            UUID employerId = UUID.randomUUID();
            UUID insurerId = UUID.randomUUID();

            BalanceForecastRecord record1 = buildForecastRecord(employerId, insurerId);
            BalanceForecastRecord record2 = buildForecastRecord(employerId, insurerId);
            record2.setForecastedAmount(new BigDecimal("60000.00"));

            when(balanceForecastService.getForecastHistory(employerId))
                    .thenReturn(List.of(record1, record2));

            mockMvc.perform(get("/api/v1/intelligence/forecasts/history")
                            .param("employerId", employerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].employerId").value(employerId.toString()))
                    .andExpect(jsonPath("$[1].forecastedAmount").value(60000.00));

            verify(balanceForecastService).getForecastHistory(employerId);
        }
    }

    // ==================== Error Resolution Endpoints ====================

    @Nested
    @DisplayName("Error Resolution Endpoints")
    class ErrorResolutionEndpoints {

        @Test
        @DisplayName("GET /error-resolutions returns 200 with resolution list")
        void listErrorResolutions_Returns200() throws Exception {
            UUID endorsementId = UUID.randomUUID();
            UUID resolutionId = UUID.randomUUID();

            ErrorResolution resolution = buildErrorResolution(resolutionId, endorsementId);

            when(errorResolutionService.findByEndorsementId(endorsementId))
                    .thenReturn(List.of(resolution));

            mockMvc.perform(get("/api/v1/intelligence/error-resolutions")
                            .param("endorsementId", endorsementId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(resolutionId.toString()))
                    .andExpect(jsonPath("$[0].errorType").value("DATE_FORMAT"))
                    .andExpect(jsonPath("$[0].confidence").value(0.98))
                    .andExpect(jsonPath("$[0].autoApplied").value(true));

            verify(errorResolutionService).findByEndorsementId(endorsementId);
        }

        @Test
        @DisplayName("GET /error-resolutions/stats returns 200 with stats")
        void getErrorResolutionStats_Returns200() throws Exception {
            ErrorResolutionStatsResponse stats = new ErrorResolutionStatsResponse(100L, 75L, 25L, 75.0, 60L, 15L, 80.0);

            when(errorResolutionService.getStats()).thenReturn(stats);

            mockMvc.perform(get("/api/v1/intelligence/error-resolutions/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResolutions").value(100))
                    .andExpect(jsonPath("$.autoApplied").value(75))
                    .andExpect(jsonPath("$.suggested").value(25))
                    .andExpect(jsonPath("$.autoApplyRate").value(75.0));

            verify(errorResolutionService).getStats();
        }

        @Test
        @DisplayName("POST /error-resolutions/{id}/approve returns 200")
        void approveResolution_Returns200() throws Exception {
            UUID resolutionId = UUID.randomUUID();

            doNothing().when(errorResolutionService).approveResolution(resolutionId);

            mockMvc.perform(post("/api/v1/intelligence/error-resolutions/{id}/approve", resolutionId))
                    .andExpect(status().isOk());

            verify(errorResolutionService).approveResolution(resolutionId);
        }
    }

    @Nested
    @DisplayName("Error Resolution Resolve Endpoint")
    class ErrorResolutionResolveEndpoint {

        @Test
        @DisplayName("POST /error-resolutions/resolve returns 200 with resolution")
        void resolveError_Returns200() throws Exception {
            UUID endorsementId = UUID.randomUUID();
            UUID resolutionId = UUID.randomUUID();

            ErrorResolution resolution = buildErrorResolution(resolutionId, endorsementId);

            when(errorResolutionService.attemptResolution(endorsementId, "Invalid date format: 07-03-1990"))
                    .thenReturn(true);
            when(errorResolutionService.findByEndorsementId(endorsementId))
                    .thenReturn(List.of(resolution));

            mockMvc.perform(post("/api/v1/intelligence/error-resolutions/resolve")
                            .param("endorsementId", endorsementId.toString())
                            .param("errorMessage", "Invalid date format: 07-03-1990"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(resolutionId.toString()))
                    .andExpect(jsonPath("$.errorType").value("DATE_FORMAT"))
                    .andExpect(jsonPath("$.confidence").value(0.98));

            verify(errorResolutionService).attemptResolution(endorsementId, "Invalid date format: 07-03-1990");
        }

        @Test
        @DisplayName("POST /error-resolutions/resolve returns 404 when no resolution found")
        void resolveError_NoResolution_Returns404() throws Exception {
            UUID endorsementId = UUID.randomUUID();

            when(errorResolutionService.attemptResolution(endorsementId, "Unknown error"))
                    .thenReturn(false);
            when(errorResolutionService.findByEndorsementId(endorsementId))
                    .thenReturn(List.of());

            mockMvc.perform(post("/api/v1/intelligence/error-resolutions/resolve")
                            .param("endorsementId", endorsementId.toString())
                            .param("errorMessage", "Unknown error"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Forecast Not Found ====================

    @Nested
    @DisplayName("Forecast Not Found Endpoint")
    class ForecastNotFoundEndpoint {

        @Test
        @DisplayName("GET /forecasts returns 404 when no forecast exists")
        void getLatestForecast_NotFound_Returns404() throws Exception {
            UUID employerId = UUID.randomUUID();
            UUID insurerId = UUID.randomUUID();

            when(balanceForecastService.getLatestForecast(employerId, insurerId))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/intelligence/forecasts")
                            .param("employerId", employerId.toString())
                            .param("insurerId", insurerId.toString()))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Process Mining Endpoints ====================

    @Nested
    @DisplayName("Process Mining Endpoints")
    class ProcessMiningEndpoints {

        @Test
        @DisplayName("GET /process-mining/metrics returns 200 with metrics list")
        void getProcessMiningMetrics_Returns200() throws Exception {
            UUID insurerId = UUID.randomUUID();

            ProcessMiningMetric metric = buildMetric(insurerId);

            when(processMiningService.getMetrics(insurerId))
                    .thenReturn(List.of(metric));

            mockMvc.perform(get("/api/v1/intelligence/process-mining/metrics")
                            .param("insurerId", insurerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].insurerId").value(insurerId.toString()))
                    .andExpect(jsonPath("$[0].fromStatus").value("CREATED"))
                    .andExpect(jsonPath("$[0].toStatus").value("VALIDATED"))
                    .andExpect(jsonPath("$[0].avgDurationMs").value(5000))
                    .andExpect(jsonPath("$[0].p95DurationMs").value(10000))
                    .andExpect(jsonPath("$[0].sampleCount").value(50))
                    .andExpect(jsonPath("$[0].happyPathPct").value(85.00));

            verify(processMiningService).getMetrics(insurerId);
        }

        @Test
        @DisplayName("GET /process-mining/insights returns 200 with insights list")
        void getInsights_Returns200() throws Exception {
            UUID insurerId = UUID.randomUUID();

            ProcessMiningInsightResponse insight = new ProcessMiningInsightResponse(
                    insurerId, "ICICI Lombard", "BOTTLENECK",
                    "Bottleneck detected: CREATED -> VALIDATED averages 2.5 hours",
                    Instant.now());

            when(processMiningService.getLatestInsights())
                    .thenReturn(List.of(insight));

            mockMvc.perform(get("/api/v1/intelligence/process-mining/insights"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].insurerId").value(insurerId.toString()))
                    .andExpect(jsonPath("$[0].insurerName").value("ICICI Lombard"))
                    .andExpect(jsonPath("$[0].insightType").value("BOTTLENECK"));

            verify(processMiningService).getLatestInsights();
        }

        @Test
        @DisplayName("GET /process-mining/stp-rate returns 200 with STP rate")
        void getStpRate_Returns200() throws Exception {
            UUID insurerId = UUID.randomUUID();

            StpRateResponse stpRate = new StpRateResponse(
                    new BigDecimal("92.50"),
                    Map.of(insurerId, new BigDecimal("92.50")),
                    100L,
                    93L);

            when(processMiningService.getStpRate(insurerId))
                    .thenReturn(stpRate);

            mockMvc.perform(get("/api/v1/intelligence/process-mining/stp-rate")
                            .param("insurerId", insurerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overallStpRate").value(92.50))
                    .andExpect(jsonPath("$.perInsurerStpRate").isMap());

            verify(processMiningService).getStpRate(insurerId);
        }

        @Test
        @DisplayName("POST /process-mining/analyze returns 202 accepted")
        void triggerAnalysis_Returns202() throws Exception {
            doNothing().when(processMiningService).generateInsights();

            mockMvc.perform(post("/api/v1/intelligence/process-mining/analyze"))
                    .andExpect(status().isAccepted());

            verify(processMiningService).generateInsights();
        }
    }
}
