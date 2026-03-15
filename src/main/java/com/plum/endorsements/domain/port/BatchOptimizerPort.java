package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.EAAccount;
import com.plum.endorsements.domain.model.Endorsement;

import java.math.BigDecimal;
import java.util.List;

public interface BatchOptimizerPort {

    OptimizedBatchPlan optimizeBatch(List<Endorsement> queue, EAAccount account,
                                     InsurerPort.InsurerCapabilities capabilities);

    record OptimizedBatchPlan(List<Endorsement> endorsements, String strategy,
                              BigDecimal estimatedSavings, long estimatedProcessingTimeMs) {}
}
