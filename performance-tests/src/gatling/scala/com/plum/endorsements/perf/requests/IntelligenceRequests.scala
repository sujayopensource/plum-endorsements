package com.plum.endorsements.perf.requests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object IntelligenceRequests {

  val listAnomalies: HttpRequestBuilder =
    http("List Anomalies")
      .get("/api/v1/intelligence/anomalies")
      .check(status.is(200))

  val listAnomaliesByStatus: HttpRequestBuilder =
    http("List Anomalies by Status")
      .get("/api/v1/intelligence/anomalies")
      .queryParam("status", "FLAGGED")
      .check(status.is(200))

  val getErrorResolutionStats: HttpRequestBuilder =
    http("Get Error Resolution Stats")
      .get("/api/v1/intelligence/error-resolutions/stats")
      .check(status.is(200))
      .check(jsonPath("$.totalResolutions").exists)

  val getProcessMiningMetrics: HttpRequestBuilder =
    http("Get Process Mining Metrics")
      .get("/api/v1/intelligence/process-mining/metrics")
      .check(status.is(200))

  val getProcessMiningInsights: HttpRequestBuilder =
    http("Get Process Mining Insights")
      .get("/api/v1/intelligence/process-mining/insights")
      .check(status.is(200))

  val getStpRate: HttpRequestBuilder =
    http("Get STP Rate")
      .get("/api/v1/intelligence/process-mining/stp-rate")
      .check(status.is(200))
      .check(jsonPath("$.overallStpRate").exists)

  val triggerAnalysis: HttpRequestBuilder =
    http("Trigger Process Mining Analysis")
      .post("/api/v1/intelligence/process-mining/analyze")
      .check(status.is(202))

  val generateForecast: HttpRequestBuilder =
    http("Generate Balance Forecast")
      .post("/api/v1/intelligence/forecasts/generate")
      .queryParam("employerId", "#{employerId}")
      .queryParam("insurerId", "#{insurerId}")
      .check(status.in(200, 404))

  val reviewAnomaly: HttpRequestBuilder =
    http("Review Anomaly")
      .put("/api/v1/intelligence/anomalies/#{anomalyId}/review")
      .body(StringBody(
        """{
          |  "status": "REVIEWED",
          |  "reviewerNotes": "Reviewed during performance test",
          |  "actionTaken": "ACKNOWLEDGED"
          |}""".stripMargin
      ))
      .check(status.in(200, 404))

  val approveResolution: HttpRequestBuilder =
    http("Approve Error Resolution")
      .post("/api/v1/intelligence/error-resolutions/#{resolutionId}/approve")
      .check(status.in(200, 404))

  val getForecastHistory: HttpRequestBuilder =
    http("Get Forecast History")
      .get("/api/v1/intelligence/forecasts/history")
      .queryParam("employerId", "#{employerId}")
      .check(status.in(200, 404))
}
