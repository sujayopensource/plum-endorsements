package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.scenarios.FullLifecycleScenario._
import io.gatling.core.Predef._

import scala.concurrent.duration._

class FullLifecycleSimulation extends Simulation {

  setUp(
    lifecycle.inject(
      rampUsersPerSec(1).to(50).during(5.minutes),
      constantUsersPerSec(50).during(10.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(95).lt(1000),
      global.successfulRequests.percent.gt(99.0)
    )
}
