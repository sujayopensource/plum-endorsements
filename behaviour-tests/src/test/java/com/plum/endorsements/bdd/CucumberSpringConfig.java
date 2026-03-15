package com.plum.endorsements.bdd;

import com.plum.endorsements.EndorsementApplication;
import com.plum.endorsements.application.scheduler.BatchAssemblyScheduler;
import com.plum.endorsements.application.scheduler.BatchStatusPollerScheduler;
import com.plum.endorsements.application.scheduler.ProvisionalCoverageCleanupScheduler;
import com.plum.endorsements.application.scheduler.DataRetentionScheduler;
import com.plum.endorsements.application.scheduler.ReconciliationScheduler;
import com.plum.endorsements.bdd.support.DatabaseHelper;
import com.plum.endorsements.bdd.support.TestContext;
import com.redis.testcontainers.RedisContainer;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@CucumberContextConfiguration
@SpringBootTest(
        classes = EndorsementApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("bdd")
public class CucumberSpringConfig {

    // ── Testcontainer singletons (shared across all scenarios) ──

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("endorsements_test")
                    .withUsername("test")
                    .withPassword("test");

    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    static {
        POSTGRES.start();
        REDIS.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    // ── Disable schedulers to prevent background state changes ──

    @MockitoBean
    BatchAssemblyScheduler batchAssemblyScheduler;

    @MockitoBean
    BatchStatusPollerScheduler batchStatusPollerScheduler;

    @MockitoBean
    ProvisionalCoverageCleanupScheduler coverageCleanupScheduler;

    @MockitoBean
    ReconciliationScheduler reconciliationScheduler;

    @MockitoBean
    DataRetentionScheduler dataRetentionScheduler;

    @LocalServerPort
    int port;

    @Autowired
    DatabaseHelper databaseHelper;

    @Autowired
    TestContext testContext;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.filters(new AllureRestAssured());
        databaseHelper.cleanDatabase();
        testContext.reset();
    }
}
