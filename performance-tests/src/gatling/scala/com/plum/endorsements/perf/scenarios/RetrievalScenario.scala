package com.plum.endorsements.perf.scenarios

import com.plum.endorsements.perf.feeders.EndorsementFeeders._
import com.plum.endorsements.perf.requests.EndorsementRequests._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._

object RetrievalScenario {

  val retrieve: ScenarioBuilder = scenario("Retrieval")
    .feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(endorsementTypeFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    // First create an endorsement to get a valid ID
    .exec(createEndorsement)
    .pause(200.milliseconds, 500.milliseconds)
    // Then test retrieval operations
    .exec(getEndorsement)
    .pause(200.milliseconds, 500.milliseconds)
    .exec(listEndorsements)
    .pause(200.milliseconds, 500.milliseconds)
    .exec(listFilteredEndorsements)
}
