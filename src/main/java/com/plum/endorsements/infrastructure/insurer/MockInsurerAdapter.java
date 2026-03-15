package com.plum.endorsements.infrastructure.insurer;

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
public class MockInsurerAdapter implements InsurerPort {

    private final MeterRegistry meterRegistry;

    @Override
    @CircuitBreaker(name = "insurerSubmission", fallbackMethod = "submitRealTimeFallback")
    @Retry(name = "insurerSubmission")
    public SubmissionResult submitRealTime(UUID endorsementId, Map<String, Object> endorsementData) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("Mock insurer: submitting endorsement {} in real-time", endorsementId);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Mock insurer real-time submission interrupted for endorsement {}", endorsementId);
        }

        String insurerReference = "INS-RT-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Mock insurer: endorsement {} accepted with reference {}", endorsementId, insurerReference);

        sample.stop(meterRegistry.timer("endorsement.insurer.mock.duration", "method", "submitRealTime"));
        return new SubmissionResult(true, insurerReference, null);
    }

    private SubmissionResult submitRealTimeFallback(UUID endorsementId, Map<String, Object> endorsementData, Throwable t) {
        log.warn("Circuit breaker fallback for endorsement {}: {}", endorsementId, t.getMessage());
        return new SubmissionResult(false, null, "Insurer service unavailable: " + t.getMessage());
    }

    @Override
    public String submitBatch(UUID batchId, List<Map<String, Object>> endorsements) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String batchReference = "INS-BATCH-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Mock insurer: batch {} submitted with {} endorsements, insurer batch reference: {}",
                batchId, endorsements.size(), batchReference);

        sample.stop(meterRegistry.timer("endorsement.insurer.mock.duration", "method", "submitBatch"));
        return batchReference;
    }

    @Override
    public BatchStatusResult checkBatchStatus(String insurerBatchRef) {
        log.info("Mock insurer: checking batch status for reference {}", insurerBatchRef);
        return new BatchStatusResult("COMPLETED", List.of());
    }

    @Override
    public InsurerCapabilities getCapabilities() {
        return new InsurerCapabilities(true, true, 100, 24, 60);
    }

    @Override
    public String getAdapterType() {
        return "MOCK";
    }
}
