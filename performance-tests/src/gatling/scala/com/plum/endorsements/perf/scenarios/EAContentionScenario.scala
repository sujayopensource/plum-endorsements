package com.plum.endorsements.perf.scenarios

import com.plum.endorsements.perf.feeders.EndorsementFeeders._
import com.plum.endorsements.perf.requests.EAAccountRequests._
import com.plum.endorsements.perf.requests.EndorsementRequests._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

object EAContentionScenario {

  // All virtual users target the same employer+insurer (first entry from feeders)
  private val fixedEmployerId = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d"
  private val fixedInsurerId = "11111111-1111-4111-8111-111111111111"

  val contention: ScenarioBuilder = scenario("EA Contention")
    .exec(session => session
      .set("employerId", fixedEmployerId)
      .set("insurerId", fixedInsurerId)
      .set("endorsementType", "ADD")
    )
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(createEndorsement)
    .exec(getBalance)
}
