package com.plum.endorsements.apitest.base;

import com.plum.endorsements.EndorsementApplication;
import com.plum.endorsements.application.scheduler.BatchAssemblyScheduler;
import com.plum.endorsements.application.scheduler.BatchStatusPollerScheduler;
import com.plum.endorsements.application.scheduler.ProvisionalCoverageCleanupScheduler;
import com.plum.endorsements.application.scheduler.DataRetentionScheduler;
import com.plum.endorsements.application.scheduler.ReconciliationScheduler;
import com.redis.testcontainers.RedisContainer;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(
        classes = EndorsementApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("apitest")
public abstract class BaseApiTest {

    // ── Testcontainer singletons (shared across all test classes) ──

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
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Redis
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // Kafka
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

    // ── Injected dependencies ──

    @LocalServerPort
    int port;

    @Autowired
    protected JdbcTemplate jdbc;

    // ── Common UUIDs for test data ──

    protected static final UUID EMPLOYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    protected static final UUID EMPLOYEE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    protected static final UUID INSURER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    protected static final UUID POLICY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    // ── Multi-insurer IDs (matching V7 migration seeds) ──

    protected static final UUID ICICI_INSURER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    protected static final UUID NIVA_INSURER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    protected static final UUID BAJAJ_INSURER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.filters(new AllureRestAssured());

        cleanDatabase();
    }

    // ── Database cleanup (FK-safe order) ──

    protected void cleanDatabase() {
        // Phase 4 tables
        jdbc.execute("DELETE FROM audit_logs");
        // Archive tables
        jdbc.execute("DELETE FROM endorsement_events_archive");
        jdbc.execute("DELETE FROM endorsements_archive");
        // Phase 3: Intelligence tables (reference endorsements via FK)
        jdbc.execute("DELETE FROM stp_rate_snapshots");
        jdbc.execute("DELETE FROM anomaly_detections");
        jdbc.execute("DELETE FROM balance_forecasts");
        jdbc.execute("DELETE FROM error_resolutions");
        jdbc.execute("DELETE FROM process_mining_metrics");
        // Phase 1-2 tables
        jdbc.execute("DELETE FROM reconciliation_items");
        jdbc.execute("DELETE FROM reconciliation_runs");
        jdbc.execute("DELETE FROM endorsement_events");
        jdbc.execute("DELETE FROM ea_transactions");
        jdbc.execute("DELETE FROM provisional_coverages");
        jdbc.execute("DELETE FROM endorsements");
        jdbc.execute("DELETE FROM endorsement_batches");
        jdbc.execute("DELETE FROM ea_accounts");
    }

    // ── Helper: Seed an EA account directly via JDBC ──

    protected void seedEAAccount(UUID employerId, UUID insurerId, BigDecimal balance) {
        jdbc.update(
                "INSERT INTO ea_accounts (employer_id, insurer_id, balance, reserved, updated_at) VALUES (?, ?, ?, 0, now())",
                employerId, insurerId, balance
        );
    }

    // ── Helper: Build a standard CreateEndorsementRequest body ──

    protected Map<String, Object> createEndorsementRequest(String type, BigDecimal premiumAmount) {
        return createEndorsementRequest(EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID, type,
                LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), premiumAmount, null);
    }

    protected Map<String, Object> createEndorsementRequest(
            UUID employerId, UUID employeeId, UUID insurerId, UUID policyId,
            String type, LocalDate coverageStart, LocalDate coverageEnd,
            BigDecimal premiumAmount, String idempotencyKey) {

        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("employerId", employerId.toString());
        request.put("employeeId", employeeId.toString());
        request.put("insurerId", insurerId.toString());
        request.put("policyId", policyId.toString());
        request.put("type", type);
        request.put("coverageStartDate", coverageStart.toString());
        if (coverageEnd != null) {
            request.put("coverageEndDate", coverageEnd.toString());
        }
        request.put("employeeData", Map.of(
                "name", "Test Employee",
                "dob", "1990-05-15",
                "gender", "M"
        ));
        if (premiumAmount != null) {
            request.put("premiumAmount", premiumAmount);
        }
        if (idempotencyKey != null) {
            request.put("idempotencyKey", idempotencyKey);
        }
        return request;
    }

    // ── Helper: Create an endorsement via the API and return the response ──

    protected ValidatableResponse createEndorsementViaApi(Map<String, Object> request) {
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements")
                .then();
    }

    // ── Helper: Create an endorsement and extract the generated ID ──

    protected String createEndorsementAndGetId(String type, BigDecimal premiumAmount) {
        Map<String, Object> request = createEndorsementRequest(type, premiumAmount);
        return createEndorsementViaApi(request)
                .statusCode(201)
                .extract()
                .path("id");
    }

    // ── Helper: Seed an endorsement at a specific status via JDBC ──

    protected UUID seedEndorsementAtStatus(String status, int retryCount) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO endorsements (id, employer_id, employee_id, insurer_id, policy_id,
                    type, status, coverage_start_date, employee_data, premium_amount,
                    idempotency_key, retry_count, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, 'ADD', ?, '2026-04-01', '{"name":"Test Employee","dob":"1990-05-15","gender":"M"}'::jsonb, 1000.00, ?, ?, now(), now(), 0)
                """,
                id, EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                status, "key-" + id, retryCount
        );
        return id;
    }
}
