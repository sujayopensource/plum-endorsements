# Functional Specification: Plum Endorsement Service

**Version**: 4.0
**Date**: 2026-03-14
**Status**: Current
**Module**: `endorsement-service`

---

## Table of Contents

1. [Overview](#1-overview)
   - 1.1 [Product Description](#11-product-description)
   - 1.2 [Product Functional Capabilities](#12-product-functional-capabilities)
   - 1.3 [User Roles](#13-user-roles)
   - 1.4 [Use Cases](#14-use-cases)
   - 1.5 [General Constraints](#15-general-constraints)
   - 1.6 [Assumptions and Dependencies](#16-assumptions-and-dependencies)
2. [Specific Function Descriptions](#2-specific-function-descriptions)
   - 2.1 [F-01: Create Endorsement](#21-f-01-create-endorsement)
   - 2.2 [F-02: Get Endorsement by ID](#22-f-02-get-endorsement-by-id)
   - 2.3 [F-03: List Endorsements](#23-f-03-list-endorsements)
   - 2.4 [F-04: Submit Endorsement to Insurer](#24-f-04-submit-endorsement-to-insurer)
   - 2.5 [F-05: Confirm Endorsement](#25-f-05-confirm-endorsement)
   - 2.6 [F-06: Reject Endorsement](#26-f-06-reject-endorsement)
   - 2.7 [F-07: Get Provisional Coverage](#27-f-07-get-provisional-coverage)
   - 2.8 [F-08: Get EA Account Balance](#28-f-08-get-ea-account-balance)
   - 2.9 [F-09: Batch Assembly (Scheduled)](#29-f-09-batch-assembly-scheduled)
   - 2.10 [F-10: Batch Status Polling (Scheduled)](#210-f-10-batch-status-polling-scheduled)
   - 2.11 [F-11: Provisional Coverage Cleanup (Scheduled)](#211-f-11-provisional-coverage-cleanup-scheduled)
   - 2.12 [F-12: Endorsement State Machine](#212-f-12-endorsement-state-machine)
   - 2.13 [F-13: EA Balance Management](#213-f-13-ea-balance-management)
   - 2.14 [F-14: Event Publishing](#214-f-14-event-publishing)
   - 2.15 [F-15: Idempotency Control](#215-f-15-idempotency-control)
   - 2.16 [F-16: Notification Dispatch](#216-f-16-notification-dispatch)
   - 2.17 [F-17: List Insurer Configurations](#217-f-17-list-insurer-configurations)
   - 2.18 [F-18: Get Insurer Configuration](#218-f-18-get-insurer-configuration)
   - 2.19 [F-19: Get Insurer Capabilities](#219-f-19-get-insurer-capabilities)
   - 2.20 [F-20: Multi-Insurer Routing](#220-f-20-multi-insurer-routing)
   - 2.21 [F-21: Reconciliation Engine (Scheduled)](#221-f-21-reconciliation-engine-scheduled)
   - 2.22 [F-22: Trigger Manual Reconciliation](#222-f-22-trigger-manual-reconciliation)
   - 2.23 [F-23: Get Reconciliation Runs](#223-f-23-get-reconciliation-runs)
   - 2.24 [F-24: Get Reconciliation Items](#224-f-24-get-reconciliation-items)
   - 2.25 [F-25: EA Balance Optimization](#225-f-25-ea-balance-optimization)
   - 2.26 [F-26: Enhanced Kafka Event Architecture](#226-f-26-enhanced-kafka-event-architecture)
   - 2.27 [F-27: List Anomalies](#227-f-27-list-anomalies)
   - 2.28 [F-28: Get Anomaly](#228-f-28-get-anomaly)
   - 2.29 [F-29: Review Anomaly](#229-f-29-review-anomaly)
   - 2.30 [F-30: Anomaly Detection (Scheduled)](#230-f-30-anomaly-detection-scheduled)
   - 2.31 [F-31: Get Latest Balance Forecast](#231-f-31-get-latest-balance-forecast)
   - 2.32 [F-32: Get Forecast History](#232-f-32-get-forecast-history)
   - 2.33 [F-33: Generate Balance Forecast](#233-f-33-generate-balance-forecast)
   - 2.34 [F-34: Balance Forecast (Scheduled)](#234-f-34-balance-forecast-scheduled)
   - 2.35 [F-35: List Error Resolutions](#235-f-35-list-error-resolutions)
   - 2.36 [F-36: Get Error Resolution Stats](#236-f-36-get-error-resolution-stats)
   - 2.37 [F-37: Approve Error Resolution](#237-f-37-approve-error-resolution)
   - 2.38 [F-38: Automated Error Resolution (Event-Driven)](#238-f-38-automated-error-resolution-event-driven)
   - 2.39 [F-39: Get Process Mining Metrics](#239-f-39-get-process-mining-metrics)
   - 2.40 [F-40: Get Process Mining Insights](#240-f-40-get-process-mining-insights)
   - 2.41 [F-41: Get STP Rate](#241-f-41-get-stp-rate)
   - 2.42 [F-42: Trigger Process Mining Analysis](#242-f-42-trigger-process-mining-analysis)
   - 2.43 [F-43: Process Mining (Scheduled)](#243-f-43-process-mining-scheduled)
   - 2.44 [F-44: Smart Batch Optimization](#244-f-44-smart-batch-optimization)
   - 2.45 [F-45: Phase 3 Event Types](#245-f-45-phase-3-event-types)
   - 2.46 [F-46: Employer Health Score](#246-f-46-employer-health-score)
   - 2.47 [F-47: Insurer Benchmarks](#247-f-47-insurer-benchmarks)
   - 2.48 [F-48: Create Insurer Configuration](#248-f-48-create-insurer-configuration)
   - 2.49 [F-49: Update Insurer Configuration](#249-f-49-update-insurer-configuration)
   - 2.50 [F-50: Get Audit Logs](#250-f-50-get-audit-logs)
   - 2.51 [F-51: Stuck Endorsement Retry (Scheduled)](#251-f-51-stuck-endorsement-retry-scheduled)
   - 2.52 [F-52: Data Retention Cleanup (Scheduled)](#252-f-52-data-retention-cleanup-scheduled)
   - 2.53 [F-53: Get STP Rate Trend](#253-f-53-get-stp-rate-trend)
   - 2.54 [F-54: Error Resolution Outcome Tracking](#254-f-54-error-resolution-outcome-tracking)
3. [External Interfaces](#3-external-interfaces)
   - 3.1 [User Interfaces](#31-user-interfaces)
   - 3.2 [Software Interfaces](#32-software-interfaces)
   - 3.3 [Communication Interfaces](#33-communication-interfaces)
   - 3.4 [Performance Requirements](#34-performance-requirements)
   - 3.5 [Design Constraints](#35-design-constraints)
4. [Attributes](#4-attributes)
   - 4.1 [Security](#41-security)
   - 4.2 [Reliability and Availability](#42-reliability-and-availability)
   - 4.3 [Configurability](#43-configurability)
   - 4.4 [Observability](#44-observability)
5. [Data Model](#5-data-model)
   - 5.1 [Database Schema](#51-database-schema)
   - 5.2 [Domain Entities](#52-domain-entities)
   - 5.3 [Enumerations](#53-enumerations)
6. [Error Handling](#6-error-handling)
7. [Production Hardening Requirements](#7-production-hardening-requirements)
   - 7.1 [Functional Hardening](#71-functional-hardening)
   - 7.2 [Security Hardening](#72-security-hardening)
   - 7.3 [High Availability & Resilience](#73-high-availability--resilience)
   - 7.4 [Performance & Scalability](#74-performance--scalability)
   - 7.5 [Observability & Operations](#75-observability--operations)
   - 7.6 [Data Management](#76-data-management)
   - 7.7 [Deployment & CI/CD](#77-deployment--cicd)
   - 7.8 [Testing Maturity](#78-testing-maturity)

---

## 1. Overview

### 1.1 Product Description

The Plum Endorsement Service is a backend platform for managing insurance policy endorsements in an employer-sponsored group health insurance context. An endorsement represents a change to an insurance policy — adding an employee, removing an employee, or updating employee details. The service manages the full lifecycle of these endorsements, from creation through insurer processing to final confirmation or rejection.

The system implements hexagonal architecture with clear separation between domain logic, application services, and infrastructure adapters. It supports two submission modes (real-time and batch), provisional coverage for immediate employee protection, employer advance (EA) account management for financial tracking, multi-insurer routing with heterogeneous API adapters, automated reconciliation, EA balance optimization, and an intelligence layer providing anomaly detection, predictive balance forecasting, automated error resolution, and process mining.

**Technology Stack**:
- Java 21 with virtual threads
- Spring Boot 3.4.3
- PostgreSQL (primary data store)
- Apache Kafka (event streaming)
- Redis (caching)
- Resilience4j (circuit breaker and retry)
- Micrometer + Prometheus (metrics)
- OpenTelemetry (distributed tracing)
- Flyway (schema migrations)

### 1.2 Product Functional Capabilities

| # | Capability | Description |
|---|-----------|-------------|
| C-01 | Endorsement Lifecycle Management | Create, validate, submit, confirm, and reject endorsements through an 11-state machine |
| C-02 | Real-Time Insurer Submission | Submit individual endorsements to insurers via synchronous API with circuit breaker |
| C-03 | Batch Insurer Submission | Assemble queued endorsements into batches and submit to insurers on a schedule |
| C-04 | Provisional Coverage | Grant immediate provisional coverage to employees upon ADD endorsement creation |
| C-05 | EA Account Management | Track employer advance balances, reserve funds for pending endorsements, debit on confirmation |
| C-06 | Idempotent Operations | Prevent duplicate endorsement creation using idempotency keys |
| C-07 | Retry with Backoff | Automatically retry rejected endorsements up to 3 times with exponential backoff |
| C-08 | Event Sourcing | Record and publish domain events for every state transition via Kafka |
| C-09 | Employer Notifications | Notify employers on confirmation, rejection, insufficient balance, and SLA breaches |
| C-10 | Stale Coverage Cleanup | Expire provisional coverages that remain unconfirmed beyond a configurable threshold |
| C-11 | Batch SLA Monitoring | Detect and alert when insurer batch processing exceeds agreed SLA deadlines |
| C-12 | Observability | Expose metrics, health checks, distributed tracing, and structured logging |
| C-13 | Multi-Insurer Routing | Route endorsements to the correct insurer adapter based on insurer configuration, supporting heterogeneous APIs (REST/JSON, SOAP/XML, CSV/SFTP) |
| C-14 | Insurer Configuration Management | Manage insurer configurations with adapter type, capabilities, rate limits, SLA hours, and authentication settings |
| C-15 | Automated Reconciliation | Periodically reconcile submitted endorsements against insurer records, detecting matches, partial matches, and missing endorsements |
| C-16 | EA Balance Optimization | Optimize batch assembly by prioritizing deletions before additions to maximize EA balance utilization |
| C-17 | Per-Insurer Resilience | Apply independent circuit breaker, retry, and rate limit configurations per insurer adapter |
| C-18 | Insurer Data Format Mapping | Transform endorsement data to insurer-specific formats (JSON field mapping, CSV row generation, SOAP/XML envelope construction) |
| C-19 | Balance Forecasting | Forecast EA balance requirements with safety margins and alert on projected shortfalls |
| C-20 | Anomaly Detection | Rule-based detection of suspicious endorsement patterns (volume spikes, add/delete cycling, suspicious timing, unusual premiums) with review workflow |
| C-21 | Predictive EA Balance Forecasting | 30-day balance projection using historical burn rates with day-of-week and seasonal adjustments, shortfall alerting |
| C-22 | Automated Error Resolution | Pattern-based detection and auto-correction of insurer submission errors (date formats, missing fields, member IDs, premium mismatches) |
| C-23 | Process Mining | Event stream analysis to identify workflow bottlenecks, calculate STP rates, and measure lifecycle performance per insurer |
| C-24 | Smart Batch Optimization | Constraint-based batch assembly using composite scoring (urgency + EA impact) to minimize capital lockup |
| C-25 | Employer Health Score | Composite health scoring (endorsement success rate, anomaly score, balance health, reconciliation) with risk level classification |
| C-26 | Cross-Insurer Benchmarking | Performance benchmarking across insurers with STP rates, processing time percentiles, and ranking |
| C-27 | Insurer Configuration CRUD | Full lifecycle management of insurer configurations (create, update, activate/deactivate) with cache eviction |
| C-28 | Audit Logging | Aspect-driven audit trail capturing all mutation operations with entity type, action, and timestamp to dedicated audit_logs table |
| C-29 | Distributed Scheduler Coordination | ShedLock-based distributed locking for scheduled jobs to prevent duplicate execution in multi-instance deployments |
| C-30 | Data Retention & Archival | Scheduled cleanup of stale data (old anomaly detections, expired forecasts, aged process mining metrics) with configurable retention periods |
| C-31 | Stuck Endorsement Recovery | Automated detection and retry of endorsements stuck in non-terminal states beyond configurable thresholds |
| C-32 | Rate Limiting | IP-based request rate limiting via in-memory sliding window filter to protect API endpoints from abuse |

### 1.3 User Roles

| Role | Description | Interactions |
|------|-------------|-------------|
| HR Administrator | Creates and manages endorsements for employees in their organization | Create, view, list, submit endorsements; view EA balance |
| Insurance Operations | Reviews and processes endorsement submissions | Confirm, reject endorsements; view batches |
| System (Scheduler) | Automated processes that run on configured schedules | Batch assembly, batch status polling, coverage cleanup, reconciliation |
| Insurer (External) | External insurance provider that processes endorsements | Receives submissions, returns confirmation/rejection |
| Operations Engineer | Monitors system health and manages insurer configurations | View insurer configs, trigger reconciliation, view reconciliation results |
| Intelligence Analyst | Reviews anomaly flags, monitors forecasts, approves error resolutions | Review anomalies, view forecasts, approve error resolutions, trigger process mining analysis |

### 1.4 Use Cases

#### UC-01: Add Employee to Policy

**Actor**: HR Administrator
**Preconditions**: Employer has an active EA account with sufficient balance.
**Flow**:
1. HR admin submits a request to add an employee with personal data, policy ID, coverage dates, and premium amount.
2. System validates the request and creates the endorsement (status: CREATED).
3. System validates business rules and transitions to VALIDATED.
4. System grants provisional coverage to the employee and transitions to PROVISIONALLY_COVERED.
5. System reserves the premium amount from the employer's EA account.
6. HR admin submits the endorsement to the insurer.
7. System sends to insurer via real-time or batch channel.
8. Insurer confirms the endorsement.
9. System marks the endorsement as CONFIRMED and notifies the employer.

**Postconditions**: Employee has confirmed coverage. EA balance is reserved/debited.

#### UC-02: Remove Employee from Policy

**Actor**: HR Administrator
**Flow**:
1. HR admin submits a DELETE endorsement for an employee.
2. System validates and transitions through CREATED → VALIDATED → PROVISIONALLY_COVERED.
3. No EA reservation is made (DELETE type does not reserve funds).
4. Endorsement is submitted and processed by insurer.

**Postconditions**: Employee is removed from the policy.

#### UC-03: Update Employee Details

**Actor**: HR Administrator
**Flow**:
1. HR admin submits an UPDATE endorsement with modified employee data.
2. System validates and processes through the standard lifecycle.
3. No EA reservation is made (UPDATE type does not reserve funds).

**Postconditions**: Employee details are updated on the policy.

#### UC-04: View Endorsement Status

**Actor**: HR Administrator
**Flow**:
1. HR admin queries by endorsement ID or lists endorsements by employer.
2. System returns current status, type, dates, premium, and insurer reference.

#### UC-05: View EA Account Balance

**Actor**: HR Administrator
**Flow**:
1. HR admin queries the EA account for a given employer-insurer pair.
2. System returns the total balance, reserved amount, and available balance.

#### UC-06: Batch Processing

**Actor**: System (Scheduler)
**Trigger**: Cron schedule every 15 minutes.
**Flow**:
1. System finds all endorsements in QUEUED_FOR_BATCH status.
2. Groups endorsements by insurer and chunks into batches (max 100 per batch).
3. Creates batch records and submits to insurer.
4. Insurer returns batch reference.
5. Batch status poller checks status every 60 seconds.
6. On completion, each endorsement is individually confirmed or rejected.

#### UC-07: Handle Rejection with Retry

**Actor**: System
**Trigger**: Insurer rejects an endorsement.
**Flow**:
1. System receives rejection with reason.
2. If retry count < 3: increment retry counter, set status to RETRY_PENDING.
3. HR admin or system resubmits the endorsement.
4. If still rejected after 3 attempts: set status to FAILED_PERMANENT, notify employer.

#### UC-08: Provisional Coverage Expiry

**Actor**: System (Scheduler)
**Trigger**: Daily at 02:00.
**Flow**:
1. System finds provisional coverages older than 30 days that are still active (not confirmed or expired).
2. Marks each as expired.
3. Logs a warning for each expired coverage.

#### UC-09: Submit Endorsement to Specific Insurer

**Actor**: HR Administrator
**Preconditions**: Insurer is registered and active in the system.
**Flow**:
1. HR admin creates an endorsement specifying the insurer ID.
2. System resolves the correct insurer adapter via InsurerRouter based on the insurer's configuration.
3. System transforms endorsement data to insurer-specific format (JSON for ICICI Lombard, CSV for Niva Bupa, XML for Bajaj Allianz).
4. System routes to real-time or batch channel based on insurer capabilities.
5. Per-insurer circuit breaker and retry policies are applied automatically.

**Postconditions**: Endorsement is processed through the correct insurer adapter with appropriate data format and resilience policies.

#### UC-10: View Insurer Configurations

**Actor**: Operations Engineer
**Flow**:
1. Engineer queries the list of all active insurer configurations.
2. System returns insurer names, codes, adapter types, supported modes, rate limits, and SLA settings.
3. Engineer can drill down to a specific insurer's capabilities.

#### UC-11: Automated Reconciliation

**Actor**: System (Scheduler)
**Trigger**: Cron schedule every 15 minutes.
**Flow**:
1. System iterates through all active insurers.
2. For each insurer, finds endorsements in INSURER_PROCESSING status.
3. Compares endorsement state against insurer records:
   - **MATCH**: Endorsement has insurer reference and appropriate status → confirmed.
   - **PARTIAL_MATCH**: Endorsement has reference but missing batch association → flagged.
   - **MISSING**: Endorsement has no insurer reference → escalated.
4. Creates ReconciliationRun and ReconciliationItem records.
5. Publishes reconciliation events and sends notifications for discrepancies.

**Postconditions**: Discrepancies are identified, logged, and notified. Matched endorsements are confirmed.

#### UC-12: Trigger Manual Reconciliation

**Actor**: Operations Engineer
**Flow**:
1. Engineer selects an insurer from the reconciliation page.
2. Engineer triggers a manual reconciliation run.
3. System executes the same reconciliation logic as the scheduled job for the selected insurer.
4. Results are displayed with match/partial/rejected/missing counts and expandable item details.

#### UC-13: EA Balance Optimization in Batch Assembly

**Actor**: System (Scheduler)
**Trigger**: During batch assembly (every 15 minutes).
**Flow**:
1. System groups queued endorsements by insurer and employer.
2. System classifies each endorsement by priority (P0=deletions, P1=cost-neutral, P2=additions, P3=premium updates).
3. System sequences endorsements to process deletions first, freeing EA balance for subsequent additions.
4. System constructs optimized batches that respect both insurer max batch size and employer EA balance constraints.
5. If a balance shortfall is projected, system publishes a BalanceForecastAlert event and defers affected endorsements.

**Postconditions**: Batches are optimally sequenced to minimize capital lockup. Employers are alerted to projected shortfalls.

#### UC-14: Review Anomaly Flag

**Actor**: Intelligence Analyst
**Preconditions**: Anomaly detection scheduler has flagged an endorsement pattern.
**Flow**:
1. Analyst views the anomaly dashboard showing all FLAGGED anomalies.
2. Analyst filters by employer ID or anomaly status.
3. Analyst reviews the anomaly details (type, score, explanation).
4. Analyst transitions the anomaly to UNDER_REVIEW, then either DISMISSED (false positive) or CONFIRMED_FRAUD.
5. Analyst may add reviewer notes explaining the decision.

**Postconditions**: Anomaly is reviewed and resolved. Decision is auditable.

#### UC-15: Monitor EA Balance Forecast

**Actor**: HR Administrator / Intelligence Analyst
**Preconditions**: Balance forecast scheduler has generated forecasts for employer EA accounts.
**Flow**:
1. User requests the latest forecast for an employer-insurer pair.
2. System returns the 30-day projected balance need, narrative explanation, and shortfall status.
3. If a shortfall is projected, employer is notified to top up the EA account.
4. User can view forecast history to track accuracy over time.

**Postconditions**: Employer has visibility into future balance requirements.

#### UC-16: Review Error Resolution Suggestion

**Actor**: Intelligence Analyst
**Preconditions**: An endorsement submission has failed and the error resolver has generated a suggestion.
**Flow**:
1. Analyst views the error resolution dashboard showing stats (total, auto-applied, suggested, auto-apply rate).
2. Analyst reviews suggestions with confidence < 95% that were not auto-applied.
3. Analyst approves a suggestion, which applies the correction and resubmits the endorsement.

**Postconditions**: Error is resolved and endorsement resubmitted. Resolution is tracked for future auto-apply learning.

#### UC-17: Analyze Process Efficiency

**Actor**: Operations Engineer / Intelligence Analyst
**Flow**:
1. Analyst views the process mining dashboard showing STP rate and per-insurer metrics.
2. Analyst reviews bottleneck insights (transitions where p95 > 2x average).
3. Analyst can trigger an on-demand analysis for fresh data.
4. System shows per-insurer STP rates, average lifecycle times, and happy path percentages.

**Postconditions**: Operational insights are available for process improvement decisions.

### 1.5 General Constraints

| Constraint | Description |
|------------|-------------|
| GC-01 | Only ADD-type endorsements trigger provisional coverage and EA balance reservation |
| GC-02 | Endorsements cannot skip states — all transitions must follow the state machine |
| GC-03 | An endorsement may be retried at most 3 times before permanent failure |
| GC-04 | Batch size is capped by insurer-reported maxBatchSize (default 100) |
| GC-05 | Idempotency keys must be unique system-wide |
| GC-06 | EA account balance can never go negative (enforced in domain model) |
| GC-07 | Terminal states (CONFIRMED, FAILED_PERMANENT) allow no further transitions |
| GC-08 | Provisional coverages expire after 30 days if not confirmed |
| GC-09 | Each insurer adapter must declare its adapter type and capabilities via InsurerPort |
| GC-10 | Insurer-specific data format mapping is the adapter's responsibility |
| GC-11 | Reconciliation runs are per-insurer and do not cross insurer boundaries |
| GC-12 | Batch assembly must process deletions before additions within each employer group to optimize EA balance |
| GC-13 | Anomalies with score below `min-anomaly-score` (default 0.7) are not persisted or flagged |
| GC-14 | Error resolutions with confidence >= `auto-apply-threshold` (default 0.95) are auto-applied; below threshold requires manual approval |
| GC-15 | Anomaly status transitions are one-way: FLAGGED → UNDER_REVIEW → DISMISSED or CONFIRMED_FRAUD (terminal states allow no further transitions) |
| GC-16 | Maximum auto-retries for error resolution is 2 (configurable) to prevent infinite retry loops |
| GC-17 | Process mining metrics are replaced (not appended) on each analysis run per insurer |

### 1.6 Assumptions and Dependencies

| # | Assumption / Dependency |
|---|------------------------|
| A-01 | PostgreSQL 14+ is available with `gen_random_uuid()` support |
| A-02 | Kafka cluster is available for event publishing |
| A-03 | Redis is available for caching (Caffeine used as fallback) |
| A-04 | Insurer APIs conform to the InsurerPort interface contract |
| A-05 | The mock insurer adapter is used in development/test; production will use real insurer integrations |
| A-06 | All timestamps use UTC |
| A-07 | Financial amounts use DECIMAL(12,2) precision |
| A-08 | Virtual threads are available (Java 21+) |
| A-09 | All insurer adapters are simulated implementations; production will connect to real insurer APIs |
| A-10 | Insurer configurations are seeded via Flyway migrations and managed through the InsurerRegistry |
| A-11 | Reconciliation assumes endorsements in INSURER_PROCESSING status are candidates for verification |
| A-12 | Intelligence implementations are simulated/rule-based (not ML/LLM); production would use Spring AI / LangChain4j for AI-powered analysis |
| A-13 | Anomaly detection rules are deterministic; production would add Isolation Forest + LLM-based pattern analysis |
| A-14 | Balance forecasting uses statistical methods (Commons Math3); production would integrate Prophet/ARIMA or Spring AI for NL explanations |
| A-15 | Error resolution uses pattern matching; production would implement RAG pipeline with indexed insurer API docs |
| A-16 | Process mining requires endorsement events in the database; accuracy depends on event completeness |

---

## 2. Specific Function Descriptions

### 2.1 F-01: Create Endorsement

**Description**: Creates a new insurance endorsement, validates it, grants provisional coverage (for ADD type), and reserves EA funds.

**Endpoint**: `POST /api/v1/endorsements`

#### Inputs

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `employerId` | UUID | Yes | @NotNull | Identifier of the employer organization |
| `employeeId` | UUID | Yes | @NotNull | Identifier of the employee being endorsed |
| `insurerId` | UUID | Yes | @NotNull | Identifier of the insurance provider |
| `policyId` | UUID | Yes | @NotNull | Identifier of the insurance policy |
| `type` | String | Yes | @NotBlank, must be ADD/DELETE/UPDATE | Type of endorsement |
| `coverageStartDate` | LocalDate | Yes | @NotNull | Start date of coverage |
| `coverageEndDate` | LocalDate | No | — | End date of coverage |
| `employeeData` | JSON Object | Yes | @NotNull | Flexible employee information (name, age, department, email) |
| `premiumAmount` | BigDecimal | No | — | Premium amount for coverage |
| `idempotencyKey` | String | No | Auto-generated if absent | Unique key to prevent duplicate creation |

**Auto-generated Idempotency Key Format**: `{employerId}-{employeeId}-{type}-{coverageStartDate}`

#### Processing

```
1. CHECK idempotency
   └─ Query endorsement by idempotencyKey
   └─ IF exists → return existing endorsement (201)

2. INITIALIZE endorsement
   ├─ Set status = CREATED
   ├─ Set createdAt = now()
   ├─ Set updatedAt = now()
   └─ Set version = 1

3. PERSIST endorsement to database

4. SET MDC context (endorsementId, employerId)
   INCREMENT endorsement.created counter (tag: type)

5. PUBLISH EndorsementEvent.Created to Kafka

6. TRANSITION status: CREATED → VALIDATED
   PUBLISH EndorsementEvent.Validated

7. IF type == ADD:
   a. CREATE ProvisionalCoverage record
      ├─ coverageType = "PROVISIONAL"
      ├─ coverageStart = endorsement.coverageStartDate
      └─ createdAt = now()
   b. TRANSITION status: VALIDATED → PROVISIONALLY_COVERED
      PUBLISH EndorsementEvent.ProvisionalCoverageGranted
   c. FIND EA account (employerId + insurerId)
      ├─ IF account exists AND canFund(premiumAmount):
      │   ├─ RESERVE premiumAmount on EA account
      │   ├─ SAVE EATransaction (type=RESERVE)
      │   └─ INCREMENT endorsement.ea.reservation (result=success)
      └─ IF insufficient funds:
          ├─ LOG warning
          └─ INCREMENT endorsement.ea.reservation (result=insufficient)

8. IF type == DELETE or UPDATE:
   └─ TRANSITION status: VALIDATED → PROVISIONALLY_COVERED
      PUBLISH EndorsementEvent.ProvisionalCoverageGranted

9. PERSIST final endorsement state

10. CLEAR MDC context
```

#### Outputs

**Success (201 Created)**:
```json
{
  "id": "uuid",
  "employerId": "uuid",
  "employeeId": "uuid",
  "insurerId": "uuid",
  "policyId": "uuid",
  "type": "ADD",
  "status": "PROVISIONALLY_COVERED",
  "coverageStartDate": "2026-04-01",
  "coverageEndDate": "2027-04-01",
  "premiumAmount": 2500.00,
  "batchId": null,
  "insurerReference": null,
  "retryCount": 0,
  "failureReason": null,
  "idempotencyKey": "emp1-ee1-ADD-2026-04-01",
  "createdAt": "2026-03-07T10:00:00Z",
  "updatedAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses**:

| Status | Condition | Body |
|--------|-----------|------|
| 400 | Validation error (missing required fields) | `{ "title": "Validation Error", "detail": [{"field":"employerId","message":"must not be null"}] }` |
| 409 | Duplicate idempotencyKey | `{ "title": "Duplicate Endorsement", "detail": "Duplicate endorsement with idempotency key: ..." }` |
| 422 | EA balance insufficient | `{ "title": "Insufficient Balance", "detail": "Insufficient EA balance for employer ..." }` |

---

### 2.2 F-02: Get Endorsement by ID

**Description**: Retrieves a single endorsement by its unique identifier.

**Endpoint**: `GET /api/v1/endorsements/{id}`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | Yes | Endorsement identifier |

#### Processing

```
1. QUERY endorsement by id from database
2. IF not found → throw EndorsementNotFoundException
3. MAP domain model to EndorsementResponse DTO
```

#### Outputs

**Success (200 OK)**: EndorsementResponse JSON (same structure as F-01 output).

**Error (404 Not Found)**:
```json
{
  "title": "Endorsement Not Found",
  "status": 404,
  "detail": "Endorsement not found: {id}"
}
```

---

### 2.3 F-03: List Endorsements

**Description**: Lists endorsements for a given employer with optional status filtering and pagination.

**Endpoint**: `GET /api/v1/endorsements`

#### Inputs

| Parameter | Location | Type | Required | Default | Description |
|-----------|----------|------|----------|---------|-------------|
| `employerId` | Query | UUID | Yes | — | Employer identifier |
| `statuses` | Query | List\<String\> | No | all | Filter by endorsement statuses (multi-value) |
| `page` | Query | int | No | 0 | Page number (zero-based) |
| `size` | Query | int | No | 20 | Page size |

#### Processing

```
1. IF statuses provided:
   └─ PARSE status strings to EndorsementStatus enum values
   └─ QUERY findByEmployerIdAndStatusIn(employerId, statuses, Pageable)
2. ELSE:
   └─ QUERY findByEmployerId(employerId, Pageable)
3. MAP each Endorsement to EndorsementResponse
4. RETURN paginated result
```

#### Outputs

**Success (200 OK)**: Spring Page wrapper containing:
```json
{
  "content": [ /* EndorsementResponse objects */ ],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 42,
  "totalPages": 3
}
```

---

### 2.4 F-04: Submit Endorsement to Insurer

**Description**: Submits an endorsement to the insurer for processing. Routes through real-time or batch channel based on insurer capabilities.

**Endpoint**: `POST /api/v1/endorsements/{id}/submit`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | Yes | Endorsement identifier |

#### Processing

```
1. LOAD endorsement by id (throw EndorsementNotFoundException if missing)
2. SET MDC context (endorsementId, employerId)
3. RESOLVE insurer adapter via InsurerRouter.resolve(endorsement.insurerId)
4. GET insurer capabilities from resolved adapter
5. MAP endorsement data to insurer format via adapter.mapToInsurerFormat()

4. IF insurer supportsRealTime:
   a. TRANSITION status → SUBMITTED_REALTIME
      PUBLISH EndorsementEvent.SubmittedRealtime
   b. CALL insurerPort.submitRealTime(endorsementId, data)
      ├─ Circuit breaker: 50% failure threshold, 30s open state
      └─ Retry: 3 attempts, 2s base backoff, 2x exponential multiplier
   c. IF submission succeeds:
      ├─ SET insurerReference on endorsement
      ├─ TRANSITION → INSURER_PROCESSING → CONFIRMED
      ├─ CONFIRM provisional coverage (set coverageType="CONFIRMED")
      ├─ NOTIFY employer (endorsement confirmed)
      └─ PUBLISH EndorsementEvent.Confirmed
   d. IF submission fails:
      ├─ SET failureReason on endorsement
      ├─ TRANSITION → REJECTED
      └─ NOTIFY employer (endorsement rejected)

5. IF insurer does NOT support realTime (batch only):
   a. TRANSITION status → QUEUED_FOR_BATCH
      PUBLISH EndorsementEvent.QueuedForBatch

6. PERSIST endorsement
7. CLEAR MDC context
```

#### Outputs

**Success (202 Accepted)**: Empty body. The endorsement is now being processed.

**Error Responses**:

| Status | Condition |
|--------|-----------|
| 400 | Invalid state transition (endorsement not in a submittable state) |
| 404 | Endorsement not found |

---

### 2.5 F-05: Confirm Endorsement

**Description**: Records insurer confirmation of an endorsement with an insurer reference number.

**Endpoint**: `POST /api/v1/endorsements/{id}/confirm`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | Yes | Endorsement identifier |
| `insurerReference` | Query | String | Yes | Insurer-assigned reference number |

#### Processing

```
1. LOAD endorsement by id
2. SET insurerReference on endorsement
3. TRANSITION status → CONFIRMED
4. CONFIRM provisional coverage (coverageType="CONFIRMED", confirmedAt=now)
5. NOTIFY employer (endorsement confirmed)
6. PUBLISH EndorsementEvent.Confirmed
7. INCREMENT endorsement.state.transition counter (to=CONFIRMED)
8. PERSIST endorsement
```

#### Outputs

**Success (200 OK)**: Empty body.

**Error Responses**:

| Status | Condition |
|--------|-----------|
| 400 | Endorsement is not in a confirmable state |
| 404 | Endorsement not found |

---

### 2.6 F-06: Reject Endorsement

**Description**: Records insurer rejection of an endorsement with a reason. Implements retry logic for up to 3 attempts.

**Endpoint**: `POST /api/v1/endorsements/{id}/reject`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | Yes | Endorsement identifier |
| `reason` | Query | String | Yes | Reason for rejection |

#### Processing

```
1. LOAD endorsement by id
2. SET failureReason on endorsement

3. IF endorsement.canRetry() (retryCount < 3 AND status == REJECTED):
   a. INCREMENT retryCount
   b. TRANSITION status → RETRY_PENDING
   c. PUBLISH EndorsementEvent.RetryScheduled(attemptNumber)

4. ELSE (retries exhausted):
   a. TRANSITION status → FAILED_PERMANENT
   b. PUBLISH EndorsementEvent.FailedPermanent(reason)
   c. NOTIFY employer (endorsement rejected permanently)

5. PERSIST endorsement
```

#### Outputs

**Success (200 OK)**: Empty body.

**Error Responses**:

| Status | Condition |
|--------|-----------|
| 400 | Endorsement is not in a rejectable state |
| 404 | Endorsement not found |

---

### 2.7 F-07: Get Provisional Coverage

**Description**: Retrieves the provisional coverage record for a given endorsement.

**Endpoint**: `GET /api/v1/endorsements/{id}/coverage`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | Yes | Endorsement identifier |

#### Processing

```
1. QUERY provisional coverage by endorsementId
2. IF found → return ProvisionalCoverage
3. IF not found → return 404
```

#### Outputs

**Success (200 OK)**:
```json
{
  "id": "uuid",
  "endorsementId": "uuid",
  "employeeId": "uuid",
  "employerId": "uuid",
  "coverageStart": "2026-04-01",
  "coverageType": "PROVISIONAL",
  "confirmedAt": null,
  "expiredAt": null,
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Not Found (404)**: Empty body.

---

### 2.8 F-08: Get EA Account Balance

**Description**: Retrieves the employer advance account balance for a given employer-insurer pair.

**Endpoint**: `GET /api/v1/ea-accounts`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `employerId` | Query | UUID | Yes | Employer identifier |
| `insurerId` | Query | UUID | Yes | Insurer identifier |

#### Processing

```
1. QUERY EA account by (employerId, insurerId) composite key
2. IF found → calculate availableBalance = balance - reserved
3. IF not found → return 404
```

#### Outputs

**Success (200 OK)**:
```json
{
  "employerId": "uuid",
  "insurerId": "uuid",
  "balance": 1000000.00,
  "reserved": 2500.00,
  "availableBalance": 997500.00,
  "updatedAt": "2026-03-07T10:00:00Z"
}
```

**Not Found (404)**: Empty body.

---

### 2.9 F-09: Batch Assembly (Scheduled)

**Description**: Periodically assembles queued endorsements into batches grouped by insurer and submits them.

**Trigger**: Cron `0 */15 * * * *` (every 15 minutes)

#### Inputs

None (scheduled job).

#### Processing

```
1. QUERY all endorsements with status = QUEUED_FOR_BATCH
2. GROUP endorsements by insurerId

3. FOR EACH insurer group:
   a. GET insurer capabilities (maxBatchSize)
   b. CHUNK endorsements into sublists of maxBatchSize (default 100)

   c. FOR EACH chunk:
      i.   CALCULATE totalPremium (sum of premiumAmounts)
      ii.  CREATE EndorsementBatch record
           ├─ status = ASSEMBLING
           ├─ endorsementCount = chunk.size
           └─ totalPremium = calculated sum
      iii. PERSIST batch
      iv.  FOR EACH endorsement in chunk:
           ├─ SET batchId
           ├─ TRANSITION status → BATCH_SUBMITTED
           └─ PUBLISH EndorsementEvent.BatchSubmitted
      v.   CALL insurerPort.submitBatch(batchId, endorsements)
      vi.  UPDATE batch:
           ├─ status = SUBMITTED
           ├─ insurerBatchRef = returned reference
           ├─ submittedAt = now()
           └─ slaDeadline = now() + batchSlaHours
      vii. PERSIST batch
      viii. RECORD batch size metric

4. LOG completion with total endorsements and batches processed
```

#### Outputs

No direct outputs. Side effects:
- Endorsements transition from QUEUED_FOR_BATCH to BATCH_SUBMITTED.
- Batch records created in database with insurer references.
- Events published to Kafka.
- Metrics recorded.

---

### 2.10 F-10: Batch Status Polling (Scheduled)

**Description**: Polls insurers for the status of submitted batches and processes results.

**Trigger**: Fixed delay, 60 seconds between runs.

#### Inputs

None (scheduled job).

#### Processing

```
1. QUERY all batches with status IN (SUBMITTED, PROCESSING)

2. FOR EACH batch:
   a. CHECK SLA breach: IF now() > slaDeadline AND status != COMPLETE
      └─ NOTIFY batchSlaBreached(batchId, insurerId)

   b. CALL insurerPort.checkBatchStatus(insurerBatchRef)

   c. SWITCH on returned status:
      ├─ "PROCESSING":
      │   └─ UPDATE batch status → PROCESSING
      ├─ "COMPLETED":
      │   ├─ UPDATE batch status → COMPLETE
      │   └─ FOR EACH endorsement result:
      │       ├─ IF confirmed → processHandler.handleConfirmation(id, ref)
      │       └─ IF rejected  → processHandler.handleRejection(id, reason)
      └─ "FAILED":
          └─ UPDATE batch status → FAILED

3. PERSIST all batch updates
```

#### Outputs

No direct outputs. Side effects:
- Endorsements within completed batches transition to CONFIRMED or REJECTED.
- SLA breach notifications sent.
- Batch status updated.

---

### 2.11 F-11: Provisional Coverage Cleanup (Scheduled)

**Description**: Expires provisional coverages that have been active beyond the configured threshold without confirmation.

**Trigger**: Cron `0 0 2 * * *` (daily at 02:00)
**Configuration**: `endorsement.provisional-coverage.max-days` (default: 30)

#### Inputs

None (scheduled job).

#### Processing

```
1. QUERY provisional coverages WHERE:
   ├─ createdAt < (now - maxDays)
   ├─ confirmedAt IS NULL
   └─ expiredAt IS NULL

2. FOR EACH stale coverage:
   a. SET expiredAt = now()
   b. PERSIST coverage
   c. LOG warning: "Expiring stale provisional coverage: {id}"
   d. INCREMENT endorsement.coverage.expired counter

3. LOG completion: "{count} provisional coverages expired"
```

#### Outputs

No direct outputs. Stale coverages are marked as expired in the database.

---

### 2.12 F-12: Endorsement State Machine

**Description**: Governs all valid endorsement status transitions. Every state change must be validated by the state machine before it is applied.

#### State Transition Diagram

```
CREATED ──────────────► VALIDATED
                            │
                            ▼
                   PROVISIONALLY_COVERED
                      │            │
                      ▼            ▼
            SUBMITTED_REALTIME   QUEUED_FOR_BATCH
                  │                    │
                  ▼                    ▼
            INSURER_PROCESSING   BATCH_SUBMITTED
                  │                    │
            ┌─────┴─────┐       ┌─────┴─────┐
            ▼           ▼       ▼           ▼
        CONFIRMED    REJECTED  INSURER_   REJECTED
        (terminal)      │     PROCESSING     │
                        │         │          │
                        ▼         ▼          ▼
                   ┌────┴────┐  CONFIRMED  ┌────┴────┐
                   ▼         ▼  (terminal) ▼         ▼
            RETRY_PENDING  FAILED_      RETRY_    FAILED_
                   │       PERMANENT    PENDING   PERMANENT
                   │       (terminal)     │       (terminal)
                   ▼                      ▼
          SUBMITTED_REALTIME      SUBMITTED_REALTIME
          or QUEUED_FOR_BATCH     or QUEUED_FOR_BATCH
          or FAILED_PERMANENT     or FAILED_PERMANENT
```

#### Allowed Transitions Table

| From Status | Allowed Targets |
|-------------|----------------|
| CREATED | VALIDATED |
| VALIDATED | PROVISIONALLY_COVERED |
| PROVISIONALLY_COVERED | SUBMITTED_REALTIME, QUEUED_FOR_BATCH |
| SUBMITTED_REALTIME | INSURER_PROCESSING, REJECTED |
| QUEUED_FOR_BATCH | BATCH_SUBMITTED |
| BATCH_SUBMITTED | INSURER_PROCESSING, REJECTED |
| INSURER_PROCESSING | CONFIRMED, REJECTED |
| CONFIRMED | _(terminal — no transitions)_ |
| REJECTED | RETRY_PENDING, FAILED_PERMANENT |
| RETRY_PENDING | SUBMITTED_REALTIME, QUEUED_FOR_BATCH, FAILED_PERMANENT |
| FAILED_PERMANENT | _(terminal — no transitions)_ |

#### Business Rules

- `canTransitionTo(target)`: Returns `true` if the target is in the allowed set for the current status.
- `isTerminal()`: Returns `true` for CONFIRMED and FAILED_PERMANENT (no further transitions possible).
- `isActive()`: Returns `!isTerminal()`.
- `requiresInsurerAction()`: Returns `true` for SUBMITTED_REALTIME, BATCH_SUBMITTED, INSURER_PROCESSING.
- Invalid transitions throw `IllegalStateException` with message: `"Cannot transition from {current} to {target}"`.

---

### 2.13 F-13: EA Balance Management

**Description**: Manages employer advance (EA) accounts which pre-fund insurance premiums. Tracks balance, reservations, and transactions.

#### Account Operations

| Operation | Method | Effect | Transaction Type |
|-----------|--------|--------|-----------------|
| Reserve | `reserve(amount)` | Increases `reserved` by amount | RESERVE |
| Release Reservation | `releaseReservation(amount)` | Decreases `reserved` by amount | RELEASE |
| Debit | `debit(amount)` | Decreases `balance` by amount | DEBIT |
| Credit | `credit(amount)` | Increases `balance` by amount | CREDIT |
| Top Up | `credit(amount)` | Increases `balance` by amount | TOP_UP |

#### Balance Calculation

```
availableBalance = balance - reserved
```

#### Validation Rules

- `canFund(amount)`: Returns `true` if `availableBalance >= amount`.
- `reserve(amount)`: Throws `IllegalStateException` if insufficient available balance.
- All operations update the `updatedAt` timestamp.

#### EABalanceCalculator Service

- `calculateRequiredBalance(pendingAdditions)`: Sums all premium amounts from a list of pending ADD endorsements.
- `calculateExpectedCredits(pendingDeletions)`: Sums all premium amounts from a list of pending DELETE endorsements.
- `hasSufficientFunds(account, requiredAmount)`: Returns `availableBalance >= requiredAmount`.
- `sequenceForOptimalBalance(endorsements)`: Sorts endorsements by priority (P0→P3) then by coverage start date.
- `constructOptimizedBatch(endorsements, account, maxBatchSize)`: Builds optimized batch respecting balance constraints. Returns `BatchPlan(included, deferred, projectedBalance)`.
- `forecastBalance(account, endorsements)`: Projects required balance with 10% safety margin. Returns `BalanceForecast(requiredMinimum, shortfall, topUpRequired)`.

---

### 2.14 F-14: Event Publishing

**Description**: Publishes domain events to Kafka for downstream consumers and audit trail.

**Kafka Topic**: `endorsement-events`
**Partitioning Key**: `employerId` (ensures ordering per employer across all endorsements)

#### Event Types

| Event | Trigger | Key Fields |
|-------|---------|------------|
| Created | Endorsement created | employerId, employeeId, type |
| Validated | Business rules pass | endorsementId |
| ProvisionalCoverageGranted | Provisional coverage assigned | employeeId, coverageStart |
| SubmittedRealtime | Sent to insurer via real-time API | insurerId |
| QueuedForBatch | Queued for batch processing | endorsementId |
| BatchSubmitted | Included in a submitted batch | batchId |
| InsurerProcessing | Insurer acknowledged receipt | insurerReference |
| Confirmed | Insurer confirmed endorsement | insurerReference |
| Rejected | Insurer rejected endorsement | reason |
| RetryScheduled | Retry queued after rejection | attemptNumber |
| FailedPermanent | All retries exhausted | reason |
| EADebited | EA account debited | amount |
| EACredited | EA account credited | amount |
| ReconciliationMatched | Endorsement matched insurer records | employerId, insurerReference |
| ReconciliationDiscrepancy | Partial match detected during reconciliation | employerId, details |
| ReconciliationMissing | Endorsement missing from insurer records | employerId, insurerId |
| BalanceForecastAlert | EA balance shortfall projected for employer | employerId, shortfall |

All events implement `EndorsementEvent` sealed interface with:
- `UUID endorsementId()` — identifies the endorsement.
- `UUID employerId()` — identifies the employer (used as partition key in Phase 2).
- `Instant occurredAt()` — timestamp of the event.
- `String eventType()` — string identifier (e.g., `"ENDORSEMENT_CREATED"`).

#### Kafka Topics

| Topic | Partitions | Replicas | Purpose |
|-------|-----------|----------|---------|
| endorsement-events | 32 | 1 | Domain events (partition key: employerId) |
| endorsement-commands | 32 | 1 | Command messages |
| endorsement-notifications | 8 | 1 | Notification messages |
| endorsement-reconciliation | 16 | 1 | Reconciliation events |

---

### 2.15 F-15: Idempotency Control

**Description**: Prevents duplicate endorsement creation through unique idempotency keys.

#### Processing

```
1. IF request contains idempotencyKey:
   └─ USE provided key
2. ELSE:
   └─ GENERATE key = "{employerId}-{employeeId}-{type}-{coverageStartDate}"

3. QUERY existing endorsement by idempotencyKey
4. IF found:
   └─ RETURN existing endorsement (HTTP 201, no side effects)
5. IF not found:
   └─ PROCEED with creation
```

#### Database Enforcement

- Column `idempotency_key` has a UNIQUE constraint.
- Any concurrent duplicate insert will fail at the database level.

---

### 2.16 F-16: Notification Dispatch

**Description**: Sends notifications to employers for key endorsement events. Currently implemented as a logging adapter (placeholder for email/SMS/push in production).

#### Notification Types

| Event | Method | Information Included |
|-------|--------|---------------------|
| Endorsement Confirmed | `notifyEndorsementConfirmed` | employerId, endorsementId |
| Endorsement Rejected | `notifyEndorsementRejected` | employerId, endorsementId, reason |
| Insufficient Balance | `notifyInsufficientBalance` | employerId, required amount, available amount |
| Batch SLA Breached | `notifyBatchSlaBreached` | batchId, insurerId |
| Reconciliation Discrepancy | `notifyReconciliationDiscrepancy` | endorsementId, insurerId, outcome |
| Reconciliation Complete | `notifyReconciliationComplete` | insurerId, matched, discrepancies |

---

### 2.17 F-17: List Insurer Configurations

**Description**: Retrieves all active insurer configurations registered in the system.

**Endpoint**: `GET /api/v1/insurers`

#### Inputs

None.

#### Processing

```
1. QUERY all active insurer configurations from InsurerRegistry
2. MAP each InsurerConfiguration to InsurerConfigurationResponse DTO
3. RETURN list
```

#### Outputs

**Success (200 OK)**:
```json
[
  {
    "insurerId": "22222222-2222-2222-2222-222222222222",
    "insurerName": "Mock Insurer",
    "insurerCode": "MOCK",
    "adapterType": "MOCK",
    "supportsRealTime": true,
    "supportsBatch": true,
    "maxBatchSize": 100,
    "batchSlaHours": 4,
    "rateLimitPerMinute": 60,
    "dataFormat": "JSON",
    "active": true,
    "createdAt": "2026-03-08T00:00:00Z",
    "updatedAt": "2026-03-08T00:00:00Z"
  }
]
```

---

### 2.18 F-18: Get Insurer Configuration

**Description**: Retrieves a specific insurer configuration by its identifier.

**Endpoint**: `GET /api/v1/insurers/{insurerId}`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `insurerId` | Path | UUID | Yes | Insurer identifier |

#### Processing

```
1. QUERY insurer configuration by insurerId from InsurerRegistry
2. IF not found → throw InsurerNotFoundException
3. MAP to InsurerConfigurationResponse DTO
```

#### Outputs

**Success (200 OK)**: InsurerConfigurationResponse JSON (same structure as F-17 list element).

**Error (404 Not Found)**:
```json
{
  "title": "Insurer Not Found",
  "status": 404,
  "detail": "Insurer not found with ID: {insurerId}"
}
```

---

### 2.19 F-19: Get Insurer Capabilities

**Description**: Retrieves the operational capabilities of a specific insurer.

**Endpoint**: `GET /api/v1/insurers/{insurerId}/capabilities`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `insurerId` | Path | UUID | Yes | Insurer identifier |

#### Processing

```
1. QUERY insurer configuration by insurerId
2. IF not found → throw InsurerNotFoundException
3. CALL configuration.toCapabilities()
4. MAP to CapabilitiesResponse DTO
```

#### Outputs

**Success (200 OK)**:
```json
{
  "supportsRealTime": true,
  "supportsBatch": true,
  "maxBatchSize": 200,
  "batchSlaHours": 4,
  "rateLimitPerMinute": 30
}
```

---

### 2.20 F-20: Multi-Insurer Routing

**Description**: Routes endorsement processing to the correct insurer adapter based on the endorsement's insurer ID. The InsurerRouter resolves the appropriate adapter from the InsurerRegistry, enabling heterogeneous insurer integrations.

#### Architecture

```
                         ┌─────────────────┐
                         │  InsurerRouter   │
                         │  (Factory)       │
                         └────────┬─────────┘
                                  │ resolve(insurerId)
                         ┌────────▼─────────┐
                         │ InsurerRegistry   │
                         │ (@Cacheable)      │
                         └────────┬─────────┘
                                  │ lookup config → adapterType
               ┌──────────────────┼──────────────────┬──────────────────┐
               ▼                  ▼                  ▼                  ▼
      ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
      │ MockInsurer     │ │ IciciLombard   │ │ NivaBupa       │ │ BajajAllianz   │
      │ Adapter         │ │ Adapter        │ │ Adapter        │ │ Adapter        │
      │ (MOCK)          │ │ (ICICI_LOMBARD)│ │ (NIVA_BUPA)    │ │ (BAJAJ_ALLIANZ)│
      │ JSON, RT+Batch  │ │ JSON, RT only  │ │ CSV, Batch only│ │ XML, RT+Batch  │
      └─────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘
```

#### Registered Insurer Adapters

| Insurer | Adapter Type | Protocol | Real-Time | Batch | Max Batch | SLA (hrs) | Rate Limit |
|---------|-------------|----------|-----------|-------|-----------|-----------|------------|
| Mock Insurer | MOCK | JSON/REST | Yes | Yes | 100 | 4 | 60/min |
| ICICI Lombard | ICICI_LOMBARD | JSON/REST | Yes | No | — | — | 120/min |
| Niva Bupa | NIVA_BUPA | CSV/SFTP | No | Yes | 500 | 24 | — |
| Bajaj Allianz | BAJAJ_ALLIANZ | SOAP/XML | Yes | Yes | 200 | 4 | 30/min |

#### Data Format Mapping

Each adapter implements `mapToInsurerFormat()` and `mapFromInsurerFormat()` to transform endorsement data:

| Adapter | Data Mapper | Key Transformations |
|---------|-------------|-------------------|
| ICICI Lombard | IciciLombardDataMapper | employee_name→memberName, policy_id→policyNumber, ADD→ADDITION |
| Niva Bupa | NivaBupaCsvMapper | Generates CSV rows with headers: PolicyNo,MemberID,MemberName,DateOfBirth,Gender,Relationship,EndorsementType,EffectiveDate,SumInsured |
| Bajaj Allianz | BajajAllianzXmlMapper | Constructs SOAP envelope with ws:EndorsementRequest, XML-escapes special characters |

#### Per-Insurer Resilience Configuration

| Adapter | Circuit Breaker | Retry | Fallback |
|---------|----------------|-------|----------|
| ICICI Lombard | `@CircuitBreaker(name="iciciLombard")` | `@Retry(name="iciciLombard")` | Returns SubmissionResult(false, null, errorMsg) |
| Bajaj Allianz | `@CircuitBreaker(name="bajajAllianz")` | `@Retry(name="bajajAllianz")` | Returns SubmissionResult(false, null, errorMsg) |
| Niva Bupa | None (asynchronous SFTP) | None | N/A |

---

### 2.21 F-21: Reconciliation Engine (Scheduled)

**Description**: Automatically reconciles endorsements in INSURER_PROCESSING status against insurer records. Runs every 15 minutes across all active insurers.

**Trigger**: Cron `0 */15 * * * *` (every 15 minutes)

#### Inputs

None (scheduled job).

#### Processing

```
1. LOAD all active insurers from InsurerRegistry

2. FOR EACH insurer:
   a. CREATE ReconciliationRun record (status=RUNNING, startedAt=now)

   b. QUERY endorsements WHERE status=INSURER_PROCESSING AND insurerId=insurer.id

   c. RESOLVE insurer adapter via InsurerRouter

   d. FOR EACH endorsement:
      i.   IF endorsement has NO insurer reference:
           ├─ SET outcome = MISSING
           ├─ INCREMENT run.missing counter
           ├─ PUBLISH ReconciliationMissing event
           └─ NOTIFY reconciliation discrepancy

      ii.  IF insurer supports real-time AND endorsement HAS reference:
           ├─ SET outcome = MATCH
           ├─ CALL processHandler.handleConfirmation(endorsementId, reference)
           ├─ INCREMENT run.matched counter
           └─ PUBLISH ReconciliationMatched event

      iii. IF insurer is batch-only AND endorsement HAS batch ID:
           ├─ SET outcome = MATCH
           ├─ INCREMENT run.matched counter
           └─ PUBLISH ReconciliationMatched event

      iv.  IF insurer is batch-only AND endorsement has NO batch ID:
           ├─ SET outcome = PARTIAL_MATCH
           ├─ INCREMENT run.partialMatched counter
           ├─ PUBLISH ReconciliationDiscrepancy event
           └─ NOTIFY reconciliation discrepancy

      v.   CREATE ReconciliationItem record with outcome
           PERSIST item

   e. SET run.status = COMPLETED, run.completedAt = now()
   f. PERSIST run
   g. PUBLISH reconciliation metrics
   h. NOTIFY reconciliation complete (if discrepancies found)

3. LOG completion with total runs and outcomes
```

#### Reconciliation Outcomes

| Outcome | Condition | Action |
|---------|-----------|--------|
| MATCH | Endorsement has valid insurer reference | Confirm endorsement |
| PARTIAL_MATCH | Reference present but batch association missing | Flag for manual review |
| MISSING | No insurer reference found | Escalate; notify operations |
| REJECTED | Insurer explicitly rejected the endorsement | Trigger rejection flow |

#### Outputs

No direct outputs. Side effects:
- ReconciliationRun and ReconciliationItem records created in database.
- Matched endorsements transition to CONFIRMED.
- Discrepancy notifications sent.
- Reconciliation events published to Kafka.
- Metrics: `endorsement.reconciliation.completed`, `endorsement.reconciliation.matched`, `endorsement.reconciliation.discrepancies`.

---

### 2.22 F-22: Trigger Manual Reconciliation

**Description**: Triggers an on-demand reconciliation run for a specific insurer.

**Endpoint**: `POST /api/v1/reconciliation/trigger`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `insurerId` | Query | UUID | Yes | Insurer to reconcile |

#### Processing

```
1. VALIDATE insurerId exists in InsurerRegistry
2. CALL reconciliationEngine.reconcileInsurer(insurerId)
3. MAP ReconciliationRun to ReconciliationRunResponse
4. RETURN result
```

#### Outputs

**Success (200 OK)**:
```json
{
  "id": "uuid",
  "insurerId": "uuid",
  "status": "COMPLETED",
  "totalChecked": 15,
  "matched": 12,
  "partialMatched": 2,
  "rejected": 0,
  "missing": 1,
  "startedAt": "2026-03-08T10:00:00Z",
  "completedAt": "2026-03-08T10:00:05Z"
}
```

---

### 2.23 F-23: Get Reconciliation Runs

**Description**: Retrieves all reconciliation runs for a specific insurer.

**Endpoint**: `GET /api/v1/reconciliation/runs`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `insurerId` | Query | UUID | Yes | Insurer identifier |

#### Processing

```
1. QUERY reconciliation runs by insurerId
2. MAP each ReconciliationRun to ReconciliationRunResponse
3. RETURN list
```

#### Outputs

**Success (200 OK)**:
```json
[
  {
    "id": "uuid",
    "insurerId": "uuid",
    "status": "COMPLETED",
    "totalChecked": 15,
    "matched": 12,
    "partialMatched": 2,
    "rejected": 0,
    "missing": 1,
    "startedAt": "2026-03-08T10:00:00Z",
    "completedAt": "2026-03-08T10:00:05Z"
  }
]
```

---

### 2.24 F-24: Get Reconciliation Items

**Description**: Retrieves all reconciliation items for a specific reconciliation run.

**Endpoint**: `GET /api/v1/reconciliation/runs/{runId}/items`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `runId` | Path | UUID | Yes | Reconciliation run identifier |

#### Processing

```
1. QUERY reconciliation items by runId
2. MAP each ReconciliationItem to ReconciliationItemResponse
3. RETURN list
```

#### Outputs

**Success (200 OK)**:
```json
[
  {
    "id": "uuid",
    "runId": "uuid",
    "endorsementId": "uuid",
    "batchId": "uuid",
    "insurerId": "uuid",
    "employerId": "uuid",
    "outcome": "MATCH",
    "actionTaken": "Confirmed endorsement",
    "createdAt": "2026-03-08T10:00:02Z"
  }
]
```

---

### 2.25 F-25: EA Balance Optimization

**Description**: Optimizes batch assembly by intelligently sequencing endorsements to minimize EA capital lockup. Deletions are processed before additions to free balance, and endorsements are only included in batches if the employer's EA account can support them.

#### Priority Classification

| Priority | Type | Condition | Effect on Balance |
|----------|------|-----------|-------------------|
| P0_DELETION | DELETE | Always | Frees balance (credit) |
| P1_COST_NEUTRAL | UPDATE | Premium amount is zero | No balance impact |
| P2_ADDITION | ADD | Always | Consumes balance (reserve) |
| P3_PREMIUM_UPDATE | UPDATE | Premium amount > 0 | Consumes balance (reserve) |

#### Sequencing Algorithm

```
1. CLASSIFY each endorsement using EndorsementPriority.classify()
2. SORT by priority rank (P0 → P1 → P2 → P3)
3. WITHIN same priority, SORT by coverageStartDate (ascending)
4. RETURN sequenced list
```

#### Optimized Batch Construction

```
constructOptimizedBatch(endorsements, eaAccount, maxBatchSize):

1. SEQUENCE endorsements using sequenceForOptimalBalance()
2. SET projectedBalance = eaAccount.availableBalance()
3. INITIALIZE included = [], deferred = []

4. FOR EACH endorsement in sequenced order:
   a. CALCULATE impact = calculateImpact(endorsement)
      ├─ DELETE → negative premium (frees balance)
      ├─ ADD → positive premium (consumes balance)
      └─ UPDATE → positive premium (consumes balance)

   b. IF included.size() >= maxBatchSize:
      └─ ADD to deferred

   c. ELSE IF projectedBalance + (-impact) >= 0 OR impact <= 0:
      ├─ ADD to included
      └─ UPDATE projectedBalance -= impact

   d. ELSE:
      └─ ADD to deferred (insufficient balance)

5. RETURN BatchPlan(included, deferred, projectedBalance)
```

#### Balance Forecasting

```
forecastBalance(eaAccount, endorsements):

1. CALCULATE totalRequired = sum of premium amounts for ADD + UPDATE endorsements
2. CALCULATE expectedCredits = sum of premium amounts for DELETE endorsements
3. SET netRequirement = totalRequired - expectedCredits
4. SET requiredMinimum = netRequirement * (1 + SAFETY_MARGIN)  // 10% safety margin
5. SET shortfall = max(0, requiredMinimum - availableBalance)
6. SET topUpRequired = (shortfall > 0)

7. RETURN BalanceForecast(requiredMinimum, shortfall, topUpRequired)
```

If `topUpRequired` is true, a `BalanceForecastAlert` event is published with the shortfall amount.

---

### 2.26 F-26: Enhanced Kafka Event Architecture

**Description**: Phase 2 scaled the Kafka event infrastructure to support multi-insurer throughput and added reconciliation-specific events.

#### Partition Key Change

- **Phase 1**: Partition key = `endorsementId` (per-endorsement ordering)
- **Phase 2**: Partition key = `employerId` (per-employer ordering, ensures all endorsements for an employer are processed in sequence within the same partition)

#### New Event Types

| Event | Trigger | Key Fields |
|-------|---------|------------|
| ReconciliationMatched | Endorsement matched insurer records | employerId, insurerReference |
| ReconciliationDiscrepancy | Partial match detected | employerId, details |
| ReconciliationMissing | Endorsement missing from insurer | employerId, insurerId |
| BalanceForecastAlert | EA balance shortfall projected | employerId, shortfall |

All events now include `UUID employerId()` in the sealed interface contract.

#### Updated Kafka Topics

| Topic | Phase 1 Partitions | Phase 2 Partitions | Replicas | Purpose |
|-------|-------------------|-------------------|----------|---------|
| endorsement-events | 3 | 32 | 1 | All domain events |
| endorsement-commands | 3 | 32 | 1 | Command messages |
| endorsement-notifications | 1 | 8 | 1 | Notification messages |
| endorsement-reconciliation | — | 16 | 1 | Reconciliation events (new) |

---

### 2.27 F-27: List Anomalies

**Description**: Lists detected anomalies, optionally filtered by employer ID or anomaly status. Defaults to showing FLAGGED anomalies.

**Endpoint**: `GET /api/v1/intelligence/anomalies`

#### Inputs

| Parameter | Location | Type | Required | Default | Description |
|-----------|----------|------|----------|---------|-------------|
| `employerId` | Query | UUID | No | — | Filter by employer |
| `status` | Query | String | No | FLAGGED | Filter by anomaly status (FLAGGED, UNDER_REVIEW, DISMISSED, CONFIRMED_FRAUD) |

#### Processing

```
1. IF employerId provided:
   └─ QUERY anomalies by employerId
2. ELSE IF status provided:
   └─ QUERY anomalies by AnomalyStatus enum
3. ELSE:
   └─ QUERY anomalies with status = FLAGGED (default)
4. MAP each AnomalyDetection to AnomalyDetectionResponse
5. RETURN list
```

#### Outputs

**Success (200 OK)**:
```json
[
  {
    "id": "uuid",
    "endorsementId": "uuid",
    "employerId": "uuid",
    "anomalyType": "VOLUME_SPIKE",
    "score": 0.85,
    "explanation": "12 endorsements in 24h vs 2/day average (6x spike)",
    "status": "FLAGGED",
    "flaggedAt": "2026-03-08T10:00:00Z",
    "reviewedAt": null,
    "reviewerNotes": null
  }
]
```

---

### 2.28 F-28: Get Anomaly

**Description**: Retrieves a specific anomaly detection record by its identifier.

**Endpoint**: `GET /api/v1/intelligence/anomalies/{id}`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | Yes | Anomaly identifier |

#### Outputs

**Success (200 OK)**: AnomalyDetectionResponse JSON.
**Not Found (404)**: Empty body.

---

### 2.29 F-29: Review Anomaly

**Description**: Updates the status of a flagged anomaly with a review decision and optional notes.

**Endpoint**: `PUT /api/v1/intelligence/anomalies/{id}/review`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | Yes | Anomaly identifier |
| `status` | Body (JSON) | String | Yes | Target status: UNDER_REVIEW, DISMISSED, or CONFIRMED_FRAUD |
| `notes` | Body (JSON) | String | No | Reviewer notes explaining the decision |

#### Processing

```
1. LOAD anomaly by id (throw if not found)
2. VALIDATE status transition:
   └─ FLAGGED → UNDER_REVIEW, DISMISSED, CONFIRMED_FRAUD
   └─ UNDER_REVIEW → DISMISSED, CONFIRMED_FRAUD
   └─ DISMISSED, CONFIRMED_FRAUD → (terminal, no transitions)
3. UPDATE anomaly status and reviewerNotes
4. SET reviewedAt = now()
5. PERSIST anomaly
```

#### Outputs

**Success (200 OK)**: Updated AnomalyDetectionResponse JSON.

---

### 2.30 F-30: Anomaly Detection (Scheduled)

**Description**: Periodically scans recent endorsements for suspicious patterns using 5 rule-based checks.

**Trigger**: Cron `0 */5 * * * *` (every 5 minutes, configurable)
**Configuration**: `endorsement.intelligence.anomaly-detection.enabled`, `.schedule-cron`

#### Detection Rules

| Rule | Anomaly Type | Score | Detection Logic |
|------|-------------|-------|----------------|
| Volume Spike | VOLUME_SPIKE | up to 0.95 | Recent 24h count > 5x 30-day daily average |
| Add/Delete Cycling | ADD_DELETE_CYCLING | 0.85 | Same employee has both ADD and DELETE within 30 days |
| Suspicious Timing | SUSPICIOUS_TIMING | 0.75 | ADD endorsement with coverage starting within 7 days |
| Unusual Premium | UNUSUAL_PREMIUM | 0.70 | Premium > mean + 3σ (requires min 5 historical endorsements) |
| Dormancy Break | DORMANCY_BREAK | up to 0.85 | Employee with no endorsement activity for 90+ days. Score: min(0.85, 0.6 + daysSinceLastActivity/365) |

#### Processing

```
1. QUERY endorsements created in last 5 minutes with status CREATED or VALIDATED
2. FOR EACH endorsement:
   a. RUN all 5 detection rules
   b. CALCULATE maxScore across all triggered rules
   c. IF maxScore >= min-anomaly-score (default 0.7):
      ├─ CREATE AnomalyDetection record (status=FLAGGED)
      ├─ PERSIST to database
      ├─ PUBLISH EndorsementEvent.AnomalyDetected to Kafka
      └─ INCREMENT endorsement.anomaly.detected counter
3. LOG completion
```

---

### 2.31 F-31: Get Latest Balance Forecast

**Description**: Retrieves the most recent balance forecast for a specific employer-insurer pair.

**Endpoint**: `GET /api/v1/intelligence/forecasts`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `employerId` | Query | UUID | Yes | Employer identifier |
| `insurerId` | Query | UUID | Yes | Insurer identifier |

#### Outputs

**Success (200 OK)**:
```json
{
  "id": "uuid",
  "employerId": "uuid",
  "insurerId": "uuid",
  "forecastDate": "2026-04-07",
  "forecastedAmount": 280000.00,
  "actualAmount": null,
  "accuracy": null,
  "narrative": "Based on 90 days of history (45 ADD endorsements, avg ₹5,000), projected 30-day need is ₹280,000. Confidence: 72.5%.",
  "createdAt": "2026-03-08T06:00:00Z"
}
```

**Not Found (404)**: Empty body when no forecast exists.

---

### 2.32 F-32: Get Forecast History

**Description**: Retrieves forecast history for an employer.

**Endpoint**: `GET /api/v1/intelligence/forecasts/history`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `employerId` | Query | UUID | Yes | Employer identifier |

#### Outputs

**Success (200 OK)**: List of ForecastHistoryResponse objects.

---

### 2.33 F-33: Generate Balance Forecast

**Description**: Manually triggers a balance forecast for a specific employer-insurer pair.

**Endpoint**: `POST /api/v1/intelligence/forecasts/generate`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `employerId` | Query | UUID | Yes | Employer identifier |
| `insurerId` | Query | UUID | Yes | Insurer identifier |

#### Processing

```
1. QUERY historical ADD endorsements for employer-insurer (90-day window)
2. CALCULATE average premium and daily endorsement rate
3. CALCULATE base daily burn rate = avgPremium × dailyRate
4. FOR EACH day in next 30 days:
   a. APPLY day-of-week factor (Mon 1.2x ... Sun 0.2x)
   b. APPLY month factor (Apr 1.4x, Oct 1.3x, Dec 0.85x)
   c. ACCUMULATE forecasted need
5. CALCULATE confidence = min(50 + sampleSize × 0.5, 95)
6. GENERATE narrative description
7. PERSIST BalanceForecastRecord
8. PUBLISH EndorsementEvent.ForecastGenerated
9. IF forecastedNeed > currentBalance:
   ├─ PUBLISH EndorsementEvent.BalanceForecastAlert
   └─ NOTIFY employer of required top-up
```

#### Outputs

**Success (200 OK)**: BalanceForecastResponse JSON.

---

### 2.34 F-34: Balance Forecast (Scheduled)

**Description**: Generates forecasts daily for all active EA accounts.

**Trigger**: Cron `0 0 6 * * *` (daily at 6 AM, configurable)
**Configuration**: `endorsement.intelligence.balance-forecast.enabled`, `.schedule-cron`

#### Processing

```
1. QUERY all active EA accounts
2. FOR EACH account:
   a. CALL generateForecast(account.employerId, account.insurerId)
   b. IF failure → LOG warning, CONTINUE with next account
3. LOG completion
```

---

### 2.35 F-35: List Error Resolutions

**Description**: Lists error resolutions, optionally filtered by endorsement ID.

**Endpoint**: `GET /api/v1/intelligence/error-resolutions`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `endorsementId` | Query | UUID | No | Filter by endorsement |

#### Outputs

**Success (200 OK)**:
```json
[
  {
    "id": "uuid",
    "endorsementId": "uuid",
    "errorType": "DATE_FORMAT_ERROR",
    "originalValue": "07-03-1990",
    "correctedValue": "1990-03-07",
    "resolution": "Reformatted date from DD-MM-YYYY to ISO YYYY-MM-DD",
    "confidence": 0.98,
    "autoApplied": true,
    "createdAt": "2026-03-08T10:00:00Z"
  }
]
```

---

### 2.36 F-36: Get Error Resolution Stats

**Description**: Returns aggregate statistics on error resolution outcomes.

**Endpoint**: `GET /api/v1/intelligence/error-resolutions/stats`

#### Outputs

**Success (200 OK)**:
```json
{
  "totalResolutions": 42,
  "autoApplied": 28,
  "suggested": 14,
  "autoApplyRate": 66.67,
  "successCount": 25,
  "failureCount": 3,
  "successRate": 89.29
}
```

> **Success tracking** (added March 14, 2026): After auto-applying a fix, the system tracks the eventual endorsement outcome (CONFIRMED = success, REJECTED/FAILED_PERMANENT = failure). The `successCount`, `failureCount`, and `successRate` fields reflect resolution outcomes across all tracked auto-applied resolutions.

---

### 2.37 F-37: Approve Error Resolution

**Description**: Approves a suggested (non-auto-applied) error resolution, applying the correction.

**Endpoint**: `POST /api/v1/intelligence/error-resolutions/{id}/approve`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | Yes | Error resolution identifier |

#### Outputs

**Success (200 OK)**: Empty body.

---

### 2.38 F-38: Automated Error Resolution (Event-Driven)

**Description**: Automatically attempts to resolve insurer submission errors when endorsements are rejected. Integrates into the rejection/submission flow.

**Trigger**: Called from `ProcessEndorsementHandler` when insurer returns a validation error.

#### Error Resolution Rules

| Error Type | Pattern Match | Confidence | Auto-Apply | Resolution |
|-----------|--------------|-----------|-----------|-----------|
| DATE_FORMAT_ERROR | "date", "dob", "format" in error | 0.98 | Yes | Reformat DD-MM-YYYY → YYYY-MM-DD |
| MEMBER_ID_FORMAT_ERROR | "member", "id format", "invalid id" | 0.96 | Yes | Apply PLM- prefix + 8-char UUID |
| MISSING_FIELD_ERROR | "required", "missing", "mandatory" | 0.90 | No | Provide sensible defaults |
| PREMIUM_MISMATCH_ERROR | "premium", "mismatch", "amount" | 0.85 | No | Recalculate with 5% adjustment |
| UNKNOWN_ERROR | Fallback | 0.30 | No | Suggest manual review |

#### Processing

```
1. RECEIVE error message from insurer submission failure
2. CLASSIFY error type via pattern matching
3. GENERATE correction based on error type rules
4. CALCULATE confidence score
5. CREATE ErrorResolution record
6. IF confidence >= auto-apply-threshold (0.95) AND retryCount < max-auto-retries (2):
   ├─ APPLY correction to endorsement data
   ├─ SET autoApplied = true
   ├─ RESUBMIT endorsement to insurer
   └─ PUBLISH EndorsementEvent.ErrorAutoResolved
7. ELSE:
   ├─ SET autoApplied = false
   └─ PUBLISH EndorsementEvent.ErrorResolutionSuggested
8. PERSIST ErrorResolution
```

---

### 2.39 F-39: Get Process Mining Metrics

**Description**: Retrieves workflow transition metrics, optionally filtered by insurer.

**Endpoint**: `GET /api/v1/intelligence/process-mining/metrics`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `insurerId` | Query | UUID | No | Filter by insurer (null returns all) |

#### Outputs

**Success (200 OK)**:
```json
[
  {
    "id": "uuid",
    "insurerId": "uuid",
    "fromStatus": "CREATED",
    "toStatus": "VALIDATED",
    "avgDurationMs": 45000,
    "p95DurationMs": 120000,
    "p99DurationMs": 300000,
    "sampleCount": 150,
    "happyPathPct": 92.0,
    "calculatedAt": "2026-03-08T03:00:00Z"
  }
]
```

---

### 2.40 F-40: Get Process Mining Insights

**Description**: Returns bottleneck insights derived from process mining analysis.

**Endpoint**: `GET /api/v1/intelligence/process-mining/insights`

#### Outputs

**Success (200 OK)**:
```json
[
  {
    "insurerId": "uuid",
    "insurerName": "ICICI Lombard",
    "insightType": "BOTTLENECK",
    "insight": "Bottleneck detected: VALIDATED → SUBMITTED averages 3.2 hours (p95: 7.1 hours). Based on 42 samples.",
    "calculatedAt": "2026-03-08T03:00:00Z"
  }
]
```

---

### 2.41 F-41: Get STP Rate

**Description**: Returns the straight-through processing rate (% of endorsements completing without retry or rejection).

**Endpoint**: `GET /api/v1/intelligence/process-mining/stp-rate`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `insurerId` | Query | UUID | No | Filter by insurer (null returns overall + per-insurer breakdown) |

#### Outputs

**Success (200 OK)**:
```json
{
  "overallStpRate": 94.7,
  "perInsurerStpRate": {
    "33333333-...": 96.2,
    "44444444-...": 89.1,
    "55555555-...": 93.5
  }
}
```

---

### 2.42 F-42: Trigger Process Mining Analysis

**Description**: Manually triggers a full process mining analysis across all insurers.

**Endpoint**: `POST /api/v1/intelligence/process-mining/analyze`

#### Processing

```
1. FOR EACH active insurer:
   a. QUERY all EndorsementEvent records for insurer
   b. GROUP events by endorsementId (timeline per endorsement)
   c. SORT by occurredAt timestamp
   d. FOR EACH endorsement timeline:
      ├─ RECORD each transition (fromEvent → toEvent)
      ├─ CALCULATE duration between transitions
      ├─ DETERMINE happy path (no RETRY/REJECTED events)
   e. AGGREGATE statistics per transition:
      ├─ avgDurationMs (mean)
      ├─ p95DurationMs (95th percentile)
      ├─ p99DurationMs (99th percentile)
      ├─ sampleCount
      └─ happyPathPct
   f. DELETE previous metrics for insurer
   g. PERSIST new ProcessMiningMetric records
2. DETECT bottlenecks: transitions where p95 > 2x average AND sampleCount >= 5
3. RETURN 202 Accepted
```

#### Outputs

**Success (202 Accepted)**: Empty body. Analysis runs asynchronously.

---

### 2.43 F-43: Process Mining (Scheduled)

**Description**: Runs process mining analysis daily for all active insurers.

**Trigger**: Cron `0 0 3 * * *` (daily at 3 AM, configurable)
**Configuration**: `endorsement.intelligence.process-mining.enabled`, `.schedule-cron`

---

### 2.44 F-44: Smart Batch Optimization

**Description**: Constraint-based batch optimizer that replaces simple FIFO chunking in the batch assembly scheduler. Uses composite scoring to maximize batch efficiency.

**Trigger**: Called during batch assembly (F-09) when `endorsement.intelligence.batch-optimizer.enabled` is true.

#### Algorithm

```
1. CLASSIFY each endorsement by priority:
   └─ P0_DELETION (rank 0) → P1_COST_NEUTRAL (rank 1) → P2_ADDITION (rank 2) → P3_PREMIUM_UPDATE (rank 3)

2. FOR EACH endorsement, CALCULATE composite score:
   a. urgencyScore = coverageStartDate proximity (0.0-1.0)
   b. eaImpactScore = premiumAmount relative to available balance (0.0-1.0)
   c. compositeScore = (urgencyScore × 0.6) + (eaImpactScore × 0.4)

3. SORT by: priority rank ASC, compositeScore DESC

4. CONSTRUCT batch respecting constraints:
   ├─ EA balance constraint (projected balance >= 0)
   ├─ Insurer max batch size constraint
   └─ Deletions free balance before additions consume it

5. RETURN optimized endorsement ordering
```

#### Outputs

Endorsed ordering for batch assembly. If optimization fails, falls back to standard priority-based sequencing.

---

### 2.45 F-45: Phase 3 Event Types

**Description**: Phase 3 added 6 new domain event types to the sealed `EndorsementEvent` interface.

#### New Event Types

| Event | Trigger | Key Fields |
|-------|---------|------------|
| AnomalyDetected | Anomaly score >= threshold | anomalyType, anomalyScore, explanation |
| ForecastGenerated | Forecast created for employer | forecastedNeed, daysAhead, narrative |
| BatchOptimized | Smart optimizer applied to batch | batchId, optimizationStrategy, savedAmount |
| ErrorAutoResolved | Error auto-resolved (confidence >= threshold) | errorType, resolution, autoApplied |
| ErrorResolutionSuggested | Error resolution below auto-apply threshold | errorType, suggestedFix, confidence |
| ProcessMiningInsight | Process mining analysis generated insight | insightType, insight |

Total event types in sealed interface: 22 (12 MVP + 4 Phase 2 + 6 Phase 3).

---

### 2.46 F-46: Employer Health Score

**Description**: Calculates a composite health score for an employer based on endorsement success rate, anomaly frequency, EA balance health, and reconciliation outcomes.

**Endpoint**: `GET /api/v1/intelligence/employers/{employerId}/health-score`

#### Processing

```
1. COUNT confirmed, rejected, failed endorsements for employer
2. CALCULATE endorsementSuccessRate = confirmed / (confirmed + rejected + failed) * 100
3. COUNT anomalies flagged in last 90 days
4. CALCULATE anomalyScore: 0 anomalies = 100, 1-2 = 80, 3-5 = 60, 6+ = 30
5. FIND all EA accounts for employer
6. CALCULATE balanceHealthScore = % of accounts with positive balance * 100
7. SET reconciliationScore = 100 (extensible placeholder)
8. COMPUTE overallScore = 0.4 * successRate + 0.2 * anomalyScore + 0.2 * balanceHealth + 0.2 * reconciliation
9. CLASSIFY riskLevel: LOW (>= 80), MEDIUM (60-79), HIGH (< 60)
```

#### Outputs

**Success (200 OK)**:
```json
{
  "overallScore": 85.0,
  "riskLevel": "LOW",
  "endorsementSuccessRate": 92.0,
  "anomalyScore": 80.0,
  "balanceHealthScore": 100.0,
  "reconciliationScore": 100.0,
  "calculatedAt": "2026-03-14T10:00:00Z"
}
```

---

### 2.47 F-47: Insurer Benchmarks

**Description**: Aggregates cross-insurer performance metrics for benchmarking.

**Endpoint**: `GET /api/v1/intelligence/benchmarks`

#### Processing

```
1. QUERY latest ProcessMiningMetric records for all insurers
2. FOR EACH insurer:
   a. COMPUTE average processing time from avg_duration_ms
   b. EXTRACT p95 and p99 latencies
   c. COMPUTE STP rate from happy_path_pct
   d. COUNT total samples
3. SORT by STP rate descending (best performers first)
4. RETURN list of InsurerBenchmark records
```

#### Outputs

**Success (200 OK)**: List of `InsurerBenchmarkResponse` objects with `insurerId`, `insurerName`, `avgProcessingTimeMs`, `p95ProcessingTimeMs`, `p99ProcessingTimeMs`, `stpRate`, `totalSamples`.

---

### 2.48 F-48: Create Insurer Configuration

**Description**: Creates a new insurer configuration record for onboarding a new insurance provider.

**Endpoint**: `POST /api/v1/insurers`

#### Inputs

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `insurerName` | String | Yes | Human-readable insurer name |
| `insurerCode` | String | Yes | Short identifier (e.g., `ICICI_LOMBARD`) |
| `adapterType` | String | Yes | Adapter implementation type |
| `supportsRealTime` | boolean | No | Real-time submission capability (default: false) |
| `supportsBatch` | boolean | No | Batch submission capability (default: false) |
| `maxBatchSize` | int | No | Max endorsements per batch (default: 100) |
| `batchSlaHours` | long | No | Batch SLA in hours (default: 24) |
| `rateLimitPerMinute` | int | No | Rate limit (default: 60) |
| `dataFormat` | String | No | Data format: JSON, CSV, XML (default: JSON) |

#### Outputs

**Success (201 Created)**: `InsurerConfigurationResponse` with generated `insurerId`.
**Error (409 Conflict)**: Duplicate `insurerCode`.

---

### 2.49 F-49: Update Insurer Configuration

**Description**: Updates an existing insurer configuration. Triggers cache eviction in `InsurerRegistry`.

**Endpoint**: `PUT /api/v1/insurers/{insurerId}`

#### Processing

```
1. LOAD insurer configuration by insurerId
2. UPDATE mutable fields (name, rate limits, active status, etc.)
3. SAVE via InsurerRegistry.updateConfiguration() — triggers @CacheEvict
4. RETURN updated configuration
```

---

### 2.50 F-50: Get Audit Logs

**Description**: Retrieves audit log entries with optional filtering by entity type.

**Endpoint**: `GET /api/v1/audit-logs`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `entityType` | Query | String | No | Filter by entity type (ENDORSEMENT, EA_ACCOUNT, etc.) |
| `page` | Query | int | No | Page number (default: 0) |
| `size` | Query | int | No | Page size (default: 20) |

#### Outputs

**Success (200 OK)**: Paginated list of audit log entries with `action`, `entityType`, `entityId`, `details`, `timestamp`.

---

### 2.51 F-51: Stuck Endorsement Retry (Scheduled)

**Description**: Detects endorsements stuck in non-terminal intermediate states (e.g., `SUBMITTED_REALTIME`, `INSURER_PROCESSING`) beyond a configurable threshold and triggers retry.

**Trigger**: Configurable cron via `endorsement.stuck-retry.schedule-cron`

#### Processing

```
1. QUERY endorsements in intermediate states WHERE updatedAt < (now - threshold)
2. FOR EACH stuck endorsement:
   a. LOG warning with endorsement details
   b. ATTEMPT resubmission via ProcessEndorsementHandler
   c. PUBLISH StuckEndorsementRetried event
3. INCREMENT endorsement.stuck.retry counter
```

---

### 2.52 F-52: Data Retention Cleanup (Scheduled)

**Description**: Periodically archives or removes aged records to maintain database performance.

**Trigger**: Configurable cron via `endorsement.data-retention.schedule-cron`

#### Processing

```
1. ARCHIVE endorsements older than retention period to endorsements_archive table
2. ARCHIVE ea_transactions older than retention period
3. ARCHIVE endorsement_events older than retention period
4. DELETE expired anomaly detections beyond retention window
5. DELETE aged balance forecast records
6. DELETE stale process mining metrics
7. LOG summary of archived/deleted record counts
```

### 2.53 F-53: Get STP Rate Trend

**Description**: Returns historical daily STP rate snapshots for a specific insurer, enabling trend analysis and visualization.

**Endpoint**: `GET /api/v1/intelligence/process-mining/stp-rate/trend`

#### Inputs

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `insurerId` | Query | UUID | No | Insurer identifier (returns all if omitted) |
| `days` | Query | Integer | No | Number of days of history (default: 30) |

#### Outputs

**Success (200 OK)**:
```json
{
  "insurerId": "550e8400-e29b-41d4-a716-446655440000",
  "dataPoints": [
    {
      "date": "2026-02-12",
      "stpRate": 91.2,
      "total": 1502,
      "stp": 1370
    },
    {
      "date": "2026-02-13",
      "stpRate": 92.5,
      "total": 1480,
      "stp": 1369
    }
  ],
  "currentRate": 92.1,
  "changePercent": 0.9
}
```

#### Processing

```
1. QUERY stp_rate_snapshots WHERE insurer_id = ? AND snapshot_date >= (today - days)
2. ORDER BY snapshot_date ASC
3. CALCULATE changePercent = latest rate - earliest rate
4. RETURN StpRateTrendResponse
```

**Data Source**: `stp_rate_snapshots` table, populated daily by `ProcessMiningScheduler`.

---

### 2.54 F-54: Error Resolution Outcome Tracking

**Description**: Automatically tracks whether error resolutions led to successful endorsement confirmation or failure. Hooks into endorsement state transitions at CONFIRMED, REJECTED, and FAILED_PERMANENT.

**Trigger**: Invoked by `ProcessEndorsementHandler` when an endorsement reaches a terminal or near-terminal state.

#### Processing

```
1. ON endorsement CONFIRMED:
   a. FIND error_resolutions WHERE endorsement_id = ? AND outcome IS NULL
   b. SET outcome = 'SUCCESS', outcome_at = now(), outcome_endorsement_status = 'CONFIRMED'
2. ON endorsement REJECTED:
   a. FIND error_resolutions WHERE endorsement_id = ? AND outcome IS NULL
   b. SET outcome = 'FAILURE', outcome_at = now(), outcome_endorsement_status = 'REJECTED'
3. ON endorsement FAILED_PERMANENT:
   a. FIND error_resolutions WHERE endorsement_id = ? AND outcome IS NULL
   b. SET outcome = 'FAILURE', outcome_at = now(), outcome_endorsement_status = 'FAILED_PERMANENT'
```

---

## 3. External Interfaces

### 3.1 User Interfaces

The service exposes a RESTful API. A separate React frontend application (located in the `frontend/` directory) provides the web-based user interface. The API follows these conventions:

- **Base Path**: `/api/v1`
- **Content Type**: `application/json`
- **Error Format**: RFC 7807 Problem Detail (`application/problem+json`)
- **API Documentation**: OpenAPI 3.0 via Springdoc at `/swagger-ui` and `/api-docs`

### 3.2 Software Interfaces

#### 3.2.1 Insurer Integration (InsurerPort)

The insurer interface defines the contract for communicating with insurance providers.

**Methods**:

| Method | Description | Returns |
|--------|-------------|---------|
| `submitRealTime(endorsementId, data)` | Submit single endorsement for immediate processing | `SubmissionResult(success, insurerReference, errorMessage)` |
| `submitBatch(batchId, endorsements)` | Submit batch of endorsements | `String` (insurer batch reference) |
| `checkBatchStatus(insurerBatchRef)` | Poll batch processing status | `BatchStatusResult(status, results)` |
| `getCapabilities()` | Query insurer's supported features | `InsurerCapabilities(supportsRealTime, supportsBatch, maxBatchSize, batchSlaHours, rateLimitPerMinute)` |

**Current Implementations** (all simulated):

| Adapter | Class | Protocol | Simulated Latency |
|---------|-------|----------|-------------------|
| Mock Insurer | `MockInsurerAdapter` | REST/JSON | 100ms |
| ICICI Lombard | `IciciLombardAdapter` | REST/JSON | 150ms |
| Niva Bupa | `NivaBupaAdapter` | CSV/SFTP | 200ms (upload) |
| Bajaj Allianz | `BajajAllianzAdapter` | SOAP/XML | 250ms (real-time), 300ms (batch) |

**Resilience Patterns Applied**:

| Instance | Circuit Breaker | Retry | Fallback |
|----------|----------------|-------|----------|
| `insurerSubmission` (Mock) | 50% failure, 30s open, window=10 | 3 attempts, 2s base, 2x backoff | SubmissionResult(false) |
| `iciciLombard` | Per-insurer config | Per-insurer config | SubmissionResult(false) |
| `bajajAllianz` | Per-insurer config | Per-insurer config | SubmissionResult(false) |

#### 3.2.2 PostgreSQL

- **Driver**: PostgreSQL JDBC 42.7.3
- **Connection**: `jdbc:postgresql://localhost:5432/endorsements`
- **ORM**: Spring Data JPA with Hibernate
- **DDL Strategy**: `validate` (Flyway manages schema)
- **Connection Pool**: HikariCP (Spring Boot default)

#### 3.2.3 Apache Kafka

- **Bootstrap Servers**: `localhost:9092`
- **Producer Acks**: `all` (strongest durability guarantee)
- **Consumer Group**: `endorsement-service`
- **Consumer Offset Reset**: `earliest`
- **Serialization**: String keys and values (JSON payload)

#### 3.2.4 Redis

- **Host**: `localhost:6379`
- **Usage**: Application-level caching
- **Cache Spec**: `maximumSize=1000, expireAfterWrite=60s` (Caffeine)

### 3.3 Communication Interfaces

| Protocol | Port | Purpose |
|----------|------|---------|
| HTTP/REST | 8080 | Application API |
| PostgreSQL | 5432 | Primary data store |
| Kafka | 9092 | Event streaming |
| Redis | 6379 | Caching |
| OTLP (Jaeger) | 4317 | Distributed tracing export |
| Prometheus | 8080 `/actuator/prometheus` | Metrics scrape endpoint |
| Logstash | 5000 | Structured log aggregation (TCP) |
| Elasticsearch | 9200 | Search and log storage |
| Grafana | 3000 | Dashboard visualization |
| WebSocket | 8080 `/ws/**` | Real-time event broadcast |

### 3.4 Performance Requirements

| Metric | Target |
|--------|--------|
| p50 Response Time (all endpoints) | < 200ms |
| p95 Response Time (all endpoints) | < 500ms |
| p99 Response Time (all endpoints) | < 1500ms |
| p95 Create Endorsement | < 500ms |
| p95 Get Endorsement | < 100ms |
| p95 Submit Endorsement | < 800ms |
| Success Rate (normal load) | > 99.9% |
| Success Rate (stress load) | > 95% |
| Concurrent EA contention | 100% success (200 users) |
| Sustained load | 80 users/sec for 15 minutes |
| Spike recovery | < 5% failure, max response < 10s |
| Multi-insurer throughput | 100K endorsements/day across 5 insurers |
| Per-insurer p95 latency | < 500ms (ICICI), < 1000ms (Bajaj), < 500ms (batch submission for Niva) |
| Reconciliation run time | < 30s per insurer per cycle |

### 3.5 Design Constraints

| Constraint | Description |
|------------|-------------|
| Hexagonal Architecture | Domain logic is isolated from infrastructure through ports and adapters |
| CQRS Pattern | Read and write operations are separated (`EndorsementQueryHandler` vs `CreateEndorsementHandler` / `ProcessEndorsementHandler`) |
| Event-Driven | All state transitions publish domain events to Kafka |
| Optimistic Locking | `version` field prevents concurrent modification conflicts |
| Virtual Threads | Java 21 virtual threads for high-concurrency I/O operations |

---

## 4. Attributes

### 4.1 Security

| Control | Implementation | Status |
|---------|----------------|--------|
| Authentication | None (all requests permitted) — JWT + OAuth2 architecture documented in [Product Evolution Vision](deliverables/vision/Product_Evolution_Vision.md#9-authentication--authorization-architecture) | MVP — auth not required by design challenge |
| CSRF Protection | Disabled (stateless API) | By design |
| Session Management | `SessionCreationPolicy.STATELESS` | Active |
| Request Tracing | `X-Request-Id` header (generated if absent) + MDC propagation | Active |
| Input Validation | Jakarta Bean Validation on all request DTOs | Active |
| SQL Injection Prevention | JPA parameterized queries | Active |
| Financial Integrity | EA balance cannot go negative (domain model enforcement) | Active |
| Rate Limiting | IP-based sliding window via `RateLimitingFilter` | Active |
| Audit Logging | `AuditLoggingAspect` captures mutations to `audit_logs` table | Active |

> **Note:** Authentication and authorization (JWT, RBAC, OAuth2/OIDC) are not requirements of the design challenge. A comprehensive AuthZ/AuthN architecture and strategy (5 roles, tenant isolation, 15-day implementation plan) is documented in the [Product Evolution Vision](deliverables/vision/Product_Evolution_Vision.md#9-authentication--authorization-architecture) for production deployment readiness.

### 4.2 Reliability and Availability

| Mechanism | Configuration |
|-----------|---------------|
| Circuit Breaker | 50% failure threshold, 30s open state, sliding window of 10 |
| Retry | 3 attempts, 2s base, 2x exponential backoff |
| Idempotency | Unique key prevents duplicate processing |
| Optimistic Locking | Version field detects concurrent modifications |
| Endorsement Retry | Up to 3 business-level retries for rejected endorsements |
| Health Checks | `/actuator/health` with database and circuit breaker indicators |
| Kafka Acks | `all` — message acknowledged by all in-sync replicas |

### 4.3 Configurability

All key parameters are externalized via `application.yml` and overridable through environment variables or system properties:

| Category | Properties |
|----------|-----------|
| Database | `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` |
| Kafka | `spring.kafka.bootstrap-servers`, `spring.kafka.consumer.group-id` |
| Redis | `spring.data.redis.host`, `spring.data.redis.port` |
| Batch Schedule | `endorsement.batch.schedule-cron` (default: every 15 min) |
| Retry | `endorsement.retry.max-attempts` (default: 3), `endorsement.retry.backoff-ms` (default: 5000) |
| Coverage Cleanup | `endorsement.provisional-coverage.max-days` (default: 30) |
| Server Port | `server.port` (default: 8080) |
| Tracing Sample Rate | `management.tracing.sampling.probability` (default: 1.0) |
| Circuit Breaker | `resilience4j.circuitbreaker.instances.insurerSubmission.*`, `.iciciLombard.*`, `.bajajAllianz.*` |
| Reconciliation Schedule | `0 */15 * * * *` (every 15 minutes, same cron as batch assembly) |
| EA Safety Margin | 10% (hardcoded in `EABalanceCalculator.SAFETY_MARGIN`) |
| Anomaly Detection | `endorsement.intelligence.anomaly-detection.enabled` (default: true), `.schedule-cron` (default: every 5 min), `.min-anomaly-score` (default: 0.7), `.volume-spike-threshold` (default: 0.5), `.cycling-window-days` (default: 30) |
| Balance Forecast | `endorsement.intelligence.balance-forecast.enabled` (default: true), `.schedule-cron` (default: daily 6 AM), `.forecast-days-ahead` (default: 30), `.alert-days-ahead` (default: 7) |
| Batch Optimizer | `endorsement.intelligence.batch-optimizer.enabled` (default: true) |
| Error Resolution | `endorsement.intelligence.error-resolution.enabled` (default: true), `.auto-apply-threshold` (default: 0.95), `.max-auto-retries` (default: 2) |
| Process Mining | `endorsement.intelligence.process-mining.enabled` (default: true), `.schedule-cron` (default: daily 3 AM) |

### 4.4 Observability

#### 4.4.1 Custom Metrics (Micrometer / Prometheus)

| Metric Name | Type | Tags | Description |
|-------------|------|------|-------------|
| `endorsement.created` | Counter | `type` | Endorsements created by type |
| `endorsement.state.transition` | Counter | `from`, `to` | State machine transitions |
| `endorsement.insurer.submission.duration` | Timer | `mode`, `result` | Insurer API call latency |
| `endorsement.batch.size` | Summary | — | Batch sizes distribution |
| `endorsement.ea.reservation` | Counter | `result` | EA reservation outcomes |
| `endorsement.kafka.publish` | Counter | `result`, `eventType` | Kafka publishing outcomes |
| `endorsement.scheduler.duration` | Timer | `scheduler`, `result` | Scheduler execution time |
| `endorsement.scheduler.execution` | Counter | `scheduler`, `result` | Scheduler run count |
| `endorsement.active.count` | Gauge | `status` | Active endorsements per status (11 gauges) |
| `endorsement.error` | Counter | `type` | Error occurrences by category |
| `endorsement.coverage.expired` | Counter | — | Expired provisional coverages |
| `endorsement.insurer.mock.duration` | Timer | `method` | Mock insurer adapter timing |
| `endorsement.insurer.icici.duration` | Timer | `method` | ICICI Lombard adapter timing |
| `endorsement.insurer.nivabupa.duration` | Timer | `method` | Niva Bupa adapter timing |
| `endorsement.insurer.bajaj.duration` | Timer | `method` | Bajaj Allianz adapter timing |
| `endorsement.reconciliation.completed` | Counter | — | Reconciliation runs completed |
| `endorsement.reconciliation.matched` | Gauge | — | Matched endorsements in latest run |
| `endorsement.reconciliation.discrepancies` | Gauge | — | Discrepancies in latest run |
| `endorsement.reconciliation.error` | Counter | `insurerCode` | Reconciliation errors per insurer |
| `endorsement.anomaly.detected` | Counter | `anomalyType`, `employerId` | Anomalies flagged by type |
| `endorsement.anomaly.score` | Summary | `anomalyType` | Anomaly score distribution |
| `endorsement.forecast.generated` | Counter | `employerId` | Forecasts generated |
| `endorsement.forecast.shortfall.detected` | Counter | `employerId` | Shortfall alerts triggered |
| `endorsement.error.auto_resolved` | Counter | `errorType`, `insurerId` | Auto-resolved errors |
| `endorsement.error.suggested` | Counter | `errorType` | Error resolution suggestions |
| `endorsement.error.resolution.confidence` | Summary | `errorType` | Confidence score distribution |
| `endorsement.process.stp_rate` | Gauge | `insurerId` | STP rate per insurer |
| `endorsement.process.avg_lifecycle_hours` | Gauge | `insurerId` | Average lifecycle hours |
| `endorsement.batch.optimization.savings` | Summary | — | Batch optimization savings |

**Common Tag**: `service=endorsement-service`
**Histogram Percentiles**: p50, p95, p99

#### 4.4.2 Distributed Tracing (OpenTelemetry)

- **Sampling Rate**: 100%
- **Baggage Propagation**: `endorsementId`, `employerId` propagated across service boundaries
- **MDC Integration**: `traceId`, `spanId`, `requestId` in all log entries

#### 4.4.3 Structured Logging

- **MDC Fields**: `requestId`, `traceId`, `spanId`, `endorsementId`, `employerId`, `kafkaEventType`
- **Request Logging Filter**: Logs method, URI, status, duration (ms), response body size
- **Excluded Paths**: `/actuator/*`, `/swagger-ui*`, `/api-docs*`, `/webjars*`

#### 4.4.4 Actuator Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Application health (DB, circuit breaker) |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/prometheus` | Prometheus scrape endpoint |
| `/actuator/circuitbreakers` | Circuit breaker states |
| `/actuator/retries` | Retry statistics |

---

## 5. Data Model

### 5.1 Database Schema

#### 5.1.1 endorsements

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Unique identifier |
| `employer_id` | UUID | NOT NULL | Employer organization |
| `employee_id` | UUID | NOT NULL | Employee being endorsed |
| `insurer_id` | UUID | NOT NULL | Insurance provider |
| `policy_id` | UUID | NOT NULL | Insurance policy |
| `type` | VARCHAR(20) | NOT NULL | ADD, DELETE, UPDATE |
| `status` | VARCHAR(30) | NOT NULL | Current lifecycle status |
| `coverage_start_date` | DATE | NOT NULL | Coverage start |
| `coverage_end_date` | DATE | — | Coverage end |
| `employee_data` | JSONB | NOT NULL | Employee details |
| `premium_amount` | DECIMAL(12,2) | — | Premium amount |
| `batch_id` | UUID | — | Assigned batch |
| `insurer_reference` | VARCHAR(100) | — | Insurer-assigned reference |
| `retry_count` | INT | DEFAULT 0 | Number of retry attempts |
| `failure_reason` | TEXT | — | Last failure reason |
| `idempotency_key` | VARCHAR(100) | UNIQUE | Deduplication key |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Last modification |
| `version` | INT | DEFAULT 1 | Optimistic lock version |

**Indexes**: employer_id, employee_id, status, batch_id, insurer_id, created_at

#### 5.1.2 ea_accounts

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `employer_id` | UUID | PK (composite) | Employer identifier |
| `insurer_id` | UUID | PK (composite) | Insurer identifier |
| `balance` | DECIMAL(12,2) | NOT NULL, DEFAULT 0 | Total account balance |
| `reserved` | DECIMAL(12,2) | NOT NULL, DEFAULT 0 | Reserved for pending endorsements |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Last modification |

#### 5.1.3 ea_transactions

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Auto-incrementing identifier |
| `employer_id` | UUID | NOT NULL | Employer identifier |
| `insurer_id` | UUID | NOT NULL | Insurer identifier |
| `endorsement_id` | UUID | FK → endorsements(id) | Related endorsement |
| `type` | VARCHAR(20) | NOT NULL | DEBIT, CREDIT, RESERVE, RELEASE, TOP_UP |
| `amount` | DECIMAL(12,2) | NOT NULL | Transaction amount |
| `balance_after` | DECIMAL(12,2) | NOT NULL | Balance snapshot after transaction |
| `description` | TEXT | — | Transaction description |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Transaction timestamp |

**Indexes**: (employer_id, insurer_id), endorsement_id

#### 5.1.4 endorsement_batches

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Batch identifier |
| `insurer_id` | UUID | NOT NULL | Target insurer |
| `status` | VARCHAR(20) | NOT NULL | ASSEMBLING, SUBMITTED, PROCESSING, PARTIAL_COMPLETE, COMPLETE, FAILED |
| `endorsement_count` | INT | NOT NULL | Number of endorsements in batch |
| `total_premium` | DECIMAL(12,2) | — | Sum of premiums |
| `submitted_at` | TIMESTAMPTZ | — | Submission timestamp |
| `sla_deadline` | TIMESTAMPTZ | — | SLA expiry timestamp |
| `insurer_batch_ref` | VARCHAR(100) | — | Insurer-assigned batch reference |
| `response_data` | JSONB | — | Raw insurer response |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Creation timestamp |

**Indexes**: insurer_id, status

#### 5.1.5 provisional_coverages

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Coverage record identifier |
| `endorsement_id` | UUID | NOT NULL, FK → endorsements(id) | Related endorsement |
| `employee_id` | UUID | NOT NULL | Covered employee |
| `employer_id` | UUID | NOT NULL | Employer organization |
| `coverage_start` | DATE | NOT NULL | Coverage effective date |
| `coverage_type` | VARCHAR(20) | DEFAULT 'PROVISIONAL' | PROVISIONAL or CONFIRMED |
| `confirmed_at` | TIMESTAMPTZ | — | When coverage was confirmed |
| `expired_at` | TIMESTAMPTZ | — | When coverage expired |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Creation timestamp |

**Indexes**: endorsement_id, employee_id, employer_id

#### 5.1.6 endorsement_events

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Auto-incrementing identifier |
| `endorsement_id` | UUID | NOT NULL, FK → endorsements(id) | Related endorsement |
| `event_type` | VARCHAR(50) | NOT NULL | Event type identifier |
| `event_data` | JSONB | NOT NULL | Full event payload |
| `actor` | VARCHAR(100) | — | Who triggered the event |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Event timestamp |

**Indexes**: endorsement_id, event_type

#### 5.1.7 insurer_configurations

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `insurer_id` | UUID | PK | Unique insurer identifier |
| `insurer_name` | VARCHAR(100) | NOT NULL | Human-readable insurer name |
| `insurer_code` | VARCHAR(20) | NOT NULL, UNIQUE | Short code (e.g., ICICI_LOMBARD) |
| `adapter_type` | VARCHAR(30) | NOT NULL | Adapter implementation type |
| `supports_real_time` | BOOLEAN | NOT NULL, DEFAULT false | Real-time submission capability |
| `supports_batch` | BOOLEAN | NOT NULL, DEFAULT false | Batch submission capability |
| `max_batch_size` | INT | NOT NULL, DEFAULT 100 | Maximum endorsements per batch |
| `batch_sla_hours` | BIGINT | NOT NULL, DEFAULT 24 | SLA in hours for batch processing |
| `rate_limit_per_min` | INT | NOT NULL, DEFAULT 60 | Rate limit for API calls |
| `api_base_url` | VARCHAR(500) | — | Base URL for insurer API |
| `auth_type` | VARCHAR(30) | — | Authentication type (OAUTH2, SSH_KEY, WS_SECURITY) |
| `auth_config` | JSONB | — | Authentication configuration details |
| `data_format` | VARCHAR(10) | NOT NULL, DEFAULT 'JSON' | Data format (JSON, CSV, XML) |
| `retry_max_attempts` | INT | NOT NULL, DEFAULT 3 | Maximum retry attempts |
| `retry_wait_ms` | BIGINT | NOT NULL, DEFAULT 2000 | Milliseconds between retries |
| `circuit_breaker_config` | JSONB | — | Circuit breaker configuration |
| `active` | BOOLEAN | NOT NULL, DEFAULT true | Whether configuration is active |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Last modification |

**Indexes**: insurer_code, active

**Seeded Data** (via V6 + V7 migrations):

| Insurer | ID | Code | Adapter | Format | Auth |
|---------|-----|------|---------|--------|------|
| Mock Insurer | 22222222-...-222222222222 | MOCK | MOCK | JSON | API_KEY |
| ICICI Lombard | 33333333-...-333333333333 | ICICI_LOMBARD | ICICI_LOMBARD | JSON | OAUTH2 |
| Niva Bupa | 44444444-...-444444444444 | NIVA_BUPA | NIVA_BUPA | CSV | SSH_KEY |
| Bajaj Allianz | 55555555-...-555555555555 | BAJAJ_ALLIANZ | BAJAJ_ALLIANZ | XML | WS_SECURITY |

#### 5.1.8 reconciliation_runs

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Run identifier |
| `insurer_id` | UUID | NOT NULL, FK → insurer_configurations | Target insurer |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'RUNNING' | RUNNING or COMPLETED |
| `total_checked` | INT | NOT NULL, DEFAULT 0 | Total endorsements checked |
| `matched` | INT | NOT NULL, DEFAULT 0 | Fully matched count |
| `partial_matched` | INT | NOT NULL, DEFAULT 0 | Partially matched count |
| `rejected` | INT | NOT NULL, DEFAULT 0 | Rejected count |
| `missing` | INT | NOT NULL, DEFAULT 0 | Missing from insurer count |
| `started_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Run start time |
| `completed_at` | TIMESTAMPTZ | — | Run completion time |

**Indexes**: insurer_id, status

#### 5.1.9 reconciliation_items

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Item identifier |
| `run_id` | UUID | NOT NULL, FK → reconciliation_runs | Parent run |
| `endorsement_id` | UUID | NOT NULL, FK → endorsements | Endorsement being reconciled |
| `batch_id` | UUID | — | Associated batch (if batch submission) |
| `insurer_id` | UUID | NOT NULL | Insurer identifier |
| `employer_id` | UUID | NOT NULL | Employer identifier |
| `outcome` | VARCHAR(20) | NOT NULL | MATCH, PARTIAL_MATCH, REJECTED, MISSING |
| `sent_data` | JSONB | — | Data sent to insurer |
| `confirmed_data` | JSONB | — | Data confirmed by insurer |
| `discrepancy_details` | JSONB | — | Discrepancy details |
| `action_taken` | VARCHAR(100) | — | Description of action taken |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Item creation time |

**Indexes**: run_id, outcome, endorsement_id

#### 5.1.10 anomaly_detections

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Anomaly identifier |
| `endorsement_id` | UUID | — | Related endorsement (nullable for employer-level anomalies) |
| `employer_id` | UUID | NOT NULL | Employer identifier |
| `anomaly_type` | VARCHAR(50) | NOT NULL | VOLUME_SPIKE, ADD_DELETE_CYCLING, SUSPICIOUS_TIMING, UNUSUAL_PREMIUM, DORMANCY_BREAK |
| `score` | DECIMAL(5,4) | NOT NULL | Anomaly confidence score (0.0-1.0) |
| `explanation` | TEXT | NOT NULL | Human-readable explanation |
| `flagged_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | When flagged |
| `reviewed_at` | TIMESTAMPTZ | — | When reviewed |
| `status` | VARCHAR(30) | NOT NULL, DEFAULT 'FLAGGED' | FLAGGED, UNDER_REVIEW, DISMISSED, CONFIRMED_FRAUD |
| `reviewer_notes` | TEXT | — | Reviewer's decision notes |

**Indexes**: employer_id, status

#### 5.1.11 balance_forecast_records

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Forecast identifier |
| `employer_id` | UUID | NOT NULL | Employer identifier |
| `insurer_id` | UUID | NOT NULL | Insurer identifier |
| `forecast_date` | DATE | NOT NULL | Date being forecasted for (30 days ahead) |
| `forecasted_amount` | DECIMAL(12,2) | NOT NULL | Projected balance need |
| `actual_amount` | DECIMAL(12,2) | — | Actual consumption (filled later) |
| `accuracy` | DECIMAL(5,4) | — | 1 - abs(actual - forecasted) / forecasted |
| `narrative` | TEXT | — | Human-readable forecast description |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Creation timestamp |

**Indexes**: (employer_id, insurer_id), forecast_date

#### 5.1.12 error_resolutions

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Resolution identifier |
| `endorsement_id` | UUID | — | Related endorsement |
| `error_type` | VARCHAR(100) | NOT NULL | DATE_FORMAT, MISSING_FIELD, MEMBER_ID_FORMAT, PREMIUM_MISMATCH, UNKNOWN_ERROR |
| `original_value` | TEXT | — | Original failed value |
| `corrected_value` | TEXT | — | Suggested correction |
| `resolution` | TEXT | NOT NULL | Explanation of the fix |
| `confidence` | DECIMAL(5,4) | NOT NULL | Resolution confidence (0.0-1.0) |
| `auto_applied` | BOOLEAN | NOT NULL, DEFAULT false | Whether auto-applied |
| `outcome` | VARCHAR(20) | — | SUCCESS or FAILURE (populated after endorsement reaches terminal state) |
| `outcome_at` | TIMESTAMPTZ | — | When outcome was recorded |
| `outcome_endorsement_status` | VARCHAR(50) | — | Final endorsement status (CONFIRMED, REJECTED, FAILED_PERMANENT) |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Creation timestamp |

**Indexes**: endorsement_id, error_type

#### 5.1.13 process_mining_metrics

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Metric identifier |
| `insurer_id` | UUID | NOT NULL | Insurer identifier |
| `from_status` | VARCHAR(50) | NOT NULL | Source event type |
| `to_status` | VARCHAR(50) | NOT NULL | Target event type |
| `avg_duration_ms` | BIGINT | NOT NULL | Mean transition duration |
| `p95_duration_ms` | BIGINT | NOT NULL | 95th percentile duration |
| `p99_duration_ms` | BIGINT | NOT NULL | 99th percentile duration |
| `sample_count` | INT | NOT NULL | Number of observations |
| `happy_path_pct` | DECIMAL(5,2) | NOT NULL | Percentage following happy path |
| `calculated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | When calculated |

**Indexes**: insurer_id, (from_status, to_status)

#### 5.1.14 audit_logs (V17)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Audit log identifier |
| `action` | VARCHAR(100) | NOT NULL | Action performed (CREATE, UPDATE, CONFIRM, REJECT, etc.) |
| `entity_type` | VARCHAR(100) | NOT NULL | Entity type (ENDORSEMENT, EA_ACCOUNT, INSURER_CONFIG, etc.) |
| `entity_id` | VARCHAR(255) | — | Identifier of the affected entity |
| `details` | JSONB | — | Additional details (before/after state, parameters) |
| `timestamp` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | When the action occurred |

**Indexes**: entity_type, entity_id, timestamp

#### 5.1.15 shedlock (V16)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `name` | VARCHAR(64) | PK | Lock name (scheduler identifier) |
| `lock_until` | TIMESTAMPTZ | NOT NULL | When the lock expires |
| `locked_at` | TIMESTAMPTZ | NOT NULL | When the lock was acquired |
| `locked_by` | VARCHAR(255) | NOT NULL | Instance that holds the lock |

Used by ShedLock for distributed scheduler coordination across multiple application instances.

#### 5.1.16 endorsements_archive, ea_transactions_archive, endorsement_events_archive (V15)

Archive tables mirroring the schema of `endorsements`, `ea_transactions`, and `endorsement_events` respectively. Used by `DataRetentionScheduler` to move aged records from active tables to archive storage.

#### 5.1.17 stp_rate_snapshots (V19)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Snapshot identifier |
| `insurer_id` | UUID | NOT NULL | Insurer identifier |
| `snapshot_date` | DATE | NOT NULL | Date of the STP rate snapshot |
| `total_endorsements` | INT | NOT NULL, DEFAULT 0 | Total endorsements processed |
| `stp_endorsements` | INT | NOT NULL, DEFAULT 0 | Successfully straight-through processed |
| `stp_rate` | DECIMAL(7,4) | NOT NULL, DEFAULT 0 | STP rate percentage |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Creation timestamp |

**Indexes**: (insurer_id, snapshot_date) UNIQUE

Used by `ProcessMiningScheduler` to capture daily STP rate snapshots per insurer for trend analysis.

#### Schema Version Note

- `ea_accounts` table received an additional `version BIGINT DEFAULT 0` column (V14) for optimistic locking support on concurrent balance operations.
- `error_resolutions` table received `outcome`, `outcome_at`, `outcome_endorsement_status` columns (V20) for tracking resolution success/failure.

### 5.2 Domain Entities

#### 5.2.1 Endorsement (Core Aggregate Root)

| Method | Description |
|--------|-------------|
| `transitionTo(status)` | Validates and applies state transition, updates `updatedAt` |
| `canRetry()` | Returns `true` if `retryCount < 3` AND `status == REJECTED` |
| `isTerminal()` | Returns `true` if status is CONFIRMED or FAILED_PERMANENT |
| `incrementRetry()` | Increments `retryCount`, sets status to RETRY_PENDING |

#### 5.2.2 EAAccount

| Method | Description |
|--------|-------------|
| `availableBalance()` | Returns `balance - reserved` |
| `canFund(amount)` | Returns `availableBalance() >= amount` |
| `reserve(amount)` | Adds to `reserved`, throws if insufficient balance |
| `releaseReservation(amount)` | Subtracts from `reserved` |
| `debit(amount)` | Subtracts from `balance` |
| `credit(amount)` | Adds to `balance` |

#### 5.2.3 ProvisionalCoverage

| Method | Description |
|--------|-------------|
| `isActive()` | Returns `true` if not confirmed and not expired |
| `confirm(at)` | Sets `confirmedAt`, changes type to CONFIRMED |
| `expire(at)` | Sets `expiredAt` |

#### 5.2.4 EndorsementBatch

| Method | Description |
|--------|-------------|
| `isSlaBreached(now)` | Returns `true` if `now > slaDeadline` and status != COMPLETE |

#### 5.2.5 InsurerConfiguration

| Method | Description |
|--------|-------------|
| `toCapabilities()` | Converts configuration to `InsurerPort.InsurerCapabilities` record |

#### 5.2.6 ReconciliationRun

| Method | Description |
|--------|-------------|
| `incrementMatched()` | Increments `matched` and `totalChecked` counters |
| `incrementPartialMatched()` | Increments `partialMatched` and `totalChecked` counters |
| `incrementRejected()` | Increments `rejected` and `totalChecked` counters |
| `incrementMissing()` | Increments `missing` and `totalChecked` counters |
| `complete()` | Sets status to "COMPLETED" and `completedAt` to now |

#### 5.2.7 ReconciliationItem

Builder-based construction. No behavioral methods. Stores reconciliation outcome, sent/confirmed data, discrepancy details, and action taken.

#### 5.2.8 AnomalyDetection

| Method | Description |
|--------|-------------|
| `transitionTo(status)` | Validates and applies anomaly status transition |
| `isTerminal()` | Returns `true` if status is DISMISSED or CONFIRMED_FRAUD |

#### 5.2.9 BalanceForecastRecord

Builder-based construction. Stores forecast date, projected amount, actual amount (when available), accuracy, and narrative description.

#### 5.2.10 ErrorResolution

Builder-based construction. Stores error type, original/corrected values, resolution explanation, confidence score, and auto-applied flag.

#### 5.2.11 ProcessMiningMetric

Builder-based construction. Stores per-transition timing metrics (avg, p95, p99), sample count, and happy path percentage per insurer.

### 5.3 Enumerations

#### EndorsementType

| Value | Description |
|-------|-------------|
| ADD | Adding an employee to a policy |
| DELETE | Removing an employee from a policy |
| UPDATE | Modifying employee details on a policy |

#### EndorsementStatus

| Value | Terminal | Requires Insurer | Allowed Targets |
|-------|---------|-------------------|-----------------|
| CREATED | No | No | VALIDATED |
| VALIDATED | No | No | PROVISIONALLY_COVERED |
| PROVISIONALLY_COVERED | No | No | SUBMITTED_REALTIME, QUEUED_FOR_BATCH |
| SUBMITTED_REALTIME | No | Yes | INSURER_PROCESSING, REJECTED |
| QUEUED_FOR_BATCH | No | No | BATCH_SUBMITTED |
| BATCH_SUBMITTED | No | Yes | INSURER_PROCESSING, REJECTED |
| INSURER_PROCESSING | No | Yes | CONFIRMED, REJECTED |
| CONFIRMED | Yes | No | _(none)_ |
| REJECTED | No | No | RETRY_PENDING, FAILED_PERMANENT |
| RETRY_PENDING | No | No | SUBMITTED_REALTIME, QUEUED_FOR_BATCH, FAILED_PERMANENT |
| FAILED_PERMANENT | Yes | No | _(none)_ |

#### BatchStatus

| Value | Description |
|-------|-------------|
| ASSEMBLING | Batch is being constructed |
| SUBMITTED | Batch sent to insurer |
| PROCESSING | Insurer is processing the batch |
| PARTIAL_COMPLETE | Some endorsements in batch are processed |
| COMPLETE | All endorsements in batch are processed |
| FAILED | Batch processing failed |

#### EATransactionType

| Value | Description |
|-------|-------------|
| DEBIT | Funds deducted from balance |
| CREDIT | Funds added to balance |
| RESERVE | Funds reserved for a pending endorsement |
| RELEASE | Reserved funds released |
| TOP_UP | Account replenishment |

#### ReconciliationOutcome

| Value | Description |
|-------|-------------|
| MATCH | Endorsement fully matched with insurer records |
| PARTIAL_MATCH | Endorsement partially matched (reference present but batch missing) |
| REJECTED | Endorsement rejected by insurer |
| MISSING | Endorsement missing from insurer records |

#### AnomalyStatus

| Value | Terminal | Allowed Targets |
|-------|---------|-----------------|
| FLAGGED | No | UNDER_REVIEW, DISMISSED, CONFIRMED_FRAUD |
| UNDER_REVIEW | No | DISMISSED, CONFIRMED_FRAUD |
| DISMISSED | Yes | _(none)_ |
| CONFIRMED_FRAUD | Yes | _(none)_ |

#### AnomalyType

| Value | Description |
|-------|-------------|
| VOLUME_SPIKE | Abnormal burst of endorsement submissions |
| ADD_DELETE_CYCLING | Same employee added then deleted within window |
| SUSPICIOUS_TIMING | ADD endorsement with coverage starting very soon |
| UNUSUAL_PREMIUM | Premium significantly outside statistical norm |

#### EndorsementPriority

| Value | Rank | Classification Rule | Balance Impact |
|-------|------|---------------------|----------------|
| P0_DELETION | 0 | type == DELETE | Frees balance (credit) |
| P1_COST_NEUTRAL | 1 | type == UPDATE AND premiumAmount == 0 | No impact |
| P2_ADDITION | 2 | type == ADD | Consumes balance (reserve) |
| P3_PREMIUM_UPDATE | 3 | type == UPDATE AND premiumAmount > 0 | Consumes balance (reserve) |

---

## 6. Error Handling

All error responses conform to RFC 7807 Problem Detail format. Each error type increments the `endorsement.error` counter with the corresponding type tag.

| Exception | HTTP Status | Error Type Tag | Title | Detail |
|-----------|-------------|---------------|-------|--------|
| `EndorsementNotFoundException` | 404 Not Found | `not_found` | Endorsement Not Found | Endorsement not found: {id} |
| `DuplicateEndorsementException` | 409 Conflict | `duplicate` | Duplicate Endorsement | Duplicate endorsement with idempotency key: {key} |
| `InsufficientBalanceException` | 422 Unprocessable Entity | `insufficient_balance` | Insufficient Balance | Insufficient EA balance for employer {id}: required={x}, available={y} |
| `InsurerNotFoundException` | 404 Not Found | `insurer_not_found` | Insurer Not Found | Insurer not found with ID: {id} |
| `IllegalStateException` | 400 Bad Request | `illegal_state` | Invalid Operation | Cannot transition from {current} to {target} |
| `MethodArgumentNotValidException` | 400 Bad Request | `validation` | Validation Error | Array of `{field, message}` objects |
| `Exception` (catch-all) | 500 Internal Server Error | `unexpected` | Internal Server Error | An unexpected error occurred. Please try again later. |

**Error Response Format**:
```json
{
  "type": "about:blank",
  "title": "Endorsement Not Found",
  "status": 404,
  "detail": "Endorsement not found: 550e8400-e29b-41d4-a716-446655440000",
  "instance": "/api/v1/endorsements/550e8400-e29b-41d4-a716-446655440000"
}
```

---

## 7. Production Hardening Requirements

This section identifies all components that are currently implemented with simulated, mocked, or development-grade behaviour and require hardening before production deployment. Items are categorised into functional gaps (what the code does today vs. what it must do) and non-functional gaps (security, availability, performance, operations).

### Hardening Priority Legend

| Priority | Meaning | Timeline |
|----------|---------|----------|
| **P0 — Blocker** | System cannot go live without this | Before first production deployment |
| **P1 — Critical** | Acceptable for soft launch; required for GA | Within 2 sprints of launch |
| **P2 — Important** | Does not block launch but limits operational confidence | Within 1 quarter of launch |
| **P3 — Desirable** | Improves maturity; plan for later phase | 2–4 quarters post-launch |

---

### 7.1 Functional Hardening

#### 7.1.1 Insurer Adapter Integrations — P0

**Current state**: All four insurer adapters (`MockInsurerAdapter`, `IciciLombardAdapter`, `NivaBupaAdapter`, `BajajAllianzAdapter`) are **simulated**. They use `Thread.sleep()` to mimic latency, generate synthetic insurer reference numbers, and never connect to external APIs.

| Adapter | Protocol | What Must Change |
|---------|----------|-----------------|
| `MockInsurerAdapter` | JSON/REST | Retain as test double; do not deploy to production |
| `IciciLombardAdapter` | REST + JSON | Replace `Thread.sleep(150ms)` with real HTTP client calls; implement OAuth2 token management; honour insurer rate limits (120 req/min); parse real response payloads |
| `NivaBupaAdapter` | SFTP + CSV | Replace in-memory CSV generation with actual SFTP file upload/download; implement PGP encryption for file transfer; handle polling for async batch results |
| `BajajAllianzAdapter` | SOAP + XML | Replace synthetic XML with real WSDL-generated client; implement WS-Security headers; handle SOAP fault responses |

**Data mappers** (`IciciLombardDataMapper`, `NivaBupaCsvMapper`, `BajajAllianzXmlMapper`) must be validated against real insurer field specifications. Current field mappings are assumed.

#### 7.1.2 Notification System — P0

**Current state**: `LoggingNotificationAdapter` implements `NotificationPort` by writing `log.info()` messages. No actual notifications reach users.

| Notification Type | Production Requirement |
|-------------------|----------------------|
| Balance alerts | Email to employer finance team + in-app notification |
| Reconciliation discrepancies | Email to operations team + PagerDuty/OpsGenie alert for P0 mismatches |
| Endorsement status changes | Webhook callback to employer HR platform |
| SLA breach warnings | Escalation to account manager via Slack/Teams integration |
| Anomaly alerts | Email + dashboard alert for compliance team |

**Required**: Implement at least email (SES/SendGrid) and webhook adapters behind `NotificationPort`.

#### 7.1.3 Intelligence Layer (Phase 3) — P1

All Phase 3 intelligence components are **simulated** with hardcoded rules and `Thread.sleep()` to fake computation time. None use real ML/AI.

| Component | Current Implementation | Production Requirement |
|-----------|----------------------|----------------------|
| `RuleBasedAnomalyDetector` | 4 static threshold rules; `Thread.sleep(100ms)` | Statistical model (Isolation Forest / Z-score) trained on historical data; configurable thresholds per employer |
| `StatisticalForecastEngine` | Hardcoded 10% monthly growth + synthetic seasonality; `Thread.sleep(200ms)` | Time-series model (Prophet, ARIMA, or Holt-Winters) fitted to real EA transaction history; confidence intervals |
| `SimulatedErrorResolver` | Regex pattern matching on error messages; `Thread.sleep(150ms)` | LLM-powered resolution (RAG over insurer documentation + error knowledge base); human-in-the-loop approval workflow |
| `ConstraintBatchOptimizer` | Greedy knapsack heuristic; `Thread.sleep(100ms)` | Linear programming solver (OR-Tools / OptaPlanner) for multi-constraint batch optimization |
| `StatisticalProcessMiner` | Hardcoded average durations + random jitter; `Thread.sleep(100ms)` | Real process mining over event log data (PM4Py or custom implementation); actual bottleneck detection from event timestamps |

**Required**: Replace `Thread.sleep()` with real computation; use actual historical data; provide model versioning and retraining pipeline.

#### 7.1.4 EA Balance Forecasting Data — P1

**Current state**: `StatisticalForecastEngine` generates forecasts from synthetic seasonality patterns rather than real transaction history.

**Required**: Source historical endorsement volumes and EA transaction data for at least 12 months to train forecasting models. Implement data pipeline to refresh training data weekly.

#### 7.1.5 Reconciliation Engine — P1

**Current state**: Reconciliation compares endorsement status against insurer adapter responses, but since adapters return synthetic data, reconciliation outcomes are not validated against real insurer records.

**Required**: Once adapters connect to real insurer APIs, validate that reconciliation correctly handles: partial confirmations (subset of batch confirmed), delayed confirmations (outside SLA window), insurer-initiated rejections with reason codes, and data discrepancies (e.g., premium mismatch between sent and confirmed).

---

### 7.2 Security Hardening

#### 7.2.1 Authentication & Authorisation — P0

**Current state**: `SecurityConfig.java` uses `.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())`. All endpoints are publicly accessible without authentication.

| Requirement | Detail |
|-------------|--------|
| Authentication | Integrate OAuth2/OIDC provider (Keycloak, Auth0, or corporate SSO). All API endpoints must require valid JWT bearer tokens. |
| Role-based access | Define roles: `ENDORSEMENT_ADMIN`, `ENDORSEMENT_OPERATOR`, `ENDORSEMENT_VIEWER`, `RECONCILIATION_ADMIN`. Map to endpoint permissions. |
| Service-to-service auth | Kafka consumers and scheduled jobs must authenticate using service accounts (mTLS or OAuth2 client credentials). |
| API key management | External webhook consumers must authenticate via API keys with rotation support. |

#### 7.2.2 Secrets Management — P0

**Current state**: Credentials are hardcoded in plain text.

| File | Secret | Current Value | Production Requirement |
|------|--------|---------------|----------------------|
| `application.yml` | PostgreSQL password | `endorsements` | Vault/AWS Secrets Manager with rotation |
| `application.yml` | Redis password | _(none set)_ | Set password; store in Vault |
| `docker-compose.yml` | `POSTGRES_PASSWORD` | `endorsements` | Environment-specific secrets injection |
| `k8s/postgres/secret.yaml` | DB password | Base64-encoded `endorsements` | Kubernetes External Secrets Operator or Sealed Secrets |
| Insurer adapter configs | API keys, OAuth tokens | Not yet implemented | Vault dynamic secrets with lease management |

#### 7.2.3 Network Security — P1

| Requirement | Detail |
|-------------|--------|
| TLS termination | All HTTP traffic must use TLS 1.2+. Configure at ingress/load balancer level. |
| Kafka encryption | Enable SASL_SSL for Kafka broker connections; encrypt data in transit. |
| Database encryption | Enable PostgreSQL SSL mode (`sslmode=verify-full`); encrypt data at rest (TDE or disk-level). |
| Redis encryption | Enable TLS for Redis connections; use ACLs for access control. |
| Network segmentation | Database and message broker should not be publicly accessible; restrict to application VPC/namespace. |

#### 7.2.4 Input Validation & Injection Prevention — P1

| Requirement | Detail |
|-------------|--------|
| Request size limits | Enforce max request body size (e.g., 1 MB) at gateway/filter level |
| Rate limiting | Implement per-client rate limiting (e.g., 100 req/min per API key) using Redis-backed token bucket |
| SQL injection | Current: Spring Data parameterised queries (safe). Verify no raw SQL in custom queries. |
| XSS | Current: REST API returns JSON only (low risk). Sanitise any user-supplied text stored in `endorsement_data` JSONB. |
| CORS | Configure explicit allowed origins instead of default permissive policy |

#### 7.2.5 Audit Logging — P1 (Partially Implemented)

**Current state**: `AuditLoggingAspect` captures all mutation operations and persists to dedicated `audit_logs` table (V17 migration). `AuditLogController` exposes `GET /api/v1/audit-logs` for querying. Records action, entity type, entity ID, details (JSONB), and timestamp.

**Remaining**: Add authenticated principal identity (`userId`, `userEmail`, `userRole`) to audit records once AuthZ/AuthN is implemented. Add source IP capture. Implement append-only constraint (prevent UPDATE/DELETE on audit_logs). Retain for compliance period (typically 7 years for insurance).

---

### 7.3 High Availability & Resilience

#### 7.3.1 Database — P0

**Current state**: Single PostgreSQL instance; no replication, failover, or backup strategy.

| Requirement | Detail |
|-------------|--------|
| Replication | Primary-replica setup with synchronous replication for zero data loss, or managed service (RDS Multi-AZ, Cloud SQL HA) |
| Connection pooling | Add PgBouncer or HikariCP tuning (current: Spring Boot defaults). Target: min 10, max 50 connections per instance. |
| Backup | Automated daily full backups + continuous WAL archiving for point-in-time recovery. Test restore procedure quarterly. |
| Failover | Automatic failover with < 30s detection time. Application must handle transient connection errors gracefully. |

#### 7.3.2 Kafka — P0

**Current state**: Single Kafka broker with `replication.factor=1`, `min.insync.replicas=1`. Any broker failure loses messages.

| Requirement | Detail |
|-------------|--------|
| Cluster size | Minimum 3 brokers for production |
| Replication | `replication.factor=3`, `min.insync.replicas=2` |
| Dead letter queue | Implement DLQ topic for messages that fail processing after max retries. Currently, failed messages are logged and lost. |
| Consumer groups | Configure `max.poll.records`, `session.timeout.ms`, and `heartbeat.interval.ms` for stable consumer group membership |
| Schema registry | Add Confluent Schema Registry for Avro/JSON Schema enforcement on event payloads |

#### 7.3.3 Redis — P1

**Current state**: Standalone Redis instance used for `@Cacheable` (InsurerRegistry).

| Requirement | Detail |
|-------------|--------|
| Clustering | Redis Sentinel or Redis Cluster for HA |
| Persistence | Enable AOF persistence for cache warm-up after restart |
| Eviction policy | Configure `maxmemory-policy` (recommend `allkeys-lru`) |
| Cache invalidation | Ensure `@CacheEvict` in InsurerRegistry is sufficient; consider TTL as backup |

#### 7.3.4 Application Instances — P1

**Current state**: Single application instance. All K8s deployments specify `replicas: 1`.

| Requirement | Detail |
|-------------|--------|
| Horizontal scaling | Minimum 2 replicas for zero-downtime deployments; autoscale based on CPU/request rate |
| Scheduler coordination | ShedLock infrastructure deployed (`shedlock` table via V16 migration, `ShedLockConfig.java`). 9 schedulers use `@Scheduled` — all are candidates for `@SchedulerLock` annotations when running multiple instances. |
| Health checks | Current: Spring Actuator `/health`. Add readiness probe that verifies DB + Kafka + Redis connectivity. Liveness probe should check for deadlocked threads. |
| Graceful shutdown | Configure `spring.lifecycle.timeout-per-shutdown-phase=30s`. Ensure in-flight Kafka messages are committed before shutdown. |

#### 7.3.5 Circuit Breaker Tuning — P2

**Current state**: Resilience4j circuit breakers configured for `iciciLombard` and `bajajAllianz` with default-like settings.

**Required**: Tune per-insurer based on real latency profiles: `slidingWindowSize`, `failureRateThreshold`, `waitDurationInOpenState`, `slowCallDurationThreshold`. Add fallback behaviour (queue for retry vs. immediate failure) based on insurer SLA agreements.

---

### 7.4 Performance & Scalability

#### 7.4.1 Database Query Optimisation — P1

| Requirement | Detail |
|-------------|--------|
| Index review | Verify indexes exist for: `endorsements(status, insurer_id)`, `endorsements(employer_id, created_at)`, `endorsements(idempotency_key)`, `reconciliation_items(run_id, outcome)`. Add composite indexes for common query patterns. |
| Query analysis | Run `EXPLAIN ANALYZE` on all `SpringDataEndorsementRepository` custom queries under realistic data volumes (1M+ endorsements). |
| Pagination | All list endpoints use `Pageable`. Verify no unbounded queries in scheduled jobs (e.g., `findByStatus` could return millions). Use cursor-based pagination for large result sets in schedulers. |

#### 7.4.2 Kafka Consumer Scaling — P1

**Current state**: Single consumer instance per topic. With 32 partitions on `endorsement-events`, only 1 of 32 partitions is actively consumed.

**Required**: Scale consumer instances to match partition count (or use a consumer group with multiple threads). Configure `concurrency` in `@KafkaListener` to enable parallel partition processing within a single instance.

#### 7.4.3 Connection Pool Tuning — P2

| Component | Current | Production Target |
|-----------|---------|-------------------|
| HikariCP (PostgreSQL) | Spring defaults (10 max) | 20–50 max per instance based on load testing |
| Kafka producer | Defaults | Tune `batch.size`, `linger.ms`, `buffer.memory` for throughput |
| Redis | Lettuce defaults | Configure `maxTotal`, `maxIdle`, `minIdle` in connection pool |

#### 7.4.4 Caching Strategy — P2

**Current state**: Only `InsurerRegistry` uses `@Cacheable`. No distributed cache for frequently accessed data.

**Required**: Evaluate caching for: employer EA account balances (read-heavy), endorsement counts by status (dashboard), insurer capabilities (rarely changes). Use Redis with appropriate TTLs. Implement cache-aside pattern with write-through for balance updates.

---

### 7.5 Observability & Operations

#### 7.5.1 Alerting Rules — P0

**Current state**: Prometheus collects metrics and Grafana dashboards visualise them, but **no alerting rules** are configured. Operational issues go unnoticed until manual inspection.

| Alert | Condition | Severity | Channel |
|-------|-----------|----------|---------|
| High error rate | `endorsement.error` rate > 5% over 5 min | Critical | PagerDuty |
| Submission latency | `endorsement.submission.latency` p95 > 2s | Warning | Slack |
| Circuit breaker open | Any insurer circuit breaker in OPEN state | Critical | PagerDuty |
| Kafka consumer lag | Consumer lag > 10,000 messages | Warning | Slack |
| Database connection pool exhaustion | Active connections > 80% of max | Warning | Slack |
| Reconciliation failures | Reconciliation MISSING count > 10 in single run | Critical | PagerDuty + Email |
| Scheduler not running | No scheduler execution metric for > 2x scheduled interval | Critical | PagerDuty |
| Disk usage | PostgreSQL / Kafka disk > 80% | Warning | Slack |

#### 7.5.2 Distributed Tracing — P1

**Current state**: OpenTelemetry dependency is declared but no exporter is configured in production profile. Trace context is not propagated through Kafka messages.

**Required**: Configure OTLP exporter to Jaeger/Tempo. Inject `traceparent` header into Kafka message headers for end-to-end trace correlation. Ensure trace sampling rate is appropriate (100% in staging, 10% in production).

#### 7.5.3 Structured Logging — P2

**Current state**: `logback-spring.xml` uses JSON format (good). `MdcRequestFilter` injects `correlationId`, `requestMethod`, `requestUri`.

**Required**: Add `userId` (from JWT), `employerId`, `insurerId` to MDC context for all log entries. Ensure log aggregation pipeline (ELK/Loki) indexes these fields for operational queries. Configure log retention policy (30 days hot, 90 days warm, 1 year cold).

#### 7.5.4 SLA Tracking — P2

**Current state**: No explicit SLA tracking. Reconciliation identifies missing endorsements but does not measure time-to-confirmation against insurer SLAs.

**Required**: Track per-insurer SLA metrics: submission-to-confirmation duration, breach count, breach percentage. Expose in Grafana dashboard and alert on breach rate > threshold.

---

### 7.6 Data Management

#### 7.6.1 Data Archival — P1

**Current state**: No archival strategy. All endorsements, events, and reconciliation records accumulate indefinitely.

| Table | Retention Policy | Archive Strategy |
|-------|-----------------|-----------------|
| `endorsements` | Active: 2 years; Archive: 7 years (regulatory) | Partition by `created_at`; move to cold storage (S3/GCS) after 2 years |
| `endorsement_events` | Active: 1 year | Archive to object storage; maintain for audit trail |
| `reconciliation_runs` / `reconciliation_items` | Active: 1 year | Archive after resolution |
| `provisional_coverages` | Active: 6 months (expired coverages cleaned by scheduler) | Archive expired records |
| Kafka topics | Default: 7 days retention | Increase to 30 days for `endorsement-events`; configure topic-level retention |

#### 7.6.2 Data Migration & Versioning — P2

**Current state**: Flyway manages schema migrations (V1–V17). No data migration tooling for production data transformations.

**Required**: Establish Flyway naming convention for data migrations (e.g., `V{n}__data_migrate_*.sql`). Test all migrations against production-sized datasets. Implement rollback scripts for critical migrations.

#### 7.6.3 PII & Data Privacy — P1

**Current state**: Employee data (`employeeName`, `employeeEmail`, `dateOfBirth`) stored in plain text in `endorsement_data` JSONB column.

**Required**: Encrypt PII fields at rest (application-level encryption or PostgreSQL pgcrypto). Implement data masking for non-production environments. Define data deletion workflow for GDPR/DPDP Act right-to-erasure requests. Ensure PII is not logged in application logs.

---

### 7.7 Deployment & CI/CD

#### 7.7.1 CI/CD Pipeline — P0

**Current state**: No CI/CD pipeline. Builds and tests are run manually via `./gradlew` and `./run-all-tests.sh`.

| Stage | Requirement |
|-------|-------------|
| Build | Automated build on every PR (GitHub Actions / GitLab CI) |
| Unit tests | Run on every commit; fail PR if tests fail |
| Integration tests | Run API + BDD tests against containerised dependencies (Testcontainers) |
| Security scan | SAST (SonarQube/Semgrep), dependency vulnerability scan (OWASP Dependency-Check / Snyk) |
| Container build | Build Docker image; push to private registry (ECR/GCR/ACR) |
| Staging deploy | Auto-deploy to staging on merge to main |
| Production deploy | Manual approval gate; canary or blue-green deployment strategy |
| Smoke tests | Run subset of API tests against deployed environment post-deploy |

#### 7.7.2 Environment Configuration — P1

**Current state**: `application.yml` contains hardcoded `localhost` references. Only `application-railway.yml` and `application-test.yml` profiles exist.

**Required**: Create environment-specific profiles: `application-staging.yml`, `application-production.yml`. Externalise all environment-specific values (DB URL, Kafka brokers, Redis host, insurer API URLs) to environment variables or config maps. Never reference `localhost` in non-dev profiles.

#### 7.7.3 Container Hardening — P2

**Current state**: `Dockerfile` exists but uses default JVM settings.

| Requirement | Detail |
|-------------|--------|
| Base image | Use distroless or slim JDK image (e.g., `eclipse-temurin:21-jre-alpine`) |
| Non-root user | Run application as non-root user inside container |
| JVM tuning | Set explicit `-Xms`, `-Xmx`, `-XX:MaxMetaspaceSize`; enable `-XX:+UseContainerSupport` |
| Image scanning | Scan for CVEs using Trivy or Snyk Container before deployment |
| Multi-stage build | Separate build and runtime stages to minimise image size |

---

### 7.8 Testing Maturity

#### 7.8.1 Code Coverage Enforcement — P1

**Current state**: No code coverage tool configured. No coverage thresholds enforced.

**Required**: Add JaCoCo Gradle plugin. Enforce minimum thresholds: 80% line coverage, 70% branch coverage. Fail build if coverage drops below threshold. Exclude generated code and configuration classes.

#### 7.8.2 Contract Testing — P2

**Current state**: No contract tests. API consumers (frontend, external HR platforms) have no guaranteed schema stability.

**Required**: Implement Spring Cloud Contract or Pact for consumer-driven contract testing. Publish contracts to a broker. Verify contracts on every backend change.

#### 7.8.3 Chaos Engineering — P3

**Current state**: No failure injection testing. Resilience4j circuit breakers are configured but never tested under real failure conditions.

**Required**: Implement chaos testing (Chaos Monkey for Spring Boot or LitmusChaos for K8s): kill random pod, inject network latency, simulate database unavailability, simulate Kafka broker failure. Verify system degrades gracefully and recovers automatically.

#### 7.8.4 Load Testing Validation — P2

**Current state**: Gatling performance tests exist with 100K endorsements/day target, but results are measured against simulated adapters with `Thread.sleep()`. Real insurer API latency will differ.

**Required**: Re-run performance tests once real insurer adapters are integrated. Establish baseline latency profiles per insurer. Validate that the 100K/day throughput target holds with real network I/O and insurer rate limits.

---

### 7.9 Production Hardening Summary Matrix

| # | Component | Current State | Priority | Hardening Required |
|---|-----------|--------------|----------|-------------------|
| 1 | Insurer adapters | Simulated (`Thread.sleep`) | **P0** | Real HTTP/SFTP/SOAP clients |
| 2 | Notification system | `log.info()` only | **P0** | Email, webhook, alerting integrations |
| 3 | Authentication | `permitAll()` | **P0** | OAuth2/OIDC + RBAC |
| 4 | Secrets management | Hardcoded plaintext | **P0** | Vault / Secrets Manager |
| 5 | Database HA | Single instance | **P0** | Replication + failover + backups |
| 6 | Kafka HA | Single broker, RF=1 | **P0** | 3-broker cluster, RF=3 |
| 7 | CI/CD pipeline | Manual builds | **P0** | Automated build/test/deploy pipeline |
| 8 | Alerting rules | None configured | **P0** | Prometheus alerting + PagerDuty/Slack |
| 9 | Intelligence layer | Simulated ML/AI | **P1** | Real models + training pipeline |
| 10 | Network security | No TLS, no encryption | **P1** | TLS everywhere, encrypted data at rest |
| 11 | Audit logging | Dedicated `audit_logs` table + `AuditLoggingAspect` + API | **P1** | Add user identity, source IP, append-only constraint |
| 12 | PII protection | Plaintext storage | **P1** | Encryption + masking + erasure workflow |
| 13 | Scheduler locking | ShedLock table + config deployed | **P1** | Add `@SchedulerLock` annotations to all 9 schedulers |
| 14 | Distributed tracing | Not configured | **P1** | OTLP exporter + Kafka header propagation |
| 15 | Data archival | No retention policy | **P1** | Partition + archive to cold storage |
| 16 | Environment profiles | Hardcoded localhost | **P1** | Staging + production profiles |
| 17 | Code coverage | Not measured | **P1** | JaCoCo with 80% threshold |
| 18 | Reconciliation validation | Against simulated data | **P1** | Validate with real insurer responses |
| 19 | Consumer scaling | Single consumer | **P1** | Match consumers to partition count |
| 20 | Connection pool tuning | Spring defaults | **P2** | Profile-specific pool sizing |
| 21 | Caching strategy | InsurerRegistry only | **P2** | Redis cache for hot data |
| 22 | Container hardening | Default Dockerfile | **P2** | Distroless, non-root, JVM tuning |
| 23 | Contract testing | None | **P2** | Pact / Spring Cloud Contract |
| 24 | Load test validation | Against simulated adapters | **P2** | Re-test with real adapters |
| 25 | SLA tracking | Not implemented | **P2** | Per-insurer SLA metrics |
| 26 | Circuit breaker tuning | Default-like settings | **P2** | Tune from real latency profiles |
| 27 | Chaos engineering | None | **P3** | Failure injection testing |
