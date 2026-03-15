package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.ReconciliationItem;
import com.plum.endorsements.domain.model.ReconciliationOutcome;
import com.plum.endorsements.domain.model.ReconciliationRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRepository {
    ReconciliationRun saveRun(ReconciliationRun run);
    ReconciliationItem saveItem(ReconciliationItem item);
    Optional<ReconciliationRun> findRunById(UUID runId);
    List<ReconciliationRun> findRunsByInsurerId(UUID insurerId);
    List<ReconciliationItem> findItemsByRunId(UUID runId);
    List<ReconciliationItem> findItemsByOutcome(UUID runId, ReconciliationOutcome outcome);
}
