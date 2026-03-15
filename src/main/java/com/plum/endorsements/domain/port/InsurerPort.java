package com.plum.endorsements.domain.port;

import java.math.BigDecimal;
import java.util.*;

public interface InsurerPort {
    SubmissionResult submitRealTime(UUID endorsementId, Map<String, Object> endorsementData);
    String submitBatch(UUID batchId, List<Map<String, Object>> endorsements);
    BatchStatusResult checkBatchStatus(String insurerBatchRef);
    InsurerCapabilities getCapabilities();

    default String getAdapterType() {
        return "MOCK";
    }

    default Map<String, Object> mapToInsurerFormat(Map<String, Object> endorsementData) {
        return endorsementData;
    }

    default Map<String, Object> mapFromInsurerFormat(Map<String, Object> insurerData) {
        return insurerData;
    }

    record SubmissionResult(boolean success, String insurerReference, String errorMessage) {}
    record BatchStatusResult(String status, List<EndorsementResult> results) {}
    record EndorsementResult(UUID endorsementId, boolean confirmed, String insurerReference, String rejectionReason) {}
    record InsurerCapabilities(boolean supportsRealTime, boolean supportsBatch, int maxBatchSize, long batchSlaHours, int rateLimitPerMinute) {}
}
