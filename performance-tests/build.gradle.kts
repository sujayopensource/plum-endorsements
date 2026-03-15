plugins {
    scala
    id("io.gatling.gradle") version "3.10.5"
}

group = "com.plum"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    gatling("io.gatling.highcharts:gatling-charts-highcharts:3.10.5")
    gatling("org.postgresql:postgresql:42.7.3")
}

gatling {
    jvmArgs = listOf("-Xms512m", "-Xmx2g")

    systemProperties = mapOf(
        "baseUrl" to (System.getProperty("baseUrl") ?: "http://localhost:8080"),
        "dbUrl" to (System.getProperty("dbUrl") ?: "jdbc:postgresql://localhost:5432/endorsements"),
        "dbUser" to (System.getProperty("dbUser") ?: "plum"),
        "dbPassword" to (System.getProperty("dbPassword") ?: "plum_dev"),
        "durationMinutes" to (System.getProperty("durationMinutes") ?: "15"),
        "targetRps" to (System.getProperty("targetRps") ?: "20"),
        "rampDurationSeconds" to (System.getProperty("rampDurationSeconds") ?: "60"),
        "thinkTimeMs" to (System.getProperty("thinkTimeMs") ?: "1000")
    )
}

// Custom task that runs a single Gatling simulation, passing the class via
// -Dgatling.core.simulationClass which Gatling reads from its HOCON config.
tasks.register<JavaExec>("gatlingRunSimulation") {
    dependsOn("gatlingClasses")
    classpath = sourceSets["gatling"].runtimeClasspath
    mainClass.set("io.gatling.app.Gatling")

    val simClass = System.getProperty("gatling.simulation",
        "com.plum.endorsements.perf.simulations.BaselineSimulation")
    val buildDir = layout.buildDirectory.get()

    args = listOf(
        "--results-folder", "$buildDir/reports/gatling",
        "--binaries-folder", "$buildDir/classes/scala/gatling"
    )

    jvmArgs(
        "-Xms512m", "-Xmx2g",
        "-Dgatling.core.simulationClass=$simClass"
    )

    // Forward system properties to the Gatling process
    val props = listOf("baseUrl", "dbUrl", "dbUser", "dbPassword",
        "durationMinutes", "targetRps", "rampDurationSeconds", "thinkTimeMs")
    props.forEach { prop ->
        val value = System.getProperty(prop)
        if (value != null) {
            jvmArgs("-D$prop=$value")
        }
    }
}
