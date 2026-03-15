package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.model.ProvisionalCoverage;
import com.plum.endorsements.domain.port.EndorsementRepository;
import com.plum.endorsements.domain.port.EventPublisher;
import com.plum.endorsements.domain.port.NotificationPort;
import com.plum.endorsements.domain.port.ProvisionalCoverageRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProvisionalCoverageCleanupScheduler")
class ProvisionalCoverageCleanupSchedulerTest {

    @Mock private ProvisionalCoverageRepository provisionalCoverageRepository;
    @Mock private EndorsementRepository endorsementRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private NotificationPort notificationPort;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private ProvisionalCoverageCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ProvisionalCoverageCleanupScheduler(
                provisionalCoverageRepository, endorsementRepository,
                eventPublisher, notificationPort, meterRegistry);
        ReflectionTestUtils.setField(scheduler, "maxDays", 30);
        ReflectionTestUtils.setField(scheduler, "warningDaysBeforeExpiry", 2);

        // Default: no coverages approaching expiry
        lenient().when(provisionalCoverageRepository.findActiveExpiringBefore(any(), any()))
                .thenReturn(List.of());
    }

    private ProvisionalCoverage buildCoverage(UUID endorsementId) {
        return ProvisionalCoverage.builder()
                .id(UUID.randomUUID())
                .endorsementId(endorsementId)
                .employeeId(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .coverageStart(LocalDate.now().minusDays(35))
                .createdAt(Instant.now().minusSeconds(31 * 86400))
                .build();
    }

    private Endorsement buildEndorsement(UUID id, EndorsementStatus status) {
        return Endorsement.builder()
                .id(id)
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("expires coverage for terminal endorsements and publishes events")
    void expireStaleProvisionalCoverages_TerminalEndorsement_ExpiresAndPublishesEvent() {
        UUID endorsementId = UUID.randomUUID();
        ProvisionalCoverage coverage = buildCoverage(endorsementId);
        Endorsement endorsement = buildEndorsement(endorsementId, EndorsementStatus.FAILED_PERMANENT);

        when(provisionalCoverageRepository.findStaleProvisionalCoverages(30))
                .thenReturn(List.of(coverage));
        when(endorsementRepository.findById(endorsementId))
                .thenReturn(Optional.of(endorsement));

        scheduler.expireStaleProvisionalCoverages();

        verify(provisionalCoverageRepository).save(coverage);
        assertThat(coverage.getExpiredAt()).isNotNull();
        verify(eventPublisher).publish(any(EndorsementEvent.ProvisionalCoverageExpired.class));
        verify(notificationPort).notifyCoverageExpired(
                eq(coverage.getEmployerId()), eq(coverage.getEmployeeId()), any());
    }

    @Test
    @DisplayName("skips coverage expiry when endorsement is still actively processing")
    void expireStaleProvisionalCoverages_ActiveEndorsement_SkipsExpiry() {
        UUID endorsementId = UUID.randomUUID();
        ProvisionalCoverage coverage = buildCoverage(endorsementId);
        Endorsement endorsement = buildEndorsement(endorsementId, EndorsementStatus.INSURER_PROCESSING);

        when(provisionalCoverageRepository.findStaleProvisionalCoverages(30))
                .thenReturn(List.of(coverage));
        when(endorsementRepository.findById(endorsementId))
                .thenReturn(Optional.of(endorsement));

        scheduler.expireStaleProvisionalCoverages();

        verify(provisionalCoverageRepository, never()).save(any());
        assertThat(coverage.getExpiredAt()).isNull();
        verify(eventPublisher, never()).publish(any());
        verify(notificationPort, never()).notifyCoverageExpired(any(), any(), any());
    }

    @Test
    @DisplayName("expires coverage when endorsement no longer exists in database")
    void expireStaleProvisionalCoverages_OrphanedCoverage_Expires() {
        UUID endorsementId = UUID.randomUUID();
        ProvisionalCoverage coverage = buildCoverage(endorsementId);

        when(provisionalCoverageRepository.findStaleProvisionalCoverages(30))
                .thenReturn(List.of(coverage));
        when(endorsementRepository.findById(endorsementId))
                .thenReturn(Optional.empty());

        scheduler.expireStaleProvisionalCoverages();

        verify(provisionalCoverageRepository).save(coverage);
        assertThat(coverage.getExpiredAt()).isNotNull();
        verify(eventPublisher).publish(any(EndorsementEvent.ProvisionalCoverageExpired.class));
    }

    @Test
    @DisplayName("does nothing when no stale coverages exist")
    void expireStaleProvisionalCoverages_NoStale_DoesNothing() {
        when(provisionalCoverageRepository.findStaleProvisionalCoverages(30))
                .thenReturn(List.of());

        scheduler.expireStaleProvisionalCoverages();

        verify(provisionalCoverageRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("warns about coverages approaching expiry before expiring stale ones")
    void expireStaleProvisionalCoverages_WarnsAboutExpiringCoverages() {
        UUID endorsementId = UUID.randomUUID();
        ProvisionalCoverage expiringCoverage = ProvisionalCoverage.builder()
                .id(UUID.randomUUID())
                .endorsementId(endorsementId)
                .employeeId(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .coverageStart(LocalDate.now().minusDays(29))
                .createdAt(Instant.now().minusSeconds(29 * 86400)) // 29 days old — within warning window
                .build();

        when(provisionalCoverageRepository.findActiveExpiringBefore(any(), any()))
                .thenReturn(List.of(expiringCoverage));
        when(provisionalCoverageRepository.findStaleProvisionalCoverages(30))
                .thenReturn(List.of());

        scheduler.expireStaleProvisionalCoverages();

        verify(notificationPort).notifyCoverageAtRisk(
                eq(expiringCoverage.getEmployerId()),
                eq(expiringCoverage.getEndorsementId()),
                contains("will expire in"));
    }

    @Test
    @DisplayName("does not warn for recently created coverages")
    void expireStaleProvisionalCoverages_DoesNotWarnForFreshCoverages() {
        when(provisionalCoverageRepository.findActiveExpiringBefore(any(), any()))
                .thenReturn(List.of());
        when(provisionalCoverageRepository.findStaleProvisionalCoverages(30))
                .thenReturn(List.of());

        scheduler.expireStaleProvisionalCoverages();

        verify(notificationPort, never()).notifyCoverageAtRisk(any(), any(), any());
    }

    @Test
    @DisplayName("handles mix of terminal and active endorsements correctly")
    void expireStaleProvisionalCoverages_MixedStatuses_ExpiresOnlyTerminal() {
        UUID terminalEndId = UUID.randomUUID();
        UUID activeEndId = UUID.randomUUID();
        ProvisionalCoverage terminalCov = buildCoverage(terminalEndId);
        ProvisionalCoverage activeCov = buildCoverage(activeEndId);

        Endorsement terminalEnd = buildEndorsement(terminalEndId, EndorsementStatus.FAILED_PERMANENT);
        Endorsement activeEnd = buildEndorsement(activeEndId, EndorsementStatus.BATCH_SUBMITTED);

        when(provisionalCoverageRepository.findStaleProvisionalCoverages(30))
                .thenReturn(List.of(terminalCov, activeCov));
        when(endorsementRepository.findById(terminalEndId))
                .thenReturn(Optional.of(terminalEnd));
        when(endorsementRepository.findById(activeEndId))
                .thenReturn(Optional.of(activeEnd));

        scheduler.expireStaleProvisionalCoverages();

        verify(provisionalCoverageRepository).save(terminalCov);
        verify(provisionalCoverageRepository, never()).save(activeCov);
        assertThat(terminalCov.getExpiredAt()).isNotNull();
        assertThat(activeCov.getExpiredAt()).isNull();
        verify(eventPublisher, times(1)).publish(any(EndorsementEvent.ProvisionalCoverageExpired.class));
    }
}
