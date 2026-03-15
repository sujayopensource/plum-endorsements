package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.requests.IntelligenceRequests._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class IntelligenceApiSimulation extends Simulation {

  val intelligenceReadScenario = scenario("Intelligence API Read Load")
    .exec(listAnomalies)
    .pause(500.milliseconds, 1.second)
    .exec(listAnomaliesByStatus)
    .pause(500.milliseconds, 1.second)
    .exec(getErrorResolutionStats)
    .pause(500.milliseconds, 1.second)
    .exec(getProcessMiningMetrics)
    .pause(500.milliseconds, 1.second)
    .exec(getProcessMiningInsights)
    .pause(500.milliseconds, 1.second)
    .exec(getStpRate)

  val intelligenceWriteScenario = scenario("Intelligence API Write Load")
    .exec(triggerAnalysis)
    .pause(2.seconds, 5.seconds)

  setUp(
    intelligenceReadScenario.inject(
      rampUsersPerSec(1).to(20).during(30.seconds),
      constantUsersPerSec(20).during(2.minutes)
    ),
    intelligenceWriteScenario.inject(
      rampUsersPerSec(0.1).to(2).during(30.seconds),
      constantUsersPerSec(2).during(2.minutes)
    )
  ).protocols(httpProtocol)
    .maxDuration(5.minutes)
    .assertions(
      global.responseTime.percentile(95).lt(500),
      global.successfulRequests.percent.gt(99.0),
      details("List Anomalies").responseTime.percentile(95).lt(300),
      details("Get Error Resolution Stats").responseTime.percentile(95).lt(200),
      details("Get STP Rate").responseTime.percentile(95).lt(200)
    )
}
