package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.scenarios.MixedWorkloadScenario._
import io.gatling.core.Predef._

import scala.concurrent.duration._

class SoakSimulation extends Simulation {

  setUp(
    mixed.inject(
      rampUsersPerSec(1).to(100).during(2.minutes),
      constantUsersPerSec(100).during(4.hours)
    )
  ).protocols(httpProtocol)
    .maxDuration(4.hours.plus(5.minutes))
    .assertions(
      global.responseTime.percentile(95).lt(1000),
      global.successfulRequests.percent.gt(99.0)
    )
}
