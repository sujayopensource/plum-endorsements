package com.plum.endorsements.application.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${endorsement.retention.days:365}")
    private int retentionDays;

    @Value("${endorsement.retention.batch-size:1000}")
    private int batchSize;

    @Scheduled(cron = "${endorsement.retention.archive-cron:0 0 3 * * SUN}")
    @SchedulerLock(name = "dataRetention", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    @Transactional
    public void archiveOldEndorsements() {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            log.info("Starting data retention archival (retentionDays={}, cutoff={})", retentionDays, cutoff);

            // Archive events for terminal endorsements older than cutoff
            int eventsArchived = jdbcTemplate.update(
                    "INSERT INTO endorsement_events_archive SELECT ee.* FROM endorsement_events ee "
                    + "JOIN endorsements e ON ee.endorsement_id = e.id "
                    + "WHERE e.status IN ('CONFIRMED', 'REJECTED', 'CANCELLED') "
                    + "AND e.updated_at < ? "
                    + "AND NOT EXISTS (SELECT 1 FROM endorsement_events_archive a WHERE a.id = ee.id) "
                    + "LIMIT ?",
                    cutoff, batchSize * 5);

            // Archive terminal endorsements older than cutoff
            int endorsementsArchived = jdbcTemplate.update(
                    "INSERT INTO endorsements_archive SELECT * FROM endorsements "
                    + "WHERE status IN ('CONFIRMED', 'REJECTED', 'CANCELLED') "
                    + "AND updated_at < ? "
                    + "AND NOT EXISTS (SELECT 1 FROM endorsements_archive a WHERE a.id = endorsements.id) "
                    + "LIMIT ?",
                    cutoff, batchSize);

            // Delete archived events
            int eventsDeleted = 0;
            if (eventsArchived > 0) {
                eventsDeleted = jdbcTemplate.update(
                        "DELETE FROM endorsement_events WHERE id IN ("
                        + "SELECT ee.id FROM endorsement_events ee "
                        + "JOIN endorsement_events_archive a ON ee.id = a.id "
                        + "LIMIT ?)",
                        batchSize * 5);
            }

            // Delete archived endorsements (only if their events are already archived)
            int endorsementsDeleted = 0;
            if (endorsementsArchived > 0) {
                endorsementsDeleted = jdbcTemplate.update(
                        "DELETE FROM endorsements WHERE id IN ("
                        + "SELECT e.id FROM endorsements e "
                        + "JOIN endorsements_archive a ON e.id = a.id "
                        + "WHERE NOT EXISTS (SELECT 1 FROM endorsement_events ee WHERE ee.endorsement_id = e.id) "
                        + "LIMIT ?)",
                        batchSize);
            }

            meterRegistry.counter("endorsement.archive.count", "type", "endorsement")
                    .increment(endorsementsArchived);
            meterRegistry.counter("endorsement.archive.count", "type", "event")
                    .increment(eventsArchived);

            log.info("Data retention completed: archived {} endorsements ({} deleted), {} events ({} deleted)",
                    endorsementsArchived, endorsementsDeleted, eventsArchived, eventsDeleted);
        } catch (Exception e) {
            result = "failure";
            log.error("Data retention archival failed", e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("endorsement.scheduler.duration",
                    "scheduler", "data_retention", "result", result));
            meterRegistry.counter("endorsement.scheduler.execution",
                    "scheduler", "data_retention", "result", result).increment();
        }
    }
}
