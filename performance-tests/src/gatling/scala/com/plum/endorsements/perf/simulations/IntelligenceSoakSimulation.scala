package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.requests.IntelligenceRequests._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
 * Intelligence Soak Simulation
 *
 * Tests intelligence endpoints under sustained constant load for memory leak
 * detection and response time degradation analysis. Response times should remain
 * stable throughout the entire duration; any upward trend indicates resource
 * leaks or connection pool exhaustion.
 *
 * Profile:
 *   - Constant 10 users/sec for 5 minutes
 *   - Mix of all intelligence read + write endpoints
 *
 * Focus:
 *   - Response times should NOT degrade over time
 *   - Memory usage should remain stable (checked externally)
 */
class IntelligenceSoakSimulation extends Simulation {

  val employerFeeder = csv("feeders/employers.csv").circular
  val insurerFeeder = csv("feeders/insurers.csv").circular

  val anomalyIdFeeder = Iterator.continually(Map(
    "anomalyId" -> java.util.UUID.randomUUID().toString
  ))

  val resolutionIdFeeder = Iterator.continually(Map(
    "resolutionId" -> java.util.UUID.randomUUID().toString
  ))

  val soakReadScenario = scenario("Intelligence Soak - Read Operations")
    .randomSwitch(
      20.0 -> exec(listAnomalies),
      15.0 -> exec(listAnomaliesByStatus),
      15.0 -> exec(getErrorResolutionStats),
      15.0 -> exec(getProcessMiningMetrics),
      15.0 -> exec(getStpRate),
      10.0 -> exec(getProcessMiningInsights),
      10.0 -> feed(employerFeeder).exec(getForecastHistory)
    )
    .pause(1.second, 3.seconds)

  val soakWriteScenario = scenario("Intelligence Soak - Write Operations")
    .randomSwitch(
      30.0 -> exec(triggerAnalysis),
      30.0 -> feed(employerFeeder).feed(insurerFeeder).exec(generateForecast),
      20.0 -> feed(anomalyIdFeeder).exec(reviewAnomaly),
      20.0 -> feed(resolutionIdFeeder).exec(approveResolution)
    )
    .pause(2.seconds, 5.seconds)

  setUp(
    soakReadScenario.inject(
      rampUsersPerSec(1).to(8).during(30.seconds),
      constantUsersPerSec(8).during(5.minutes)
    ),
    soakWriteScenario.inject(
      rampUsersPerSec(0.5).to(2).during(30.seconds),
      constantUsersPerSec(2).during(5.minutes)
    )
  ).protocols(httpProtocol)
    .maxDuration(6.minutes)
    .assertions(
      global.responseTime.percentile(95).lt(500),
      global.responseTime.percentile(99).lt(1500),
      global.successfulRequests.percent.gt(99.0),
      details("List Anomalies").responseTime.percentile(95).lt(300),
      details("Get Error Resolution Stats").responseTime.percentile(95).lt(300),
      details("Get STP Rate").responseTime.percentile(95).lt(200),
      details("Get Process Mining Metrics").responseTime.percentile(95).lt(300)
    )
}
