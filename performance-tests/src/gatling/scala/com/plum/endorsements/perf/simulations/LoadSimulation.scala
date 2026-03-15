package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.scenarios.CreateEndorsementScenario._
import com.plum.endorsements.perf.scenarios.FullLifecycleScenario._
import com.plum.endorsements.perf.scenarios.RetrievalScenario._
import io.gatling.core.Predef._

import scala.concurrent.duration._

class LoadSimulation extends Simulation {

  setUp(
    create.inject(
      constantUsersPerSec(20).during(15.minutes)
    ),
    retrieve.inject(
      constantUsersPerSec(50).during(15.minutes)
    ),
    lifecycle.inject(
      constantUsersPerSec(10).during(15.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(95).lt(500),
      global.successfulRequests.percent.gt(99.9),
      forAll.responseTime.max.lt(5000)
    )
}
