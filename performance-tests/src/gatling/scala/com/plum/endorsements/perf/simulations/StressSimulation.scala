package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.scenarios.MixedWorkloadScenario._
import io.gatling.core.Predef._

import scala.concurrent.duration._

class StressSimulation extends Simulation {

  setUp(
    mixed.inject(
      rampUsersPerSec(10).to(500).during(10.minutes),
      constantUsersPerSec(500).during(5.minutes),
      rampUsersPerSec(500).to(10).during(5.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(99).lt(3000),
      global.failedRequests.percent.lt(5.0)
    )
}
