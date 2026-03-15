package com.plum.endorsements.infrastructure.insurer.bajaj;

import com.plum.endorsements.domain.port.InsurerPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
public class BajajAllianzAdapter implements InsurerPort {

    private final MeterRegistry meterRegistry;
    private final BajajAllianzXmlMapper xmlMapper;

    @Override
    @CircuitBreaker(name = "bajajAllianz", fallbackMethod = "submitRealTimeFallback")
    @Retry(name = "bajajAllianz")
    public SubmissionResult submitRealTime(UUID endorsementId, Map<String, Object> endorsementData) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("Bajaj Allianz: submitting endorsement {} via SOAP/XML", endorsementId);

        String xmlPayload = xmlMapper.toXmlEnvelope(endorsementData);
        log.debug("Bajaj Allianz: XML payload size: {} bytes", xmlPayload.length());

        try {
            // Simulated SOAP API call latency (slower than REST)
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Bajaj Allianz submission interrupted for endorsement {}", endorsementId);
        }

        String insurerReference = "BAJAJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Bajaj Allianz: endorsement {} accepted with reference {}", endorsementId, insurerReference);

        sample.stop(meterRegistry.timer("endorsement.insurer.bajaj.duration", "method", "submitRealTime"));
        return new SubmissionResult(true, insurerReference, null);
    }

    private SubmissionResult submitRealTimeFallback(UUID endorsementId, Map<String, Object> endorsementData, Throwable t) {
        log.warn("Bajaj Allianz circuit breaker fallback for endorsement {}: {}", endorsementId, t.getMessage());
        return new SubmissionResult(false, null, "Bajaj Allianz service unavailable: " + t.getMessage());
    }

    @Override
    public String submitBatch(UUID batchId, List<Map<String, Object>> endorsements) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("Bajaj Allianz: submitting batch {} with {} endorsements via SOAP/XML", batchId, endorsements.size());

        try {
            // Simulated batch SOAP call latency
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Bajaj Allianz batch submission interrupted for batch {}", batchId);
        }

        String batchReference = "BAJAJ-BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Bajaj Allianz: batch {} submitted with reference {}", batchId, batchReference);

        sample.stop(meterRegistry.timer("endorsement.insurer.bajaj.duration", "method", "submitBatch"));
        return batchReference;
    }

    @Override
    public BatchStatusResult checkBatchStatus(String insurerBatchRef) {
        log.info("Bajaj Allianz: checking batch status for reference {}", insurerBatchRef);

        try {
            // Simulated SOAP status check
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return new BatchStatusResult("COMPLETED", List.of());
    }

    @Override
    public InsurerCapabilities getCapabilities() {
        return new InsurerCapabilities(true, true, 200, 4, 30);
    }

    @Override
    public String getAdapterType() {
        return "BAJAJ_ALLIANZ";
    }

    @Override
    public Map<String, Object> mapToInsurerFormat(Map<String, Object> endorsementData) {
        return xmlMapper.toInsurerFormat(endorsementData);
    }

    @Override
    public Map<String, Object> mapFromInsurerFormat(Map<String, Object> insurerData) {
        return xmlMapper.fromInsurerFormat(insurerData);
    }
}
