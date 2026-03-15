package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.model.ProcessMiningMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EventStreamAnalyzer")
class EventStreamAnalyzerTest {

    private EventStreamAnalyzer analyzer;
    private UUID insurerId;

    @BeforeEach
    void setUp() {
        analyzer = new EventStreamAnalyzer();
        insurerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("calculates transition durations between events")
    void analyzeWorkflow_CalculatesTransitionDurations() {
        UUID endorsementId = UUID.randomUUID();
        UUID employerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Instant t0 = Instant.now().minus(10, ChronoUnit.HOURS);
        Instant t1 = t0.plus(1, ChronoUnit.HOURS);
        Instant t2 = t1.plus(2, ChronoUnit.HOURS);

        List<EndorsementEvent> events = List.of(
                new EndorsementEvent.Created(endorsementId, t0, employerId, employeeId, EndorsementType.ADD),
                new EndorsementEvent.Validated(endorsementId, t1, employerId),
                new EndorsementEvent.Confirmed(endorsementId, t2, employerId, "REF-123")
        );

        List<ProcessMiningMetric> metrics = analyzer.analyzeWorkflow(events, insurerId);

        assertThat(metrics).isNotEmpty();
        assertThat(metrics).allSatisfy(metric -> {
            assertThat(metric.getInsurerId()).isEqualTo(insurerId);
            assertThat(metric.getAvgDurationMs()).isGreaterThanOrEqualTo(0);
            assertThat(metric.getSampleCount()).isGreaterThan(0);
        });

        // Should have 2 transitions: Created->Validated, Validated->Confirmed
        assertThat(metrics).hasSize(2);
    }

    @Test
    @DisplayName("identifies happy path when no retries or rejections")
    void analyzeWorkflow_HappyPath_100Percent() {
        UUID endorsementId = UUID.randomUUID();
        UUID employerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Instant t0 = Instant.now().minus(5, ChronoUnit.HOURS);

        List<EndorsementEvent> events = List.of(
                new EndorsementEvent.Created(endorsementId, t0, employerId, employeeId, EndorsementType.ADD),
                new EndorsementEvent.Confirmed(endorsementId, t0.plus(1, ChronoUnit.HOURS), employerId, "REF-456")
        );

        List<ProcessMiningMetric> metrics = analyzer.analyzeWorkflow(events, insurerId);

        assertThat(metrics).isNotEmpty();
        // All endorsements are happy path (no RETRY or REJECTED events)
        assertThat(metrics.get(0).getHappyPathPct())
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("identifies non-happy path with rejection events")
    void analyzeWorkflow_WithRejection_LowerHappyPathPct() {
        UUID endorsementId1 = UUID.randomUUID();
        UUID endorsementId2 = UUID.randomUUID();
        UUID employerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Instant t0 = Instant.now().minus(10, ChronoUnit.HOURS);

        List<EndorsementEvent> events = new ArrayList<>();

        // Endorsement 1: happy path
        events.add(new EndorsementEvent.Created(endorsementId1, t0, employerId, employeeId, EndorsementType.ADD));
        events.add(new EndorsementEvent.Confirmed(endorsementId1, t0.plus(1, ChronoUnit.HOURS), employerId, "REF-1"));

        // Endorsement 2: rejected (not happy path)
        events.add(new EndorsementEvent.Created(endorsementId2, t0, employerId, employeeId, EndorsementType.ADD));
        events.add(new EndorsementEvent.Rejected(endorsementId2, t0.plus(2, ChronoUnit.HOURS), employerId, "Invalid data"));

        List<ProcessMiningMetric> metrics = analyzer.analyzeWorkflow(events, insurerId);

        assertThat(metrics).isNotEmpty();
        // 1 of 2 endorsements is happy path = 50%
        assertThat(metrics.get(0).getHappyPathPct())
                .isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("handles empty events list gracefully")
    void analyzeWorkflow_EmptyEvents_ReturnsEmptyMetrics() {
        List<EndorsementEvent> events = List.of();

        List<ProcessMiningMetric> metrics = analyzer.analyzeWorkflow(events, insurerId);

        assertThat(metrics).isEmpty();
    }

    @Test
    @DisplayName("groups events by endorsement ID for separate timelines")
    void analyzeWorkflow_GroupsByEndorsement() {
        UUID endorsementId1 = UUID.randomUUID();
        UUID endorsementId2 = UUID.randomUUID();
        UUID employerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Instant t0 = Instant.now().minus(10, ChronoUnit.HOURS);

        List<EndorsementEvent> events = new ArrayList<>();

        // Two separate endorsement timelines
        events.add(new EndorsementEvent.Created(endorsementId1, t0, employerId, employeeId, EndorsementType.ADD));
        events.add(new EndorsementEvent.Validated(endorsementId1, t0.plus(30, ChronoUnit.MINUTES), employerId));
        events.add(new EndorsementEvent.Created(endorsementId2, t0, employerId, employeeId, EndorsementType.DELETE));
        events.add(new EndorsementEvent.Validated(endorsementId2, t0.plus(45, ChronoUnit.MINUTES), employerId));

        List<ProcessMiningMetric> metrics = analyzer.analyzeWorkflow(events, insurerId);

        // Both endorsements have Created->Validated, so should be 1 unique transition type with count 2
        assertThat(metrics).hasSize(1);
        ProcessMiningMetric createdToValidated = metrics.get(0);
        assertThat(createdToValidated.getSampleCount()).isEqualTo(2);
        assertThat(createdToValidated.getFromStatus()).isEqualTo("ENDORSEMENT_CREATED");
        assertThat(createdToValidated.getToStatus()).isEqualTo("ENDORSEMENT_VALIDATED");
    }

    @Test
    @DisplayName("single event endorsement produces no transition metrics")
    void analyzeWorkflow_SingleEvent_NoTransitions() {
        UUID endorsementId = UUID.randomUUID();
        UUID employerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        List<EndorsementEvent> events = List.of(
                new EndorsementEvent.Created(endorsementId, Instant.now(), employerId, employeeId, EndorsementType.ADD)
        );

        List<ProcessMiningMetric> metrics = analyzer.analyzeWorkflow(events, insurerId);

        // Single event, no transitions to calculate
        assertThat(metrics).isEmpty();
    }
}
