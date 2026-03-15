package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.feeders.EndorsementFeeders._
import com.plum.endorsements.perf.requests.EndorsementRequests._
import com.plum.endorsements.perf.requests.IntelligenceRequests._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
 * Batch Optimization Under Load Simulation
 *
 * Tests batch optimization behaviour under high endorsement creation load.
 * Verifies that batch assembly completes within SLA while endorsements are
 * being created concurrently.
 *
 * Phases:
 *   1. Create 500 endorsements rapidly (50/sec for 10 sec)
 *   2. Trigger batch assembly while new endorsements still flowing
 *   3. Verify batch optimization completes within SLA
 */
class BatchOptimizationUnderLoadSimulation extends Simulation {

  // Phase 1: Rapid endorsement creation
  val rapidCreationScenario = scenario("Phase 1 - Rapid Endorsement Creation")
    .feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(session => session.set("endorsementType", "ADD"))
    .exec(createEndorsement)

  // Phase 2: Trigger batch assembly while endorsements still flowing
  val batchAssemblyScenario = scenario("Phase 2 - Batch Assembly Under Load")
    .pause(5.seconds)  // wait for some endorsements to accumulate
    .repeat(10) {
      exec(
        http("Trigger Batch Assembly")
          .post("/api/v1/endorsements/batch/assemble")
          .check(status.in(200, 202, 204))
      )
      .pause(3.seconds, 5.seconds)
    }

  // Phase 3: Monitor batch status and verify completion
  val batchVerificationScenario = scenario("Phase 3 - Batch Verification")
    .pause(15.seconds)  // wait for batch assembly to start
    .repeat(20) {
      exec(
        http("Check Batch Status")
          .get("/api/v1/endorsements/batch/status")
          .check(status.in(200, 404))
      )
      .pause(2.seconds, 4.seconds)
    }

  // Concurrent slow-drip creation to simulate ongoing traffic
  val ongoingCreationScenario = scenario("Ongoing Endorsement Creation")
    .feed(employerFeeder)
    .feed(insurerFeeder)
    .feed(employeeDataFeeder)
    .feed(premiumFeeder)
    .feed(coverageDateFeeder)
    .exec(session => session.set("endorsementType", "UPDATE"))
    .exec(createEndorsement)
    .pause(500.milliseconds, 1.second)

  // Intelligence monitoring during batch processing
  val intelligenceMonitorScenario = scenario("Intelligence Monitoring During Batch")
    .pause(10.seconds)
    .repeat(5) {
      exec(getProcessMiningMetrics)
        .pause(2.seconds)
        .exec(getStpRate)
        .pause(3.seconds)
    }

  setUp(
    // Phase 1: burst creation - 50 users/sec for 10 seconds = ~500 endorsements
    rapidCreationScenario.inject(
      constantUsersPerSec(50).during(10.seconds)
    ),
    // Phase 2: batch assembly triggered concurrently
    batchAssemblyScenario.inject(
      atOnceUsers(2)
    ),
    // Phase 3: verification
    batchVerificationScenario.inject(
      atOnceUsers(2)
    ),
    // Ongoing creation during batch processing
    ongoingCreationScenario.inject(
      nothingFor(10.seconds),
      constantUsersPerSec(5).during(50.seconds)
    ),
    // Intelligence monitoring
    intelligenceMonitorScenario.inject(
      atOnceUsers(1)
    )
  ).protocols(httpProtocol)
    .maxDuration(3.minutes)
    .assertions(
      global.successfulRequests.percent.gt(95.0),
      details("Create Endorsement").responseTime.percentile(95).lt(2000),
      details("Trigger Batch Assembly").responseTime.percentile(95).lt(3000),
      details("Check Batch Status").responseTime.percentile(95).lt(500),
      details("Create Endorsement").failedRequests.percent.lt(5.0)
    )
}
