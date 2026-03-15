package com.plum.endorsements.perf.scenarios

import com.plum.endorsements.perf.requests.IntelligenceRequests._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._

object IntelligenceMixedWorkloadScenario {

  val readHeavyScenario: ScenarioBuilder = scenario("Intelligence Read-Heavy")
    .exec(listAnomalies)
    .pause(1.second)
    .exec(getErrorResolutionStats)
    .pause(1.second)
    .exec(getProcessMiningMetrics)
    .pause(1.second)
    .exec(getStpRate)
    .pause(1.second)
    .exec(getProcessMiningInsights)

  val writeHeavyScenario: ScenarioBuilder = scenario("Intelligence Write-Heavy")
    .exec(generateForecast)
    .pause(2.seconds)
    .exec(triggerAnalysis)
    .pause(2.seconds)

  val mixedScenario: ScenarioBuilder = scenario("Intelligence Mixed")
    .randomSwitch(
      70.0 -> exec(readHeavyScenario),
      30.0 -> exec(writeHeavyScenario)
    )
}
