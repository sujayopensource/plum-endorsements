package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.scenarios.MixedWorkloadScenario._
import io.gatling.core.Predef._

import scala.concurrent.duration._

class SpikeSimulation extends Simulation {

  setUp(
    mixed.inject(
      rampUsersPerSec(10).to(50).during(1.minute),
      constantUsersPerSec(100).during(2.minutes),
      rampUsersPerSec(100).to(500).during(1.minute),
      constantUsersPerSec(500).during(2.minutes),
      rampUsersPerSec(500).to(100).during(2.minutes),
      constantUsersPerSec(100).during(6.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.failedRequests.percent.lt(5.0),
      global.responseTime.max.lt(10000)
    )
}
