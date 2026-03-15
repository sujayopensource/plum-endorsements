package com.plum.endorsements.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for the Endorsement Service.
 *
 * Custom metrics taxonomy:
 * - endorsement.created (Counter): type={ADD,DELETE,UPDATE}
 * - endorsement.state.transition (Counter): from={status}, to={status}
 * - endorsement.insurer.submission.duration (Timer): mode={realtime,batch}, result={success,failure}
 * - endorsement.batch.size (DistributionSummary)
 * - endorsement.ea.reservation (Counter): result={success,insufficient}
 * - endorsement.kafka.publish (Counter): result={success,failure}, eventType={...}
 * - endorsement.scheduler.duration (Timer): scheduler={batch_assembly,batch_poller,coverage_cleanup}, result={success,failure}
 * - endorsement.scheduler.execution (Counter): scheduler={...}, result={...}
 * - endorsement.active.count (Gauge): status={11 states}
 * - endorsement.error (Counter): type={not_found,duplicate,insufficient_balance,illegal_state,validation,unexpected}
 * - endorsement.coverage.expired (Counter)
 * - endorsement.insurer.mock.duration (Timer): method={submitRealTime,submitBatch}
 * - endorsement.insurer.icici.duration (Timer): method={submitRealTime}
 * - endorsement.insurer.nivabupa.duration (Timer): method={submitBatch}
 * - endorsement.insurer.bajaj.duration (Timer): method={submitRealTime,submitBatch}
 * - endorsement.reconciliation.completed (Counter): insurerId={...}
 * - endorsement.reconciliation.matched (Gauge)
 * - endorsement.reconciliation.discrepancies (Gauge)
 * - endorsement.reconciliation.error (Counter): insurerCode={...}
 * - endorsement.insurer.active.count (Gauge)
 *
 * Phase 3 — Intelligence metrics:
 * - endorsement.anomaly.detected (Counter): anomalyType={VOLUME_SPIKE,...}, employerId={...}
 * - endorsement.anomaly.score (Summary): anomalyType={...}
 * - endorsement.forecast.generated (Counter): employerId={...}
 * - endorsement.forecast.shortfall.detected (Counter): employerId={...}
 * - endorsement.batch.optimization.savings (Summary): strategy={...}
 * - endorsement.batch.optimization.duration (Timer)
 * - endorsement.error.auto_resolved (Counter): errorType={...}, insurerId={...}
 * - endorsement.error.suggested (Counter): errorType={...}
 * - endorsement.error.resolution.confidence (Summary): errorType={...}
 * - endorsement.process.stp_rate (Gauge): insurerId={...}
 * - endorsement.process.avg_lifecycle_hours (Gauge): insurerId={...}
 */
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("service", "endorsement-service");
    }
}
