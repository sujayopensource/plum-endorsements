package com.plum.endorsements.perf.scenarios

import com.plum.endorsements.perf.feeders.EndorsementFeeders._
import com.plum.endorsements.perf.requests.EAAccountRequests._
import com.plum.endorsements.perf.requests.EndorsementRequests._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._

object FullLifecycleScenario {

  val lifecycle: ScenarioBuilder = scenario("Full Lifecycle")
    .feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    // Force ADD type for full lifecycle with provisional coverage + EA reservation
    .exec(session => session.set("endorsementType", "ADD"))
    // Step 1: Create endorsement
    .exec(createEndorsement)
    .pause(500.milliseconds)
    // Step 2: Verify status is PROVISIONALLY_COVERED
    .exec(getEndorsement)
    .pause(200.milliseconds)
    // Step 3: Submit to insurer
    .exec(submitEndorsement)
    .pause(500.milliseconds)
    // Step 4: Verify status after submission (mock auto-confirms -> CONFIRMED)
    .exec(getEndorsement)
    // Step 5: Check provisional coverage
    .exec(getCoverage)
    // Step 6: Check EA balance
    .exec(getBalance)
}
