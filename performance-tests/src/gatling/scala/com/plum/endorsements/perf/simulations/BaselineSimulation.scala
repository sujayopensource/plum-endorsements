package com.plum.endorsements.perf.simulations

import com.plum.endorsements.perf.config.TestConfig._
import com.plum.endorsements.perf.scenarios.FullLifecycleScenario._
import io.gatling.core.Predef._

import scala.concurrent.duration._

class BaselineSimulation extends Simulation {

  // When Ollama LLM is enabled, anomaly detection adds ~30s latency per endorsement creation.
  // Relax the p95 threshold accordingly so the baseline still validates functional correctness.
  private val ollamaEnabled: Boolean = System.getProperty("ollamaEnabled", "false").toBoolean
  private val p95Threshold: Int = if (ollamaEnabled) 60000 else 2000

  setUp(
    lifecycle.inject(
      atOnceUsers(1)
    )
  ).protocols(httpProtocol)
    .maxDuration(2.minutes)
    .assertions(
      global.responseTime.percentile(95).lt(p95Threshold),
      global.successfulRequests.percent.gt(99.0)
    )
}
