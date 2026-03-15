package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.ProvisionalCoverage;
import java.util.*;

public interface ProvisionalCoverageRepository {
    ProvisionalCoverage save(ProvisionalCoverage coverage);
    Optional<ProvisionalCoverage> findByEndorsementId(UUID endorsementId);
    List<ProvisionalCoverage> findActiveByEmployeeId(UUID employeeId);
    List<ProvisionalCoverage> findStaleProvisionalCoverages(int maxDays);
    List<ProvisionalCoverage> findActiveExpiringBefore(java.time.Instant warningCutoff, java.time.Instant staleCutoff);
}
