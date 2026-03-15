package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.ProcessMiningMetric;

import java.util.List;
import java.util.UUID;

public interface ProcessMiningPort {

    List<ProcessMiningMetric> analyzeWorkflow(List<EndorsementEvent> events, UUID insurerId);
}
