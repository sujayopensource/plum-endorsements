package com.plum.endorsements.perf.scenarios

import com.plum.endorsements.perf.feeders.EndorsementFeeders._
import com.plum.endorsements.perf.requests.EndorsementRequests._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._

import scala.concurrent.duration._

object MultiInsurerScenario {

  // Multi-insurer feeder cycling through 4 configured insurers
  val multiInsurerFeeder: Iterator[Map[String, String]] = Iterator.continually(
    Vector(
      Map("insurerId" -> "22222222-2222-2222-2222-222222222222", "insurerName" -> "Mock"),
      Map("insurerId" -> "33333333-3333-3333-3333-333333333333", "insurerName" -> "ICICI_LOMBARD"),
      Map("insurerId" -> "44444444-4444-4444-4444-444444444444", "insurerName" -> "NIVA_BUPA"),
      Map("insurerId" -> "55555555-5555-5555-5555-555555555555", "insurerName" -> "BAJAJ_ALLIANZ")
    )
  ).flatten

  val createAcrossInsurers: ScenarioBuilder = scenario("Multi-Insurer Create")
    .feed(employerFeeder)
    .feed(multiInsurerFeeder)
    .feed(endorsementTypeFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(createEndorsement)
    .pause(200.milliseconds, 800.milliseconds)
    .exec(getEndorsement)

  val submitAcrossInsurers: ScenarioBuilder = scenario("Multi-Insurer Submit")
    .feed(employerFeeder)
    .feed(multiInsurerFeeder)
    .feed(endorsementTypeFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(createEndorsement)
    .pause(100.milliseconds, 500.milliseconds)
    .exec(submitEndorsement)
    .pause(200.milliseconds, 800.milliseconds)
    .exec(getEndorsement)

  val listInsurers: ScenarioBuilder = scenario("List Insurers")
    .exec(
      http("List Insurer Configs")
        .get("/api/v1/insurers")
        .check(status.is(200))
        .check(jsonPath("$.size()").ofType[Int].gte(4))
    )
}
