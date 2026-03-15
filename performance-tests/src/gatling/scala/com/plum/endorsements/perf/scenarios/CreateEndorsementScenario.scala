package com.plum.endorsements.perf.scenarios

import com.plum.endorsements.perf.feeders.EndorsementFeeders._
import com.plum.endorsements.perf.requests.EndorsementRequests._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._

object CreateEndorsementScenario {

  val create: ScenarioBuilder = scenario("Create Endorsement")
    .feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(endorsementTypeFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(createEndorsement)
    .pause(300.milliseconds, 1.second)
    .exec(getEndorsement)
}
