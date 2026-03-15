package com.plum.endorsements.infrastructure.insurer.nivabupa;

import com.plum.endorsements.domain.port.InsurerPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NivaBupaAdapter implements InsurerPort {

    private final MeterRegistry meterRegistry;
    private final NivaBupaCsvMapper csvMapper;

    @Override
    public SubmissionResult submitRealTime(UUID endorsementId, Map<String, Object> endorsementData) {
        throw new UnsupportedOperationException("Niva Bupa does not support real-time submissions. Use batch mode.");
    }

    @Override
    public String submitBatch(UUID batchId, List<Map<String, Object>> endorsements) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("Niva Bupa: submitting batch {} with {} endorsements via CSV", batchId, endorsements.size());

        String csvPayload = csvMapper.toCsvBatch(endorsements);
        log.debug("Niva Bupa: CSV payload size: {} bytes", csvPayload.length());

        try {
            // Simulated SFTP upload latency
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Niva Bupa batch submission interrupted for batch {}", batchId);
        }

        String batchReference = "NIVA-BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Niva Bupa: batch {} uploaded with reference {}", batchId, batchReference);

        sample.stop(meterRegistry.timer("endorsement.insurer.nivabupa.duration", "method", "submitBatch"));
        return batchReference;
    }

    @Override
    public BatchStatusResult checkBatchStatus(String insurerBatchRef) {
        log.info("Niva Bupa: checking batch status for reference {}", insurerBatchRef);

        try {
            // Simulated SFTP download + parse latency
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return new BatchStatusResult("COMPLETED", List.of());
    }

    @Override
    public InsurerCapabilities getCapabilities() {
        return new InsurerCapabilities(false, true, 500, 24, 0);
    }

    @Override
    public String getAdapterType() {
        return "NIVA_BUPA";
    }

    @Override
    public Map<String, Object> mapToInsurerFormat(Map<String, Object> endorsementData) {
        return endorsementData; // CSV mapping handled in batch submission
    }

    @Override
    public Map<String, Object> mapFromInsurerFormat(Map<String, Object> insurerData) {
        return insurerData;
    }
}
