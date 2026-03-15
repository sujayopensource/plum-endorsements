package com.plum.endorsements.infrastructure.insurer.icici;

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
public class IciciLombardAdapter implements InsurerPort {

    private final MeterRegistry meterRegistry;
    private final IciciLombardDataMapper dataMapper;

    @Override
    @CircuitBreaker(name = "iciciLombard", fallbackMethod = "submitRealTimeFallback")
    @Retry(name = "iciciLombard")
    public SubmissionResult submitRealTime(UUID endorsementId, Map<String, Object> endorsementData) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("ICICI Lombard: submitting endorsement {} via REST/JSON", endorsementId);

        try {
            // Simulated REST API call latency
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("ICICI Lombard submission interrupted for endorsement {}", endorsementId);
        }

        String insurerReference = "ICICI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("ICICI Lombard: endorsement {} accepted with reference {}", endorsementId, insurerReference);

        sample.stop(meterRegistry.timer("endorsement.insurer.icici.duration", "method", "submitRealTime"));
        return new SubmissionResult(true, insurerReference, null);
    }

    private SubmissionResult submitRealTimeFallback(UUID endorsementId, Map<String, Object> endorsementData, Throwable t) {
        log.warn("ICICI Lombard circuit breaker fallback for endorsement {}: {}", endorsementId, t.getMessage());
        return new SubmissionResult(false, null, "ICICI Lombard service unavailable: " + t.getMessage());
    }

    @Override
    public String submitBatch(UUID batchId, List<Map<String, Object>> endorsements) {
        throw new UnsupportedOperationException("ICICI Lombard does not support batch submissions");
    }

    @Override
    public BatchStatusResult checkBatchStatus(String insurerBatchRef) {
        throw new UnsupportedOperationException("ICICI Lombard does not support batch status checks");
    }

    @Override
    public InsurerCapabilities getCapabilities() {
        return new InsurerCapabilities(true, false, 0, 0, 120);
    }

    @Override
    public String getAdapterType() {
        return "ICICI_LOMBARD";
    }

    @Override
    public Map<String, Object> mapToInsurerFormat(Map<String, Object> endorsementData) {
        return dataMapper.toInsurerFormat(endorsementData);
    }

    @Override
    public Map<String, Object> mapFromInsurerFormat(Map<String, Object> insurerData) {
        return dataMapper.fromInsurerFormat(insurerData);
    }
}
