package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.requests.EndorsementRequests._
import com.plum.endorsements.perf.requests.IntelligenceRequests._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class AnomalyDetectionUnderLoadSimulation extends Simulation {

  val employerId = "11111111-1111-1111-1111-111111111111"
  val insurerId = "44444444-4444-4444-4444-444444444444"

  val rapidCreateFeeder = Iterator.continually(Map(
    "employerId" -> employerId,
    "employeeId" -> java.util.UUID.randomUUID().toString,
    "insurerId" -> insurerId,
    "policyId" -> java.util.UUID.randomUUID().toString,
    "endorsementType" -> "ADD",
    "coverageStartDate" -> java.time.LocalDate.now().plusDays(1).toString,
    "coverageEndDate" -> java.time.LocalDate.now().plusYears(1).toString,
    "premiumAmount" -> (1000 + scala.util.Random.nextInt(5000)).toString,
    "employeeName" -> s"PerfTest-${scala.util.Random.nextInt(10000)}",
    "employeeAge" -> (25 + scala.util.Random.nextInt(35)),
    "employeeDept" -> "Engineering",
    "employeeEmail" -> s"perf${scala.util.Random.nextInt(10000)}@test.com",
    "idempotencyKey" -> java.util.UUID.randomUUID().toString
  ))

  val rapidCreationScenario = scenario("Rapid Endorsement Creation (Volume Spike)")
    .feed(rapidCreateFeeder)
    .exec(createEndorsement)

  val anomalyCheckScenario = scenario("Check Anomalies After Volume Spike")
    .pause(30.seconds)
    .repeat(10) {
      exec(listAnomaliesByStatus)
        .pause(30.seconds)
    }

  setUp(
    rapidCreationScenario.inject(
      rampUsersPerSec(10).to(100).during(30.seconds),
      constantUsersPerSec(100).during(3.minutes)
    ),
    anomalyCheckScenario.inject(
      atOnceUsers(2)
    )
  ).protocols(httpProtocol)
    .maxDuration(5.minutes)
    .assertions(
      global.successfulRequests.percent.gt(95.0),
      details("Create Endorsement").responseTime.percentile(95).lt(2000)
    )
}
