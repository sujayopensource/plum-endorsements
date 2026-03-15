package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.scenarios.EAContentionScenario._
import io.gatling.core.Predef._

class EAContentionSimulation extends Simulation {

  setUp(
    contention.inject(
      atOnceUsers(200)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.successfulRequests.percent.is(100.0)
    )
}
