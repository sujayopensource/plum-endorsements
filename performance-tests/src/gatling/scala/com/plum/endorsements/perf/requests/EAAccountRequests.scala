package com.plum.endorsements.perf.requests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object EAAccountRequests {

  val getBalance: HttpRequestBuilder =
    http("Get EA Balance")
      .get("/api/v1/ea-accounts")
      .queryParam("employerId", "#{employerId}")
      .queryParam("insurerId", "#{insurerId}")
      .check(status.in(200, 404))
      .check(
        checkIf((response: io.gatling.http.response.Response, _: Session) =>
          response.status.code == 200
        )(jsonPath("$.availableBalance").saveAs("availableBalance"))
      )
}
