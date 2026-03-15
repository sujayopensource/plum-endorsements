package com.plum.endorsements.perf.requests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object EndorsementRequests {

  val createEndorsement: HttpRequestBuilder =
    http("Create Endorsement")
      .post("/api/v1/endorsements")
      .body(StringBody(session =>
        s"""{
           |  "employerId": "${session("employerId").as[String]}",
           |  "employeeId": "${session("employeeId").as[String]}",
           |  "insurerId": "${session("insurerId").as[String]}",
           |  "policyId": "${session("policyId").as[String]}",
           |  "type": "${session("endorsementType").as[String]}",
           |  "coverageStartDate": "${session("coverageStartDate").as[String]}",
           |  "coverageEndDate": "${session("coverageEndDate").as[String]}",
           |  "premiumAmount": ${session("premiumAmount").as[String]},
           |  "employeeData": {
           |    "name": "${session("employeeName").as[String]}",
           |    "age": ${session("employeeAge").as[Int]},
           |    "department": "${session("employeeDept").as[String]}",
           |    "email": "${session("employeeEmail").as[String]}"
           |  },
           |  "idempotencyKey": "${session("idempotencyKey").as[String]}"
           |}""".stripMargin
      ))
      .check(status.is(201))
      .check(jsonPath("$.id").saveAs("endorsementId"))
      .check(jsonPath("$.status").saveAs("endorsementStatus"))

  val getEndorsement: HttpRequestBuilder =
    http("Get Endorsement")
      .get("/api/v1/endorsements/#{endorsementId}")
      .check(status.is(200))
      .check(jsonPath("$.status").saveAs("endorsementStatus"))

  val listEndorsements: HttpRequestBuilder =
    http("List Endorsements")
      .get("/api/v1/endorsements")
      .queryParam("employerId", "#{employerId}")
      .queryParam("page", "0")
      .queryParam("size", "20")
      .check(status.is(200))

  val listFilteredEndorsements: HttpRequestBuilder =
    http("List Filtered Endorsements")
      .get("/api/v1/endorsements")
      .queryParam("employerId", "#{employerId}")
      .queryParam("statuses", "CONFIRMED")
      .queryParam("page", "0")
      .queryParam("size", "20")
      .check(status.is(200))

  val submitEndorsement: HttpRequestBuilder =
    http("Submit Endorsement")
      .post("/api/v1/endorsements/#{endorsementId}/submit")
      .check(status.is(202))

  val confirmEndorsement: HttpRequestBuilder =
    http("Confirm Endorsement")
      .post("/api/v1/endorsements/#{endorsementId}/confirm")
      .queryParam("insurerReference", session => s"INS-PERF-${java.util.UUID.randomUUID()}")
      .check(status.is(200))

  val rejectEndorsement: HttpRequestBuilder =
    http("Reject Endorsement")
      .post("/api/v1/endorsements/#{endorsementId}/reject")
      .queryParam("reason", "perf-test-rejection")
      .check(status.is(200))

  val getCoverage: HttpRequestBuilder =
    http("Get Coverage")
      .get("/api/v1/endorsements/#{endorsementId}/coverage")
      .check(status.in(200, 404))
}
