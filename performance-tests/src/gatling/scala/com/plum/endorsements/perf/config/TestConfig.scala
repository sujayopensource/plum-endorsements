package com.plum.endorsements.perf.config

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

object TestConfig {

  val baseUrl: String = System.getProperty("baseUrl", "http://localhost:8080")
  val dbUrl: String = System.getProperty("dbUrl", "jdbc:postgresql://localhost:5432/endorsements")
  val dbUser: String = System.getProperty("dbUser", "plum")
  val dbPassword: String = System.getProperty("dbPassword", "plum_dev")

  val durationMinutes: Int = Integer.getInteger("durationMinutes", 15)
  val targetRps: Int = Integer.getInteger("targetRps", 20)
  val rampDurationSeconds: Int = Integer.getInteger("rampDurationSeconds", 60)
  val thinkTimeMs: Int = Integer.getInteger("thinkTimeMs", 1000)

  val testDuration: FiniteDuration = durationMinutes.minutes
  val rampDuration: FiniteDuration = rampDurationSeconds.seconds
  val thinkTime: FiniteDuration = thinkTimeMs.milliseconds

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .header("X-Request-Id", session => java.util.UUID.randomUUID().toString)
    .maxConnectionsPerHost(10)
    .shareConnections
}
