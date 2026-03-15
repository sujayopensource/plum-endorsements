package com.plum.endorsements.perf.scenarios

import com.plum.endorsements.perf.feeders.EndorsementFeeders._
import com.plum.endorsements.perf.requests.EAAccountRequests._
import com.plum.endorsements.perf.requests.EndorsementRequests._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._

object MixedWorkloadScenario {

  private val createADD = feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(session => session.set("endorsementType", "ADD"))
    .exec(createEndorsement)

  private val createUPDATE = feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(session => session.set("endorsementType", "UPDATE"))
    .exec(createEndorsement)

  private val createDELETE = feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(session => session.set("endorsementType", "DELETE"))
    .exec(createEndorsement)

  private val readEndorsement = feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(session => session.set("endorsementType", "ADD"))
    .exec(createEndorsement)
    .pause(100.milliseconds)
    .exec(getEndorsement)

  private val listEndorsementsFlow = feed(employerFeeder)
    .exec(listEndorsements)

  private val submitAndConfirm = feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(session => session.set("endorsementType", "ADD"))
    .exec(createEndorsement)
    .pause(200.milliseconds)
    .exec(submitEndorsement)

  private val checkBalance = feed(employerFeeder)
    .feed(insurerFeeder)
    .exec(getBalance)

  val mixed: ScenarioBuilder = scenario("Mixed Workload")
    .randomSwitch(
      30.0 -> createADD,
      12.0 -> createUPDATE,
      8.0 -> createDELETE,
      20.0 -> readEndorsement,
      15.0 -> listEndorsementsFlow,
      10.0 -> submitAndConfirm,
      5.0 -> checkBalance
    )
}
