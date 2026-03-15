package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.scenarios.MultiInsurerScenario._
import io.gatling.core.Predef._

import scala.concurrent.duration._

class MultiInsurerLoadSimulation extends Simulation {

  setUp(
    createAcrossInsurers.inject(
      rampUsersPerSec(0.5).to(30).during(2.minutes),
      constantUsersPerSec(30).during(13.minutes)
    ),
    submitAcrossInsurers.inject(
      nothingFor(30.seconds),
      rampUsersPerSec(0.5).to(15).during(2.minutes),
      constantUsersPerSec(15).during(12.minutes + 30.seconds)
    ),
    listInsurers.inject(
      constantUsersPerSec(2).during(15.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(95).lt(1000),
      global.successfulRequests.percent.gt(99.0),
      forAll.responseTime.max.lt(10000),
      details("Create Endorsement").responseTime.percentile(95).lt(500),
      details("Submit Endorsement").responseTime.percentile(95).lt(1000)
    )
}
