package com.plum.endorsements.perf.feeders

import io.gatling.core.Predef._
import io.gatling.core.feeder._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.util.Random

object EndorsementFeeders {

  val employerFeeder: FeederBuilder = csv("feeders/employers.csv").circular

  val insurerFeeder: FeederBuilder = csv("feeders/insurers.csv").circular

  val endorsementTypeFeeder: FeederBuilder = {
    val types = Vector.fill(60)("ADD") ++ Vector.fill(25)("UPDATE") ++ Vector.fill(15)("DELETE")
    types.map(t => Map("endorsementType" -> t)).iterator.toIndexedSeq.circular
  }

  private val departments = Array("Engineering", "Marketing", "Finance", "Operations", "Human Resources",
    "Sales", "Legal", "Product", "Design", "Support")
  private val firstNames = Array("James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael",
    "Linda", "David", "Elizabeth", "William", "Barbara", "Richard", "Susan", "Joseph", "Jessica")
  private val lastNames = Array("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
    "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas")

  val employeeDataFeeder: Iterator[Map[String, Any]] = Iterator.continually {
    val firstName = firstNames(Random.nextInt(firstNames.length))
    val lastName = lastNames(Random.nextInt(lastNames.length))
    val age = 22 + Random.nextInt(43)
    val dept = departments(Random.nextInt(departments.length))
    val email = s"${firstName.toLowerCase}.${lastName.toLowerCase}.${Random.nextInt(10000)}@example.com"
    val employeeId = UUID.randomUUID().toString
    val policyId = UUID.randomUUID().toString
    val idempotencyKey = s"perf-${UUID.randomUUID()}"

    Map(
      "employeeId" -> employeeId,
      "policyId" -> policyId,
      "idempotencyKey" -> idempotencyKey,
      "employeeName" -> s"$firstName $lastName",
      "employeeAge" -> age,
      "employeeDept" -> dept,
      "employeeEmail" -> email
    )
  }

  val premiumFeeder: Iterator[Map[String, Any]] = Iterator.continually {
    val premium = BigDecimal(100 + Random.nextDouble() * 4900).setScale(2, BigDecimal.RoundingMode.HALF_UP)
    Map("premiumAmount" -> premium.toString())
  }

  val coverageDateFeeder: Iterator[Map[String, Any]] = Iterator.continually {
    val daysOffset = 1 + Random.nextInt(30)
    val startDate = LocalDate.now().plusDays(daysOffset)
    val endDate = startDate.plusDays(365)
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    Map(
      "coverageStartDate" -> startDate.format(formatter),
      "coverageEndDate" -> endDate.format(formatter)
    )
  }
}
