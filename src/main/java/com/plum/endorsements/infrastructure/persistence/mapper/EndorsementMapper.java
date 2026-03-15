package com.plum.endorsements.infrastructure.persistence.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.infrastructure.persistence.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Component
@RequiredArgsConstructor
public class EndorsementMapper {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public Endorsement toDomain(EndorsementEntity entity) {
        return Endorsement.builder()
            .id(entity.getId())
            .employerId(entity.getEmployerId())
            .employeeId(entity.getEmployeeId())
            .insurerId(entity.getInsurerId())
            .policyId(entity.getPolicyId())
            .type(EndorsementType.valueOf(entity.getType()))
            .status(EndorsementStatus.valueOf(entity.getStatus()))
            .coverageStartDate(entity.getCoverageStartDate())
            .coverageEndDate(entity.getCoverageEndDate())
            .employeeData(objectMapper.readTree(entity.getEmployeeData()))
            .premiumAmount(entity.getPremiumAmount())
            .batchId(entity.getBatchId())
            .insurerReference(entity.getInsurerReference())
            .retryCount(entity.getRetryCount())
            .failureReason(entity.getFailureReason())
            .idempotencyKey(entity.getIdempotencyKey())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .version(entity.getVersion())
            .build();
    }

    @SneakyThrows
    public EndorsementEntity toEntity(Endorsement domain) {
        return EndorsementEntity.builder()
            .id(domain.getId())
            .employerId(domain.getEmployerId())
            .employeeId(domain.getEmployeeId())
            .insurerId(domain.getInsurerId())
            .policyId(domain.getPolicyId())
            .type(domain.getType().name())
            .status(domain.getStatus().name())
            .coverageStartDate(domain.getCoverageStartDate())
            .coverageEndDate(domain.getCoverageEndDate())
            .employeeData(objectMapper.writeValueAsString(domain.getEmployeeData()))
            .premiumAmount(domain.getPremiumAmount())
            .batchId(domain.getBatchId())
            .insurerReference(domain.getInsurerReference())
            .retryCount(domain.getRetryCount())
            .failureReason(domain.getFailureReason())
            .idempotencyKey(domain.getIdempotencyKey())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .version(domain.getVersion())
            .build();
    }

    public ProvisionalCoverage toDomain(ProvisionalCoverageEntity entity) {
        return ProvisionalCoverage.builder()
            .id(entity.getId())
            .endorsementId(entity.getEndorsementId())
            .employeeId(entity.getEmployeeId())
            .employerId(entity.getEmployerId())
            .coverageStart(entity.getCoverageStart())
            .coverageType(entity.getCoverageType())
            .confirmedAt(entity.getConfirmedAt())
            .expiredAt(entity.getExpiredAt())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    public ProvisionalCoverageEntity toEntity(ProvisionalCoverage domain) {
        return ProvisionalCoverageEntity.builder()
            .id(domain.getId())
            .endorsementId(domain.getEndorsementId())
            .employeeId(domain.getEmployeeId())
            .employerId(domain.getEmployerId())
            .coverageStart(domain.getCoverageStart())
            .coverageType(domain.getCoverageType())
            .confirmedAt(domain.getConfirmedAt())
            .expiredAt(domain.getExpiredAt())
            .createdAt(domain.getCreatedAt())
            .build();
    }

    public EAAccount toDomain(EAAccountEntity entity) {
        return EAAccount.builder()
            .employerId(entity.getEmployerId())
            .insurerId(entity.getInsurerId())
            .balance(entity.getBalance())
            .reserved(entity.getReserved())
            .updatedAt(entity.getUpdatedAt())
            .version(entity.getVersion())
            .build();
    }

    public EAAccountEntity toEntity(EAAccount domain) {
        return EAAccountEntity.builder()
            .employerId(domain.getEmployerId())
            .insurerId(domain.getInsurerId())
            .balance(domain.getBalance())
            .reserved(domain.getReserved())
            .updatedAt(domain.getUpdatedAt())
            .version(domain.getVersion())
            .build();
    }

    public EATransaction toDomain(EATransactionEntity entity) {
        return new EATransaction(
            entity.getId(),
            entity.getEmployerId(),
            entity.getInsurerId(),
            entity.getEndorsementId(),
            EATransaction.EATransactionType.valueOf(entity.getType()),
            entity.getAmount(),
            entity.getBalanceAfter(),
            entity.getDescription(),
            entity.getCreatedAt()
        );
    }

    public EATransactionEntity toEntity(EATransaction domain) {
        return EATransactionEntity.builder()
            .id(domain.id())
            .employerId(domain.employerId())
            .insurerId(domain.insurerId())
            .endorsementId(domain.endorsementId())
            .type(domain.type().name())
            .amount(domain.amount())
            .balanceAfter(domain.balanceAfter())
            .description(domain.description())
            .createdAt(domain.createdAt())
            .build();
    }

    public EndorsementBatch toDomain(EndorsementBatchEntity entity) {
        return EndorsementBatch.builder()
            .id(entity.getId())
            .insurerId(entity.getInsurerId())
            .status(BatchStatus.valueOf(entity.getStatus()))
            .endorsementCount(entity.getEndorsementCount())
            .totalPremium(entity.getTotalPremium())
            .submittedAt(entity.getSubmittedAt())
            .slaDeadline(entity.getSlaDeadline())
            .insurerBatchRef(entity.getInsurerBatchRef())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    public EndorsementBatchEntity toEntity(EndorsementBatch domain) {
        return EndorsementBatchEntity.builder()
            .id(domain.getId())
            .insurerId(domain.getInsurerId())
            .status(domain.getStatus().name())
            .endorsementCount(domain.getEndorsementCount())
            .totalPremium(domain.getTotalPremium())
            .submittedAt(domain.getSubmittedAt())
            .slaDeadline(domain.getSlaDeadline())
            .insurerBatchRef(domain.getInsurerBatchRef())
            .createdAt(domain.getCreatedAt())
            .build();
    }

    @SneakyThrows
    public InsurerConfiguration toDomain(InsurerConfigurationEntity entity) {
        return InsurerConfiguration.builder()
            .insurerId(entity.getInsurerId())
            .insurerName(entity.getInsurerName())
            .insurerCode(entity.getInsurerCode())
            .adapterType(entity.getAdapterType())
            .supportsRealTime(entity.isSupportsRealTime())
            .supportsBatch(entity.isSupportsBatch())
            .maxBatchSize(entity.getMaxBatchSize())
            .batchSlaHours(entity.getBatchSlaHours())
            .rateLimitPerMinute(entity.getRateLimitPerMinute())
            .apiBaseUrl(entity.getApiBaseUrl())
            .authType(entity.getAuthType())
            .authConfig(entity.getAuthConfig() != null ? objectMapper.readTree(entity.getAuthConfig()) : null)
            .dataFormat(entity.getDataFormat())
            .retryMaxAttempts(entity.getRetryMaxAttempts())
            .retryWaitMs(entity.getRetryWaitMs())
            .circuitBreakerConfig(entity.getCircuitBreakerConfig() != null ? objectMapper.readTree(entity.getCircuitBreakerConfig()) : null)
            .active(entity.isActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    @SneakyThrows
    public InsurerConfigurationEntity toEntity(InsurerConfiguration domain) {
        return InsurerConfigurationEntity.builder()
            .insurerId(domain.getInsurerId())
            .insurerName(domain.getInsurerName())
            .insurerCode(domain.getInsurerCode())
            .adapterType(domain.getAdapterType())
            .supportsRealTime(domain.isSupportsRealTime())
            .supportsBatch(domain.isSupportsBatch())
            .maxBatchSize(domain.getMaxBatchSize())
            .batchSlaHours(domain.getBatchSlaHours())
            .rateLimitPerMinute(domain.getRateLimitPerMinute())
            .apiBaseUrl(domain.getApiBaseUrl())
            .authType(domain.getAuthType())
            .authConfig(domain.getAuthConfig() != null ? objectMapper.writeValueAsString(domain.getAuthConfig()) : null)
            .dataFormat(domain.getDataFormat())
            .retryMaxAttempts(domain.getRetryMaxAttempts())
            .retryWaitMs(domain.getRetryWaitMs())
            .circuitBreakerConfig(domain.getCircuitBreakerConfig() != null ? objectMapper.writeValueAsString(domain.getCircuitBreakerConfig()) : null)
            .active(domain.isActive())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }

    // --- Reconciliation mappings ---

    public ReconciliationRun toDomain(ReconciliationRunEntity entity) {
        return ReconciliationRun.builder()
            .id(entity.getId())
            .insurerId(entity.getInsurerId())
            .status(entity.getStatus())
            .totalChecked(entity.getTotalChecked())
            .matched(entity.getMatched())
            .partialMatched(entity.getPartialMatched())
            .rejected(entity.getRejected())
            .missing(entity.getMissing())
            .startedAt(entity.getStartedAt())
            .completedAt(entity.getCompletedAt())
            .build();
    }

    public ReconciliationRunEntity toEntity(ReconciliationRun domain) {
        return ReconciliationRunEntity.builder()
            .id(domain.getId())
            .insurerId(domain.getInsurerId())
            .status(domain.getStatus())
            .totalChecked(domain.getTotalChecked())
            .matched(domain.getMatched())
            .partialMatched(domain.getPartialMatched())
            .rejected(domain.getRejected())
            .missing(domain.getMissing())
            .startedAt(domain.getStartedAt())
            .completedAt(domain.getCompletedAt())
            .build();
    }

    @SneakyThrows
    public ReconciliationItem toDomain(ReconciliationItemEntity entity) {
        return ReconciliationItem.builder()
            .id(entity.getId())
            .runId(entity.getRunId())
            .endorsementId(entity.getEndorsementId())
            .batchId(entity.getBatchId())
            .insurerId(entity.getInsurerId())
            .employerId(entity.getEmployerId())
            .outcome(ReconciliationOutcome.valueOf(entity.getOutcome()))
            .sentData(entity.getSentData() != null ? objectMapper.readTree(entity.getSentData()) : null)
            .confirmedData(entity.getConfirmedData() != null ? objectMapper.readTree(entity.getConfirmedData()) : null)
            .discrepancyDetails(entity.getDiscrepancyDetails() != null ? objectMapper.readTree(entity.getDiscrepancyDetails()) : null)
            .actionTaken(entity.getActionTaken())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    @SneakyThrows
    public ReconciliationItemEntity toEntity(ReconciliationItem domain) {
        return ReconciliationItemEntity.builder()
            .id(domain.getId())
            .runId(domain.getRunId())
            .endorsementId(domain.getEndorsementId())
            .batchId(domain.getBatchId())
            .insurerId(domain.getInsurerId())
            .employerId(domain.getEmployerId())
            .outcome(domain.getOutcome().name())
            .sentData(domain.getSentData() != null ? objectMapper.writeValueAsString(domain.getSentData()) : null)
            .confirmedData(domain.getConfirmedData() != null ? objectMapper.writeValueAsString(domain.getConfirmedData()) : null)
            .discrepancyDetails(domain.getDiscrepancyDetails() != null ? objectMapper.writeValueAsString(domain.getDiscrepancyDetails()) : null)
            .actionTaken(domain.getActionTaken())
            .createdAt(domain.getCreatedAt())
            .build();
    }
}
