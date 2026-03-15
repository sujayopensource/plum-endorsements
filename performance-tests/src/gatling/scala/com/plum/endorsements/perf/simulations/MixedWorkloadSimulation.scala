package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.scenarios.MixedWorkloadScenario._
import io.gatling.core.Predef._

import scala.concurrent.duration._

class MixedWorkloadSimulation extends Simulation {

  setUp(
    mixed.inject(
      constantUsersPerSec(targetRps.toDouble).during(testDuration)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(50).lt(200),
      global.responseTime.percentile(95).lt(500),
      global.responseTime.percentile(99).lt(1500),
      global.successfulRequests.percent.gt(99.0),
      details("Create Endorsement").responseTime.percentile(95).lt(500),
      details("Get Endorsement").responseTime.percentile(95).lt(100),
      details("Submit Endorsement").responseTime.percentile(95).lt(800)
    )
}
