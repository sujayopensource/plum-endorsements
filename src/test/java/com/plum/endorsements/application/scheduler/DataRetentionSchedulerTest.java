package com.plum.endorsements.application.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataRetentionScheduler")
class DataRetentionSchedulerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;

    private DataRetentionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DataRetentionScheduler(jdbcTemplate, meterRegistry);
        ReflectionTestUtils.setField(scheduler, "retentionDays", 365);
        ReflectionTestUtils.setField(scheduler, "batchSize", 1000);
    }

    @Test
    @DisplayName("archives old terminal endorsements and their events")
    void archiveOldEndorsements_ArchivesTerminalEndorsements() {
        // Simulate archiving: events first, then endorsements
        when(jdbcTemplate.update(contains("endorsement_events_archive"), any(Object[].class)))
                .thenReturn(50);
        when(jdbcTemplate.update(contains("endorsements_archive"), any(Object[].class)))
                .thenReturn(10);
        when(jdbcTemplate.update(contains("DELETE FROM endorsement_events"), any(Object[].class)))
                .thenReturn(50);
        when(jdbcTemplate.update(contains("DELETE FROM endorsements"), any(Object[].class)))
                .thenReturn(10);

        scheduler.archiveOldEndorsements();

        // Verify archive inserts happened
        verify(jdbcTemplate, atLeast(2)).update(contains("INSERT INTO"), any(Object[].class));
        // Verify deletions happened
        verify(jdbcTemplate, atLeast(2)).update(contains("DELETE FROM"), any(Object[].class));
    }

    @Test
    @DisplayName("skips delete when nothing was archived")
    void archiveOldEndorsements_NothingToArchive_SkipsDelete() {
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenReturn(0);

        scheduler.archiveOldEndorsements();

        // Only the two INSERT queries should run; no DELETEs
        verify(jdbcTemplate, times(2)).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("records metrics for archived count")
    void archiveOldEndorsements_RecordsMetrics() {
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenReturn(0);

        scheduler.archiveOldEndorsements();

        verify(meterRegistry, atLeastOnce()).counter(eq("endorsement.archive.count"),
                eq("type"), anyString());
    }
}
