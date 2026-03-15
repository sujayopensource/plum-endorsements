package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.requests.IntelligenceRequests._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
 * Intelligence Spike Simulation
 *
 * Tests intelligence endpoints under sudden load spikes to verify the system
 * can handle burst traffic patterns without degraded response times or errors.
 *
 * Profile:
 *   - Warmup:   5 users/sec for 30 seconds
 *   - Spike:   50 users/sec for 60 seconds
 *   - Recovery: 5 users/sec for 30 seconds
 */
class IntelligenceSpikeSimulation extends Simulation {

  val employerFeeder = csv("feeders/employers.csv").circular
  val insurerFeeder = csv("feeders/insurers.csv").circular

  val intelligenceReadMix = scenario("Intelligence Spike - Read Mix")
    .randomSwitch(
      25.0 -> exec(listAnomalies),
      20.0 -> exec(listAnomaliesByStatus),
      15.0 -> exec(getErrorResolutionStats),
      15.0 -> exec(getProcessMiningMetrics),
      10.0 -> exec(getStpRate),
      10.0 -> exec(getProcessMiningInsights),
      5.0  -> feed(employerFeeder).exec(getForecastHistory)
    )
    .pause(500.milliseconds, 2.seconds)

  setUp(
    intelligenceReadMix.inject(
      // Warmup: baseline load
      constantUsersPerSec(5).during(30.seconds),
      // Spike: sudden jump to 10x load
      rampUsersPerSec(5).to(50).during(5.seconds),
      constantUsersPerSec(50).during(60.seconds),
      // Recovery: drop back to baseline
      rampUsersPerSec(50).to(5).during(5.seconds),
      constantUsersPerSec(5).during(30.seconds)
    )
  ).protocols(httpProtocol)
    .maxDuration(5.minutes)
    .assertions(
      global.responseTime.percentile(99).lt(2000),
      global.successfulRequests.percent.gt(95.0),
      global.failedRequests.count.lt(50),
      details("List Anomalies").responseTime.percentile(95).lt(1000),
      details("Get STP Rate").responseTime.percentile(95).lt(1000),
      details("Get Process Mining Metrics").responseTime.percentile(95).lt(1000)
    )
}
