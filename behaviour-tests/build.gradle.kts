plugins {
    java
    id("io.spring.dependency-management") version "1.1.7"
    id("io.qameta.allure") version "2.11.2"
}

group = "com.plum"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.3")
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
    }
}

dependencies {
    // Depend on the root project (main application classes, configs, entities)
    testImplementation(project(":"))

    // Cucumber BDD
    testImplementation("io.cucumber:cucumber-java:7.15.0")
    testImplementation("io.cucumber:cucumber-spring:7.15.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.15.0")

    // JUnit Platform Suite (required runner for Cucumber 7.x)
    testImplementation("org.junit.platform:junit-platform-suite")

    // Spring Boot test starter
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // RestAssured for REST API testing
    testImplementation("io.rest-assured:rest-assured:5.4.0")

    // Allure reporting (cucumber7-jvm maps Feature/Scenario to Allure labels)
    testImplementation("io.qameta.allure:allure-cucumber7-jvm:2.25.0")
    testImplementation("io.qameta.allure:allure-junit5:2.25.0")
    testImplementation("io.qameta.allure:allure-rest-assured:2.25.0")

    // Testcontainers
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.testcontainers:kafka:1.19.8")
    testImplementation("com.redis:testcontainers-redis:2.2.2")

    // PostgreSQL driver (needed at test runtime for Testcontainers)
    testRuntimeOnly("org.postgresql:postgresql")

    // Kafka test support
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // Spring Security test
    testImplementation("org.springframework.security:spring-security-test")

    // JDBC for data seeding
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")

    systemProperty("cucumber.plugin", listOf(
        "pretty",
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
        "html:${layout.buildDirectory.get()}/reports/cucumber/cucumber-report.html",
        "json:${layout.buildDirectory.get()}/reports/cucumber/cucumber-report.json"
    ).joinToString(","))

    systemProperty("allure.results.directory", "${layout.buildDirectory.get()}/allure-results")
}

allure {
    version.set("2.25.0")
    adapter {
        autoconfigure.set(true)
        aspectjWeaver.set(true)
    }
}
