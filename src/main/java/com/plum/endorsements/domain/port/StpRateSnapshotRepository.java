package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.StpRateSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StpRateSnapshotRepository {
    StpRateSnapshot save(StpRateSnapshot snapshot);
    List<StpRateSnapshot> findByInsurerIdAndDateRange(UUID insurerId, LocalDate from, LocalDate to);
    Optional<StpRateSnapshot> findByInsurerIdAndDate(UUID insurerId, LocalDate date);
}
