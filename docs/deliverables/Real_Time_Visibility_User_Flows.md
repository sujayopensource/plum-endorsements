# User Flows and Dashboards for Real-Time Visibility

**Project:** Plum Endorsement Management System
**Deliverable:** #4 -- Real-Time Visibility User Flows and Example Screens
**Date:** March 13, 2026

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Stakeholder Personas and Visibility Needs](#2-stakeholder-personas-and-visibility-needs)
3. [Visibility Architecture Overview](#3-visibility-architecture-overview)
4. [Frontend Application -- React Dashboard](#4-frontend-application--react-dashboard)
5. [Grafana Operational Dashboards](#5-grafana-operational-dashboards)
6. [API-Driven Visibility -- Swagger UI](#6-api-driven-visibility--swagger-ui)
7. [Event-Driven Real-Time Updates](#7-event-driven-real-time-updates)
8. [Observability Stack Integration](#8-observability-stack-integration)
9. [User Flow Diagrams](#9-user-flow-diagrams)
10. [Design Decisions and Trade-Offs](#10-design-decisions-and-trade-offs)

---

## 1. Problem Statement

In group health insurance endorsement processing, multiple stakeholders -- HR admins, finance teams, operations staff, and insurers -- need real-time visibility into endorsement status, coverage status, financial position, and system health.

Without visibility, the consequences are severe:

```
+---------------------------+---------------------------------------------+
| What Goes Wrong           | Business Impact                             |
+---------------------------+---------------------------------------------+
| Employee coverage status  | Claims denied for covered employees.        |
| is unknown                | HR cannot answer "am I covered?" questions. |
+---------------------------+---------------------------------------------+
| EA balance not tracked    | Endorsements blocked due to insufficient    |
| in real time              | funds. Batch processing stalls silently.    |
+---------------------------+---------------------------------------------+
| Insurer submission        | Endorsements stuck in SUBMITTED state for   |
| failures are invisible    | days. No one notices until claims fail.     |
+---------------------------+---------------------------------------------+
| Reconciliation gaps       | Discrepancies between sent and confirmed    |
| not surfaced              | endorsements discovered only at month-end.  |
+---------------------------+---------------------------------------------+
| No anomaly detection      | Fraudulent add/delete cycling goes          |
|                           | undetected for weeks.                       |
+---------------------------+---------------------------------------------+
```

**The core insight:** Endorsement processing is inherently asynchronous -- batch submissions take hours to days, provisional coverage must be tracked separately from confirmed coverage, and financial positions change with every endorsement. Without purpose-built visibility tooling, stakeholders operate blind.

Our system provides **four layers of visibility**, each serving different stakeholders at different time scales:

```
Layer 1: Frontend Dashboard (React)     -- Seconds    -- HR, Finance, Employers
Layer 2: Grafana Dashboards             -- Seconds    -- Operations, Platform
Layer 3: REST API + Swagger UI          -- On-demand  -- Developers, Integrators
Layer 4: Kafka Events + Future WebSocket-- Sub-second -- Real-time consumers
```

---

## 2. Stakeholder Personas and Visibility Needs

### 2.1 HR Admin

**Role:** Manages employee lifecycle events -- joinings, exits, dependent changes.

**Key questions they need answered:**
- "Is this new employee covered? The endorsement was submitted 2 hours ago."
- "How many endorsements are still pending for this month's batch?"
- "Which employees have provisional vs confirmed coverage?"

**Visibility needs:**
- Per-employee endorsement status with color-coded state
- Provisional coverage indicator (yellow badge) vs confirmed (green badge)
- Status timeline showing each state transition with timestamp
- Ability to create endorsements and track them through to completion

**Primary tools:** Frontend Dashboard (Pages 1-4), Endorsement Detail page

### 2.2 Finance Team

**Role:** Manages Employer Advance (EA) accounts and financial forecasting.

**Key questions they need answered:**
- "What is our current EA balance? How much is reserved for pending endorsements?"
- "Will we run out of EA balance before the next batch runs?"
- "Are there any balance discrepancies from the last reconciliation?"

**Visibility needs:**
- Real-time EA balance breakdown: total / reserved / available
- Balance forecast with shortfall alerts (30-day lookahead)
- Batch optimization preview: what gets included vs deferred
- Reconciliation run history with match vs discrepancy counts

**Primary tools:** Frontend (Pages 5-6), Grafana (Reconciliation dashboard)

### 2.3 Operations / Platform Team

**Role:** Monitors system health, identifies bottlenecks, responds to incidents.

**Key questions they need answered:**
- "What is our current STP (Straight-Through Processing) rate?"
- "Are any insurers experiencing elevated failure rates?"
- "Are the scheduled jobs (batch assembly, reconciliation) running on time?"
- "Are there anomalies that need investigation?"

**Visibility needs:**
- Request rate, error rate, latency percentiles (p50/p95/p99)
- Per-insurer submission success/failure rates
- Scheduler execution counts and durations
- Anomaly detection alerts with severity scores
- Process mining insights for bottleneck identification

**Primary tools:** Grafana (all 7 dashboards), Frontend (Intelligence page)

### 2.4 Employer Dashboard User

**Role:** Self-service user at the employer company tracking their endorsements.

**Key questions they need answered:**
- "What is the status of the endorsements I submitted this week?"
- "Is my new employee covered?"
- "What is my EA account balance?"

**Visibility needs:**
- Filtered view of their employer's endorsements only
- Simple status indicators (pending / confirmed / failed)
- EA account summary card

**Primary tools:** Frontend Dashboard (employer-filtered view)

---

## 3. Visibility Architecture Overview

```
+-------------------------------------------------------------------+
|                        VISIBILITY LAYERS                          |
+-------------------------------------------------------------------+
|                                                                   |
|  +---------------------+    +---------------------+              |
|  |  React Frontend     |    |  Grafana Dashboards |              |
|  |  (localhost:5173)   |    |  (localhost:3000)   |              |
|  |                     |    |                     |              |
|  |  8 pages            |    |  7 dashboards       |              |
|  |  HR, Finance,       |    |  Operations,        |              |
|  |  Employers          |    |  Platform Team      |              |
|  +----------+----------+    +----------+----------+              |
|             |                          |                         |
|             v                          v                         |
|  +---------------------+    +---------------------+              |
|  |  REST API (27 eps)  |    |  Prometheus Metrics  |              |
|  |  (localhost:8080)   |    |  (localhost:9090)    |              |
|  |  Spring Boot +      |    |  Micrometer +       |              |
|  |  Swagger UI         |    |  Custom Counters    |              |
|  +----------+----------+    +----------+----------+              |
|             |                          |                         |
|             +----------+  +-----------+                          |
|                        |  |                                      |
|                        v  v                                      |
|             +---------------------+                              |
|             |  Application Core   |                              |
|             |  (Hexagonal Arch)   |                              |
|             +----------+----------+                              |
|                        |                                         |
|          +-------------+-------------+                           |
|          |             |             |                            |
|          v             v             v                            |
|  +------------+  +-----------+  +------------+                   |
|  | PostgreSQL |  |   Kafka   |  |   Redis    |                   |
|  | (state)    |  | (events)  |  | (cache)    |                   |
|  +------------+  +-----+-----+  +------------+                   |
|                        |                                         |
|                        v                                         |
|              +-------------------+                               |
|              | 24 Event Types    |                                |
|              | (future: WS push) |                                |
|              +-------------------+                                |
|                                                                   |
+-------------------------------------------------------------------+
```

---

## 4. Frontend Application -- React Dashboard

**Technology:** React 18 + TypeScript + Vite + TanStack Query + Tailwind CSS + shadcn/ui
**URL:** `http://localhost:5173`
**State Management:** TanStack Query with 30-second stale time for near-real-time polling

### 4.1 Page 1: Dashboard (`/`)

The landing page provides a high-level overview of endorsement operations for a given employer.

```
+------------------------------------------------------------------+
|  [Plum Logo]  Dashboard | Endorsements | EA Accounts | ...       |
+------------------------------------------------------------------+
|                                                                  |
|  Dashboard                                                       |
|  Overview of endorsement operations                              |
|                                                                  |
|  Employer ID: [11111111-1111-1111-1111-111111111111          ]   |
|  Insurer ID:  [22222222-2222-2222-2222-222222222222          ]   |
|                                                                  |
|  +------------+ +------------+ +------------+ +------------+     |
|  | [doc icon] | | [clock]    | | [check]    | | [x-circle] |     |
|  | Total      | | Pending    | | Confirmed  | | Failed     |     |
|  |    47       | |    12       | |    30       | |     5       |     |
|  +------------+ +------------+ +------------+ +------------+     |
|                                                                  |
|  Status Distribution                                             |
|  [====green=====|==yellow==|=blue=|=red=]                        |
|  Confirmed: 30   Processing: 8   Queued: 4   Failed: 5          |
|                                                                  |
|  +---------------------------------------------+ +------------+ |
|  | Recent Endorsements             [View all >] | | EA Account | |
|  |                                              | |            | |
|  | Employee   Type    Status      Created       | | Balance    | |
|  | a1b2c3d4.. [ADD]   [Confirmed] 2 hours ago  | | Rs 50,000  | |
|  | e5f6g7h8.. [DEL]   [Processing] 30 min ago  | |            | |
|  | i9j0k1l2.. [ADD]   [Prov.Cov]  15 min ago   | | Reserved   | |
|  | m3n4o5p6.. [UPD]   [Created]   5 min ago    | | Rs 12,000  | |
|  | q7r8s9t0.. [ADD]   [Submitted] just now     | |            | |
|  +---------------------------------------------+ | Available  | |
|                                                   | Rs 38,000  | |
|                                                   |            | |
|                                                   | [===green] | |
|                                                   | Last: 14:30| |
|                                                   +------------+ |
+------------------------------------------------------------------+
```

**Key design decisions:**
- Employer ID is input-driven (not auth-gated in MVP) for demo flexibility
- Status distribution uses a stacked progress bar with color segments
- EA Account card shows balance/reserved/available with a visual fill bar
- TanStack Query re-fetches every 30 seconds for near-real-time updates

### 4.2 Page 2: Endorsement List (`/endorsements`)

The primary endorsement management view with filtering and pagination.

```
+------------------------------------------------------------------+
|  Endorsements                            [+ Create Endorsement]  |
|  Manage employee endorsement requests                            |
|                                                                  |
|  Employer ID: [11111111-1111-1111-1111-111111111111          ]   |
|  Status: [x Created] [x Validated] [x Processing] [ Confirmed]  |
|                                                                  |
|  +--------------------------------------------------------------+|
|  | Employee   Type   Status        Premium   Cov Start  Ins Ref ||
|  |----------------------------------------------------------------|
|  | a1b2c3d4.. [ADD]  [Confirmed]   Rs 5,000  01 Mar 26  IC-1234||
|  | e5f6g7h8.. [DEL]  [Processing]  Rs 0      15 Mar 26  --     ||
|  | i9j0k1l2.. [ADD]  [Prov.Cov]   Rs 8,000  20 Mar 26  --     ||
|  | m3n4o5p6.. [UPD]  [Created]    Rs 2,500  22 Mar 26  --     ||
|  | q7r8s9t0.. [ADD]  [Queued]     Rs 6,000  25 Mar 26  --     ||
|  | ...                                                          ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  Showing 10 of 47 endorsements        [< Previous] [Next >]     |
+------------------------------------------------------------------+
```

**Status badge color coding** (implemented via Tailwind CSS classes):

```
Status                  Background    Text Color    Border
-------------------------------------------------------------
CREATED                 gray-50       gray-700      gray-300
VALIDATED               blue-50       blue-700      blue-300
PROVISIONALLY_COVERED   sky-100       sky-800       sky-300
SUBMITTED_REALTIME      blue-100      blue-800      blue-300
QUEUED_FOR_BATCH        indigo-100    indigo-800    indigo-300
BATCH_SUBMITTED         indigo-100    indigo-800    indigo-300
INSURER_PROCESSING      yellow-100    yellow-800    yellow-300
CONFIRMED               green-100     green-800     green-300
REJECTED                red-100       red-800       red-300
RETRY_PENDING           amber-100     amber-800     amber-300
FAILED_PERMANENT        red-200       red-900       red-400
```

**Type badge color coding:**

```
Type      Background    Text Color
------------------------------------
ADD       blue-100      blue-800
DELETE    red-100       red-800
UPDATE    amber-100     amber-800
```

### 4.3 Page 3: Endorsement Detail (`/endorsements/:id`)

The single-endorsement deep-dive view with status timeline, coverage card, and action buttons.

```
+------------------------------------------------------------------+
|  [<- Back]  Endorsement Detail                                   |
|             a1b2c3d4-e5f6-7890-abcd-ef1234567890                 |
|                                                                  |
|  STATUS TIMELINE (Horizontal)                                    |
|  (o)----(o)----(o)----(o)----(o)----(*)----( )                   |
|  Created Vali-  Prov.  Subm.  Insurer Proc- Confirmed            |
|          dated  Covrd  (RT)   essing                              |
|                                                                  |
|  For rejected/retry:                                             |
|  +-----------------------------------------------------------+  |
|  | [!] Retry Pending (attempt 2/3)                            |  |
|  |     Reason: Invalid policy number format                   |  |
|  +-----------------------------------------------------------+  |
|                                                                  |
|  +--------------------------------------+ +--------------------+ |
|  | Details                              | | Actions            | |
|  |                                      | |                    | |
|  | Endorsement ID  a1b2c3d4... [copy]   | | [Submit to Insurer]| |
|  | Type            [ADD]                | | [Confirm]          | |
|  | Status          [Processing]         | | [Reject]           | |
|  | Employer ID     11111111... [copy]   | |                    | |
|  | Employee ID     aabbccdd... [copy]   | +--------------------+ |
|  | Insurer ID      22222222...          | | Coverage           | |
|  | Policy ID       policy-001...        | |                    | |
|  | Coverage        01 Mar - 31 Mar 2026 | | Type  [PROVISIONAL]| |
|  | Premium         Rs 5,000             | |       (yellow)     | |
|  | Insurer Ref     IC-1234              | | Start 01 Mar 2026  | |
|  | Batch ID        --                   | | Active Yes         | |
|  | Retry Count     0 / 3                | |                    | |
|  | Idempotency     emp-1-ADD-2026-03-01 | | After confirmation:| |
|  | Created         07 Mar 2026 14:30:22 | | Type  [CONFIRMED]  | |
|  | Updated         07 Mar 2026 14:31:45 | |       (green)      | |
|  +--------------------------------------+ +--------------------+ |
+------------------------------------------------------------------+
```

**Status Timeline implementation:**

The timeline component renders differently based on the submission path:

```
Real-time path:
  CREATED -> VALIDATED -> PROV_COVERED -> SUBMITTED_RT -> INSURER_PROC -> CONFIRMED

Batch path:
  CREATED -> VALIDATED -> PROV_COVERED -> QUEUED_BATCH -> BATCH_SUBM -> INSURER_PROC -> CONFIRMED
```

Each step is a circle:
- Green filled + checkmark = completed step
- Blue filled + dot = current step
- Gray outline = upcoming step

The component uses `batchId` presence to determine which path to render. If the endorsement has a `batchId` or status is `QUEUED_FOR_BATCH` / `BATCH_SUBMITTED`, the batch path is shown.

**Action buttons are context-dependent:**

```
Current Status          Available Actions
---------------------------------------------------
CREATED                 (none -- automatic validation)
VALIDATED               (none -- automatic provisional coverage)
PROVISIONALLY_COVERED   [Submit to Insurer]
SUBMITTED_REALTIME      (waiting for insurer)
QUEUED_FOR_BATCH        (waiting for batch assembly)
BATCH_SUBMITTED         (waiting for insurer)
INSURER_PROCESSING      [Confirm] [Reject]
CONFIRMED               (terminal -- no actions)
REJECTED                (auto-retry or manual)
RETRY_PENDING           [Submit to Insurer]
FAILED_PERMANENT        (terminal -- no actions)
```

### 4.4 Page 4: Create Endorsement (`/endorsements/new`)

Form for creating a new endorsement.

```
+------------------------------------------------------------------+
|  [<- Back]  Create Endorsement                                   |
|                                                                  |
|  +--------------------------------------------------------------+|
|  |                                                              ||
|  |  Employee ID     [                                       ]   ||
|  |  Employer ID     [11111111-1111-1111-1111-111111111111   ]   ||
|  |  Insurer ID      [22222222-2222-2222-2222-222222222222   ]   ||
|  |  Policy ID       [                                       ]   ||
|  |                                                              ||
|  |  Type            ( ) ADD    ( ) DELETE    ( ) UPDATE          ||
|  |                                                              ||
|  |  Coverage Start  [2026-03-13]                                ||
|  |  Coverage End    [2027-03-12]  (optional)                    ||
|  |  Premium Amount  [         ]   (Rs)                          ||
|  |                                                              ||
|  |  Employee Data (JSON)                                        ||
|  |  +----------------------------------------------------------+||
|  |  | {                                                        |||
|  |  |   "name": "Rajesh Kumar",                                |||
|  |  |   "dateOfBirth": "1990-05-15",                           |||
|  |  |   "relation": "SELF"                                     |||
|  |  | }                                                        |||
|  |  +----------------------------------------------------------+||
|  |                                                              ||
|  |  Idempotency Key  [auto-generated]   (optional override)    ||
|  |                                                              ||
|  |                              [Cancel]  [Create Endorsement]  ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**Validation rules (enforced by the backend `CreateEndorsementRequest` record):**
- `employeeId`, `employerId`, `insurerId`, `policyId` -- required UUIDs
- `type` -- must be one of ADD, DELETE, UPDATE
- `coverageStartDate` -- required, must not be in the past
- `premiumAmount` -- optional, non-negative
- `employeeData` -- required JSON map
- `idempotencyKey` -- auto-generated as `employerId-employeeId-type-coverageStartDate` if not provided

### 4.5 Page 5: EA Account Lookup (`/ea-accounts`)

Balance management for Employer Advance accounts.

```
+------------------------------------------------------------------+
|  EA Account Lookup                                               |
|  View employer advance account balances and transactions         |
|                                                                  |
|  Employer ID: [11111111-1111-1111-1111-111111111111          ]   |
|  Insurer ID:  [22222222-2222-2222-2222-222222222222          ]   |
|                                              [Search]            |
|                                                                  |
|  +--------------------------------------------------------------+|
|  | EA Account Balance                                           ||
|  |                                                              ||
|  |  Total Balance        Reserved           Available           ||
|  |  Rs 50,000            Rs 12,000          Rs 38,000          ||
|  |                                                              ||
|  |  [================green================|===amber===]         ||
|  |        76% available                     24% reserved        ||
|  |                                                              ||
|  |  Last updated: 13 Mar 2026 14:30                             ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  +--------------------------------------------------------------+|
|  | Transaction History                                          ||
|  |                                                              ||
|  | Date         Type     Amount      Balance    Endorsement     ||
|  | 13 Mar 14:30 DEBIT    Rs -5,000   Rs 50,000  a1b2c3d4..    ||
|  | 13 Mar 10:15 CREDIT   Rs +8,000   Rs 55,000  e5f6g7h8..    ||
|  | 12 Mar 16:45 DEBIT    Rs -3,000   Rs 47,000  i9j0k1l2..    ||
|  | 12 Mar 09:00 TOPUP    Rs +25,000  Rs 50,000  --             ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  +--------------------------------------------------------------+|
|  | Balance Optimization Recommendations                         ||
|  |                                                              ||
|  |  [!] 3 endorsements deferred due to insufficient balance.    ||
|  |      Top up Rs 15,000 to process all pending endorsements.   ||
|  |                                                              ||
|  |  Priority queue:                                             ||
|  |  P0 (DELETE):  2 endorsements  ->  credits Rs 8,000         ||
|  |  P1 (neutral): 0 endorsements                               ||
|  |  P2 (ADD):     5 endorsements  ->  debits Rs 25,000         ||
|  |  P3 (UPDATE):  1 endorsement   ->  debits Rs 2,500          ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**EA balance model:**
- `balance` = total deposited amount
- `reserved` = sum of premium amounts for endorsements in PROVISIONALLY_COVERED through INSURER_PROCESSING states
- `availableBalance` = `balance - reserved`

The available balance determines whether new endorsements can be processed. The batch optimizer (Phase 3) sequences endorsements to maximize throughput within available balance.

### 4.6 Page 6: EA Balance Optimization (`/ea-optimization`)

Visualization of the smart batch optimization algorithm.

```
+------------------------------------------------------------------+
|  EA Balance Optimization                                         |
|  Preview of batch assembly with priority sequencing              |
|                                                                  |
|  Current Available Balance: Rs 38,000                            |
|                                                                  |
|  +-------------------------------+------------------------------+|
|  | INCLUDED IN BATCH             | DEFERRED                     ||
|  |                               |                              ||
|  | Priority P0 (DELETE - credit) | Insufficient balance:        ||
|  | [DEL] emp-123  +Rs 5,000     | [ADD] emp-789  -Rs 15,000   ||
|  | [DEL] emp-456  +Rs 3,000     | [UPD] emp-012  -Rs 8,000    ||
|  |                               |                              ||
|  | Priority P1 (neutral)         |                              ||
|  | (none)                        |                              ||
|  |                               |                              ||
|  | Priority P2 (ADD - debit)     |                              ||
|  | [ADD] emp-234  -Rs 6,000     |                              ||
|  | [ADD] emp-567  -Rs 8,000     |                              ||
|  |                               |                              ||
|  | Priority P3 (UPDATE - debit)  |                              ||
|  | [UPD] emp-890  -Rs 2,500     |                              ||
|  +-------------------------------+------------------------------+|
|                                                                  |
|  Projected Balance After Batch:                                  |
|  +--------------------------------------------------------------+|
|  | Starting:   Rs 38,000                                        ||
|  | + Deletions:  Rs 8,000   (2 endorsements)                   ||
|  | - Additions: Rs -14,000   (2 endorsements)                   ||
|  | - Updates:   Rs -2,500    (1 endorsement)                    ||
|  | = Ending:    Rs 29,500                                       ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  Priority Sequencing:                                            |
|  P0 DELETE ---> P1 NEUTRAL ---> P2 ADD ---> P3 UPDATE           |
|  (credits      (no balance     (debits     (debits              |
|   first)        impact)         next)       last)               |
+------------------------------------------------------------------+
```

**Why this ordering matters:**

The batch optimizer processes DELETEs first because they release reserved balance (credits). This frees up funds for subsequent ADDs and UPDATEs. Without this optimization, a batch with 3 DELETEs and 3 ADDs might fail if processed in arrival order -- even though the net balance impact is zero.

### 4.7 Page 7: Multi-Insurer View (`/insurers`)

Cross-insurer comparison showing capabilities, pending counts, and SLA status.

```
+------------------------------------------------------------------+
|  Insurer Configurations                                          |
|  Active insurance provider integrations                          |
|                                                                  |
|  +--------------------+ +--------------------+ +----------------+|
|  | ICICI Lombard      | | Niva Bupa          | | Bajaj Allianz  ||
|  |                    | |                    | |                ||
|  | Modes: [RT]        | | Modes: [BATCH]     | | Modes: [RT]    ||
|  |                    | |                    | |       [BATCH]  ||
|  | Batch Size: --     | | Batch Size: 200    | | Batch Size: 150||
|  | SLA: 2 hours       | | SLA: 48 hours      | | SLA: 24 hours  ||
|  | Timeout: 30s       | | Timeout: 60s       | | Timeout: 45s   ||
|  |                    | |                    | |                ||
|  | Status: [Active]   | | Status: [Active]   | | Status: [Active]|
|  |                    | |                    | |                ||
|  | [View Details >]   | | [View Details >]   | | [View Details >]|
|  +--------------------+ +--------------------+ +----------------+|
|                                                                  |
|  Clicking "View Details" navigates to /insurers/:insurerId       |
|  showing full configuration, capabilities, and submission history |
+------------------------------------------------------------------+
```

**Insurer capabilities model** (from `InsurerConfiguration` domain entity):

```
Field                   Description
---------------------------------------------------------
insurerCode             Unique code (e.g., "ICICI_LOMBARD")
insurerName             Display name
supportsRealTime        true/false
supportsBatch           true/false
maxBatchSize            Max endorsements per batch submission
slaHours                Expected processing time SLA
timeoutSeconds          API call timeout
active                  Whether the insurer is currently active
```

### 4.8 Page 8: Intelligence Dashboard (`/intelligence`)

The intelligence layer provides four capabilities organized as tabs.

```
+------------------------------------------------------------------+
|  Intelligence Dashboard                                          |
|  AI-powered anomaly detection, forecasting, error resolution,    |
|  and process mining                                              |
|                                                                  |
|  [Anomalies] [Forecasts] [Error Resolution] [Process Mining]    |
|  ============                                                    |
|                                                                  |
|  (Tab content varies -- see sub-sections below)                  |
+------------------------------------------------------------------+
```

#### 4.8.1 Anomalies Tab

```
+------------------------------------------------------------------+
|  Flagged Anomalies                                               |
|  Endorsements flagged by the anomaly detection engine            |
|                                                                  |
|  Employer   Type            Score  Explanation       Status  Act |
|  ---------------------------------------------------------------+|
|  a1b2c3d4.. VOLUME SPIKE    [92%]  47 endorsements   FLAGGED    |
|             (red badge)            in 1 hour vs       [Review]   |
|                                    avg 5/hour         [Dismiss]  |
|                                                                  |
|  e5f6g7h8.. ADD DELETE      [85%]  Employee added    FLAGGED    |
|             CYCLING                and removed 3x     [Review]   |
|             (amber badge)          in 30 days         [Dismiss]  |
|                                                                  |
|  i9j0k1l2.. PREMIUM SPIKE  [78%]  Premium Rs 50K    UNDER      |
|             (amber badge)          vs avg Rs 5K      REVIEW      |
|                                                      --          |
+------------------------------------------------------------------+
```

**Anomaly types detected** (from `AnomalyType` enum):

```
Type               Trigger Condition                    Typical Score
------------------------------------------------------------------------
VOLUME_SPIKE       Endorsement count exceeds threshold  0.7 - 1.0
                   (>50% above rolling average)

ADD_DELETE_CYCLING  Same employee added and deleted      0.7 - 0.95
                   multiple times within 30 days

SUSPICIOUS_TIMING  Endorsements submitted at unusual    0.6 - 0.85
                   hours or patterns

UNUSUAL_PREMIUM    Premium amount significantly         0.7 - 0.9
                   deviates from employer norms

DORMANCY_BREAK     Employee with no activity for 90+    0.6 - 0.85
                   days suddenly has new endorsement

PREMIUM_SPIKE      Sudden increase in premium amounts   0.75 - 0.95
                   across multiple endorsements
```

**Anomaly score badge coloring:**
- Score >= 90%: red (destructive variant)
- Score >= 70%: amber (default variant)
- Score < 70%: gray (secondary variant)

**Review workflow:**
- FLAGGED -> UNDER_REVIEW (click "Review")
- FLAGGED -> DISMISSED (click "Dismiss")
- UNDER_REVIEW -> CONFIRMED_FRAUD (via API)
- UNDER_REVIEW -> DISMISSED (via API)

#### 4.8.2 Forecasts Tab

```
+------------------------------------------------------------------+
|  Balance Forecasts                                               |
|  30-day EA balance projections per employer. Select an employer  |
|  from the EA Accounts page to view their forecast.               |
|                                                                  |
|  Navigate to EA Accounts to view per-employer forecasts          |
|                                                                  |
|  (The forecasts tab links to the EA Accounts page because       |
|  forecasts are contextual to a specific employer+insurer pair.   |
|  The API endpoint GET /api/v1/intelligence/forecasts requires    |
|  employerId + insurerId parameters.)                             |
|                                                                  |
|  Forecast API response structure:                                |
|  +--------------------------------------------------------------+|
|  | Employer: 11111111...  Insurer: 22222222...                  ||
|  | Current Balance: Rs 50,000                                   ||
|  | Forecasted Need (30 days): Rs 75,000                         ||
|  | Projected Shortfall: Rs 25,000  [!!! ALERT]                  ||
|  | Confidence: 0.87                                             ||
|  | Generated At: 13 Mar 2026 06:00                              ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

#### 4.8.3 Error Resolution Tab

```
+------------------------------------------------------------------+
|  +----------+ +----------+ +----------+ +-----------+ +----------+ |
|  | Total    | | Auto-    | | Suggested| | Auto-Apply| | Success  | |
|  | Resolns  | | Applied  | | (pending)| | Rate      | | Rate     | |
|  |   142    | |   128    | |    14    | |  90.1%    | |  87.3%   | |
|  +----------+ +----------+ +----------+ +-----------+ +----------+ |
|                                                                  |
|  Recent Resolutions                                              |
|  +--------------------------------------------------------------+|
|  | Error Type    Original         Corrected       Conf  Auto    ||
|  |------------------------------------------------------------  ||
|  | DATE_FORMAT   2026/03/13       2026-03-13      98%   [Yes]  ||
|  | MISSING_FIELD --               SELF            95%   [Yes]  ||
|  | POLICY_NUM    POL123           POL-000123      92%   [Yes]  ||
|  | NAME_MISMATCH Raj Kumar        Rajesh Kumar    72%   [No]   ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**Error resolution logic** (from `SimulatedErrorResolver`):

The system learns from previously corrected errors. When a new error matches a known pattern with confidence >= 95% (`auto-apply-threshold` in config), the correction is applied automatically. Below 95%, the resolution is suggested for human review.

**Outcome tracking**: When an endorsement with a resolution reaches a terminal state (CONFIRMED, REJECTED, or FAILED_PERMANENT), `ErrorResolutionService.trackOutcome()` records the outcome. The stats endpoint now includes `successCount`, `failureCount`, and `successRate` to measure how effective resolutions are at driving successful endorsement confirmation.

#### 4.8.4 Process Mining Tab

```
+------------------------------------------------------------------+
|  +--------------------+ +--------------------+ +----------------+ |
|  | Overall STP Rate   | | ICICI Lombard      | | Bajaj Allianz  | |
|  |      87.3%          | |      92.1%          | |      81.5%      | |
|  +--------------------+ +--------------------+ +----------------+ |
|                                                                  |
|  Bottleneck Insights                              [Run Analysis] |
|  Workflow bottlenecks identified by process mining               |
|                                                                  |
|  +--------------------------------------------------------------+|
|  | [BOTTLENECK] Niva Bupa                                       ||
|  | Average batch processing time 36 hours exceeds SLA of        ||
|  | 48 hours. Approaching SLA breach threshold.                  ||
|  +--------------------------------------------------------------+|
|  | [BOTTLENECK] Bajaj Allianz                                   ||
|  | 18% rejection rate on real-time submissions due to           ||
|  | data format mismatches. Consider pre-validation rules.       ||
|  +--------------------------------------------------------------+|
|  | [OPTIMIZATION] ICICI Lombard                                 ||
|  | Real-time STP rate at 92%. Remaining 8% failures are         ||
|  | retryable -- increasing max retries could improve rate.      ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**STP Rate calculation:**

```
STP Rate = (Endorsements reaching CONFIRMED without any REJECTED state)
           / (Total endorsements reaching a terminal state)
           * 100

Per-insurer STP rate uses the same formula filtered by insurerId.
```

**STP Rate Trending:** The `ProcessMiningScheduler` captures daily STP rate snapshots into the `stp_rate_snapshots` table (V19 migration). Query historical trend data via `GET /api/v1/intelligence/process-mining/stp-rate/trend?insurerId={id}&days=30` to see how STP rates evolve over time per insurer. This enables identification of gradual degradation or improvement in insurer integration quality.

---

## 5. Grafana Operational Dashboards

**URL:** `http://localhost:3000` (admin/plum)
**Data Source:** Prometheus at `http://localhost:9090`
**Auto-refresh:** 10 seconds
**Auto-provisioned:** Dashboards and datasources are provisioned via mounted volumes in Docker Compose

### 5.1 Dashboard 1: Application Overview

**UID:** `endorsement-app-overview`
**Purpose:** High-level application health for on-call engineers

```
+------------------------------------------------------------------+
|  Endorsement Service - Application Overview        [Last 1h] [v] |
+------------------------------------------------------------------+
|                                                                  |
|  Request Metrics                                                 |
|  +------------------+ +------------------+ +------------------+  |
|  | Request Rate     | | Error Rate       | | Latency p99      |  |
|  | (per sec by URI) | | (4xx + 5xx)      | | (by endpoint)    |  |
|  |                  | |                  | |                  |  |
|  |  /api/v1/endor.. | |  0.2/sec         | |  p50: 12ms       |  |
|  |  2.5 req/s       | |  ----___----     | |  p95: 45ms       |  |
|  |  ___/--\___/--   | |                  | |  p99: 120ms      |  |
|  +------------------+ +------------------+ +------------------+  |
|                                                                  |
|  JVM Metrics                                                     |
|  +------------------+ +------------------+ +------------------+  |
|  | JVM Heap Usage   | | GC Pause Time    | | Thread Count     |  |
|  |                  | |                  | |                  |  |
|  |  256/512 MB      | |  avg 5ms         | |  42 active       |  |
|  |  [=====     ]    | |  p99 25ms        | |  (virtual)       |  |
|  +------------------+ +------------------+ +------------------+  |
|                                                                  |
|  Database                                                        |
|  +------------------+ +------------------+                       |
|  | DB Connections   | | Query Duration   |                       |
|  | Active: 8/20     | | avg 3ms          |                       |
|  +------------------+ +------------------+                       |
+------------------------------------------------------------------+
```

**Key Prometheus queries:**
- Request rate: `rate(http_server_requests_seconds_count{application="endorsement-service"}[5m])`
- Error rate: `rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])`
- Latency p99: `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))`

### 5.2 Dashboard 2: Endorsement Business

**UID:** `endorsement-business`
**Purpose:** Business-level endorsement metrics for product and operations

```
+------------------------------------------------------------------+
|  Endorsement Business Metrics                      [Last 1h] [v] |
+------------------------------------------------------------------+
|                                                                  |
|  +------------------+ +------------------+ +------------------+  |
|  | Creation Rate    | | Confirmation     | | Rejection Rate   |  |
|  | (per minute)     | | Rate             | |                  |  |
|  |                  | |                  | |                  |  |
|  |  ADD:  2.1/min   | |  87.3%           | |  8.2%            |  |
|  |  DEL:  0.8/min   | |  ---/--\---      | |  ___/--\___     |  |
|  |  UPD:  0.5/min   | |                  | |                  |  |
|  +------------------+ +------------------+ +------------------+  |
|                                                                  |
|  State Transitions Over Time                                     |
|  +--------------------------------------------------------------+|
|  |  CREATED +++++++++++++++++++++                               ||
|  |  VALIDATED ++++++++++++++++++                                ||
|  |  PROV_COV  ++++++++++++++++                                  ||
|  |  SUBMITTED   ++++++++++++                                    ||
|  |  CONFIRMED     ++++++++++                                    ||
|  |  REJECTED        ++                                          ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  Active Endorsement Gauge (by status)                            |
|  +--------------------------------------------------------------+|
|  |  CREATED:            5                                       ||
|  |  VALIDATED:          3                                       ||
|  |  PROV_COVERED:      12                                       ||
|  |  SUBMITTED_RT:       8                                       ||
|  |  QUEUED_BATCH:      15                                       ||
|  |  INSURER_PROC:       7                                       ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**Custom metrics used:**
- `endorsement.created` (Counter, tags: `type={ADD,DELETE,UPDATE}`)
- `endorsement.state.transition` (Counter, tags: `from`, `to`)
- `endorsement.active.count` (Gauge, tags: `status` -- 11 possible states)

### 5.3 Dashboard 3: Infrastructure Health

**UID:** `infrastructure-health`
**Purpose:** Infrastructure component health for DevOps/SRE

```
+------------------------------------------------------------------+
|  Infrastructure Health                             [Last 1h] [v] |
+------------------------------------------------------------------+
|                                                                  |
|  PostgreSQL                          Kafka                       |
|  +------------------+               +------------------+         |
|  | Active Conns     |               | Consumer Lag     |         |
|  | 8 / 20 max       |               | endorsement-svc  |         |
|  | [====        ]   |               | lag: 0           |         |
|  +------------------+               +------------------+         |
|                                                                  |
|  Redis                               JVM                        |
|  +------------------+               +------------------+         |
|  | Hit Rate         |               | Heap Usage       |         |
|  | 94.2%            |               | 256 / 512 MB     |         |
|  | [==========  ]   |               | [=====       ]   |         |
|  +------------------+               +------------------+         |
|                                                                  |
|  Kafka Event Publishing                                          |
|  +--------------------------------------------------------------+|
|  | Publish Rate (by event type)                                 ||
|  | ENDORSEMENT_CREATED:     2.1/min                             ||
|  | ENDORSEMENT_CONFIRMED:   1.8/min                             ||
|  | EA_DEBITED:              1.5/min                             ||
|  | ANOMALY_DETECTED:        0.3/min                             ||
|  | Publish Failures:        0.01/min                            ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**Custom metrics used:**
- `endorsement.kafka.publish` (Counter, tags: `result={success,failure}`, `eventType`)

### 5.4 Dashboard 4: Scheduler Monitoring

**UID:** `scheduler-monitoring`
**Purpose:** Monitoring the 7 scheduled jobs

```
+------------------------------------------------------------------+
|  Scheduler Monitoring                              [Last 6h] [v] |
+------------------------------------------------------------------+
|                                                                  |
|  Scheduler           Cron            Last Run   Duration  Status |
|  ------------------------------------------------------------------
|  Batch Assembly      */15 * * * *    14:30      1.2s      OK    |
|  Batch Poller        */15 * * * *    14:30      0.8s      OK    |
|  Coverage Cleanup    0 0 2 * * *     02:00      3.5s      OK    |
|  Anomaly Detection   */5 * * * *     14:35      2.1s      OK    |
|  Balance Forecast    0 0 6 * * *     06:00      4.2s      OK    |
|  Process Mining      0 0 3 * * *     03:00      8.7s      OK    |
|  Reconciliation      0 0 1 * * *     01:00      12.3s     OK    |
|                                                                  |
|  Execution Count (success vs failure)                            |
|  +--------------------------------------------------------------+|
|  | batch_assembly:      success=96  failure=0                   ||
|  | batch_poller:        success=96  failure=1                   ||
|  | coverage_cleanup:    success=1   failure=0                   ||
|  | anomaly_detection:   success=72  failure=0                   ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  Duration Histogram                                              |
|  +--------------------------------------------------------------+|
|  | batch_assembly    [====]        avg 1.2s                     ||
|  | batch_poller      [===]         avg 0.8s                     ||
|  | reconciliation    [===========] avg 12.3s                    ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**Custom metrics used:**
- `endorsement.scheduler.duration` (Timer, tags: `scheduler`, `result`)
- `endorsement.scheduler.execution` (Counter, tags: `scheduler`, `result`)

### 5.5 Dashboard 5: Multi-Insurer Monitoring

**UID:** `multi-insurer-monitoring`
**Purpose:** Per-insurer performance tracking and SLA compliance

```
+------------------------------------------------------------------+
|  Multi-Insurer Monitoring                          [Last 1h] [v] |
+------------------------------------------------------------------+
|                                                                  |
|  Submission Rate by Insurer                                      |
|  +--------------------------------------------------------------+|
|  |  ICICI Lombard (RT):    1.2/min  success=95%  failure=5%    ||
|  |  Niva Bupa (Batch):     0.5/min  success=92%  failure=8%    ||
|  |  Bajaj Allianz (RT+B):  0.8/min  success=88%  failure=12%   ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  Submission Duration by Insurer                                  |
|  +--------------------------------------------------------------+|
|  | icici_lombard    p50=120ms  p95=250ms  p99=500ms             ||
|  | bajaj_allianz    p50=200ms  p95=450ms  p99=800ms             ||
|  | niva_bupa        p50=150ms  p95=300ms  p99=600ms             ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  SLA Compliance                                                  |
|  +--------------------------------------------------------------+|
|  | ICICI Lombard    SLA: 2h    avg: 0.5h     [OK]              ||
|  | Niva Bupa        SLA: 48h   avg: 36h      [WARNING]         ||
|  | Bajaj Allianz    SLA: 24h   avg: 18h      [OK]              ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  Circuit Breaker Status                                          |
|  +--------------------------------------------------------------+|
|  | insurerSubmission   CLOSED  (healthy)                        ||
|  | iciciLombard        CLOSED  (healthy)                        ||
|  | bajajAllianz        CLOSED  (healthy)                        ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**Custom metrics used:**
- `endorsement.insurer.mock.duration` / `endorsement.insurer.icici.duration` / etc. (Timer)
- Circuit breaker metrics via Resilience4j + Actuator

### 5.6 Dashboard 6: Reconciliation Monitoring

**UID:** `reconciliation-monitoring`
**Purpose:** Tracking reconciliation runs and discrepancy detection

```
+------------------------------------------------------------------+
|  Reconciliation Monitoring                         [Last 24h][v] |
+------------------------------------------------------------------+
|                                                                  |
|  +------------------+ +------------------+ +------------------+  |
|  | Total Runs       | | Match Rate       | | Discrepancies    |  |
|  |    12             | |   94.2%           | |    23             |  |
|  +------------------+ +------------------+ +------------------+  |
|                                                                  |
|  Reconciliation History                                          |
|  +--------------------------------------------------------------+|
|  | Run Time      Insurer         Matched  Discrepancy  Missing  ||
|  | 01:00 today   ICICI Lombard   45       2            1        ||
|  | 01:00 today   Niva Bupa       30       5            3        ||
|  | 01:00 today   Bajaj Allianz   28       3            0        ||
|  | 01:00 yest.   ICICI Lombard   42       1            0        ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  Discrepancy Trend                                               |
|  +--------------------------------------------------------------+|
|  |     *                                                        ||
|  |    * *                                                       ||
|  |   *   *                                                      ||
|  |  *     *   *                                                 ||
|  | *       * * *                                                ||
|  |          *                                                   ||
|  | Mon  Tue  Wed  Thu  Fri  Sat  Sun                            ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**Custom metrics used:**
- `endorsement.reconciliation.completed` (Counter, tags: `insurerId`)
- `endorsement.reconciliation.matched` (Gauge)
- `endorsement.reconciliation.discrepancies` (Gauge)

### 5.7 Dashboard 7: Intelligence Monitoring

**UID:** `intelligence-monitoring`
**Purpose:** Phase 3 AI/intelligence feature health and effectiveness

```
+------------------------------------------------------------------+
|  Intelligence Monitoring                           [Last 6h] [v] |
+------------------------------------------------------------------+
|                                                                  |
|  Anomaly Detection                                               |
|  +------------------+ +------------------+ +------------------+  |
|  | Detection Rate   | | Avg Score        | | Score Heatmap    |  |
|  | 0.3/min          | | 0.82             | | [heat map by     |  |
|  |  ___/--\___      | |                  | |  anomaly type]   |  |
|  +------------------+ +------------------+ +------------------+  |
|                                                                  |
|  Forecasting                                                     |
|  +------------------+ +------------------+                       |
|  | Forecasts Gen.   | | Shortfall Alerts |                       |
|  | 24 (last 24h)    | | 3 active         |                       |
|  +------------------+ +------------------+                       |
|                                                                  |
|  Error Resolution                                                |
|  +------------------+ +------------------+ +------------------+  |
|  | Auto-Resolved    | | Suggested        | | Resolution Rate  |  |
|  | 128              | | 14               | | 90.1%            |  |
|  +------------------+ +------------------+ +------------------+  |
|                                                                  |
|  STP Rate Trend                                                  |
|  +--------------------------------------------------------------+|
|  | 100% |                                                       ||
|  |  95% |     *---*                                             ||
|  |  90% | *--*     *--*---*                                     ||
|  |  85% |                  *--*                                 ||
|  |  80% |                      *                                ||
|  |      +----+----+----+----+----+----+----+                   ||
|  |       Mon  Tue  Wed  Thu  Fri  Sat  Sun                     ||
|  +--------------------------------------------------------------+|
|                                                                  |
|  Batch Optimization Savings                                      |
|  +--------------------------------------------------------------+|
|  | Total saved: Rs 2,45,000 (last 30 days)                     ||
|  | Avg savings per batch: Rs 8,500                              ||
|  +--------------------------------------------------------------+|
+------------------------------------------------------------------+
```

**Custom metrics used (Phase 3):**
- `endorsement.anomaly.detected` (Counter, tags: `anomalyType`, `employerId`)
- `endorsement.anomaly.score` (Summary, tags: `anomalyType`)
- `endorsement.forecast.generated` (Counter)
- `endorsement.forecast.shortfall.detected` (Counter)
- `endorsement.error.auto_resolved` (Counter, tags: `errorType`, `insurerId`)
- `endorsement.error.suggested` (Counter, tags: `errorType`)
- `endorsement.process.stp_rate` (Gauge, tags: `insurerId`)
- `endorsement.batch.optimization.savings` (Summary, tags: `strategy`)

---

## 6. API-Driven Visibility -- Swagger UI

**URL:** `http://localhost:8080/swagger-ui`
**OpenAPI docs:** `http://localhost:8080/api-docs`

The REST API serves as the data layer for all visibility tools. The frontend calls these endpoints; Swagger UI makes them accessible for debugging, demo, and integration.

### 6.1 Complete API Endpoint Inventory

```
Controller                    Method  Endpoint                              Purpose
-----------------------------------------------------------------------------------------------------
EndorsementController         POST    /api/v1/endorsements                  Create endorsement
                              GET     /api/v1/endorsements/{id}             Get endorsement by ID
                              GET     /api/v1/endorsements                  List endorsements (paginated)
                              POST    /api/v1/endorsements/{id}/submit      Submit to insurer
                              POST    /api/v1/endorsements/{id}/confirm     Confirm endorsement
                              POST    /api/v1/endorsements/{id}/reject      Reject endorsement
                              GET     /api/v1/endorsements/{id}/coverage    Get provisional coverage

EAAccountController           GET     /api/v1/ea-accounts                   Get EA account balance

InsurerConfigController       GET     /api/v1/insurers                      List active insurers
                              GET     /api/v1/insurers/{id}                 Get insurer config
                              GET     /api/v1/insurers/{id}/capabilities    Get insurer capabilities

ReconciliationController      GET     /api/v1/reconciliation/runs           Get reconciliation runs
                              GET     /api/v1/reconciliation/runs/{id}/items Get run items
                              POST    /api/v1/reconciliation/trigger        Trigger reconciliation

IntelligenceController        GET     /api/v1/intelligence/anomalies        List anomalies
                              GET     /api/v1/intelligence/anomalies/{id}   Get anomaly
                              PUT     /api/v1/intelligence/anomalies/{id}/review  Review anomaly
                              GET     /api/v1/intelligence/forecasts        Get latest forecast
                              GET     /api/v1/intelligence/forecasts/history Get forecast history
                              POST    /api/v1/intelligence/forecasts/generate Generate forecast
                              GET     /api/v1/intelligence/error-resolutions List error resolutions
                              GET     /api/v1/intelligence/error-resolutions/stats Get stats
                              POST    /api/v1/intelligence/error-resolutions/resolve Resolve error
                              POST    /api/v1/intelligence/error-resolutions/{id}/approve Approve
                              GET     /api/v1/intelligence/process-mining/metrics Get metrics
                              GET     /api/v1/intelligence/process-mining/insights Get insights
                              GET     /api/v1/intelligence/process-mining/stp-rate Get STP rate
                              GET     /api/v1/intelligence/process-mining/stp-rate/trend Get STP rate trend
                              POST    /api/v1/intelligence/process-mining/analyze Trigger analysis

Spring Actuator               GET     /actuator/health                      Health check
                              GET     /actuator/prometheus                  Metrics endpoint
                              GET     /actuator/info                        App info
```

**Total: 29 application endpoints + 3 actuator endpoints = 32 endpoints**

### 6.2 Error Response Format

All error responses follow RFC 7807 Problem Detail format, implemented via `GlobalExceptionHandler`:

```json
{
  "type": "about:blank",
  "title": "Endorsement Not Found",
  "status": 404,
  "detail": "Endorsement with id a1b2c3d4-... not found",
  "instance": "/api/v1/endorsements/a1b2c3d4-..."
}
```

Exception-to-status mapping:

```
Exception                        HTTP Status   Error Metric Tag
----------------------------------------------------------------
EndorsementNotFoundException     404           not_found
DuplicateEndorsementException    409           duplicate
InsufficientBalanceException     422           insufficient_balance
InsurerNotFoundException         404           not_found
IllegalStateException            409           illegal_state
MethodArgumentNotValidException  400           validation
Exception (catch-all)            500           unexpected
```

### 6.3 Pagination Format

List endpoints return Spring `Page<T>` responses:

```json
{
  "content": [ ... ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 47,
  "totalPages": 3,
  "first": true,
  "last": false,
  "empty": false,
  "numberOfElements": 20,
  "size": 20,
  "number": 0
}
```

---

## 7. Event-Driven Real-Time Updates

### 7.1 Kafka Event Architecture

Every state change in the system publishes an event to the `endorsement-events` Kafka topic. This is the foundation for real-time visibility.

**Topic:** `endorsement-events`
**Partition key:** `employerId` (ensures all events for one employer are ordered)
**Serialization:** JSON via Jackson `ObjectMapper`

### 7.2 Complete Event Type Inventory

```
Category              Event Type                       When Published
---------------------------------------------------------------------------
Lifecycle             ENDORSEMENT_CREATED              New endorsement created
                      ENDORSEMENT_VALIDATED            Validation passed
                      PROVISIONAL_COVERAGE_GRANTED     Provisional coverage assigned
                      ENDORSEMENT_SUBMITTED_REALTIME   Submitted via real-time API
                      ENDORSEMENT_QUEUED_FOR_BATCH     Added to batch queue
                      BATCH_SUBMITTED                  Batch sent to insurer
                      INSURER_PROCESSING               Insurer acknowledged
                      ENDORSEMENT_CONFIRMED            Insurer confirmed
                      ENDORSEMENT_REJECTED             Insurer rejected
                      ENDORSEMENT_RETRY_SCHEDULED      Retry queued
                      ENDORSEMENT_FAILED_PERMANENT     Max retries exhausted

Financial             EA_DEBITED                       Balance reserved for endorsement
                      EA_CREDITED                      Balance released (rejection/deletion)

Reconciliation        RECONCILIATION_MATCHED           Endorsement matched insurer record
                      RECONCILIATION_DISCREPANCY       Mismatch detected
                      RECONCILIATION_MISSING           Endorsement not found at insurer

Coverage              PROVISIONAL_COVERAGE_EXPIRED     Coverage expired (30-day max)
                      PROVISIONAL_COVERAGE_CONFIRMED   Coverage upgraded to confirmed
                      BALANCE_FORECAST_ALERT           Shortfall predicted

Intelligence          ANOMALY_DETECTED                 Anomaly flagged
                      FORECAST_GENERATED               Balance forecast created
                      BATCH_OPTIMIZED                  Batch optimization applied
                      ERROR_AUTO_RESOLVED              Error automatically corrected
                      ERROR_RESOLUTION_SUGGESTED       Correction suggested (not applied)
                      PROCESS_MINING_INSIGHT           New bottleneck insight
```

**Total: 24 event types**

### 7.3 Event Structure

All events implement the `EndorsementEvent` sealed interface:

```java
public sealed interface EndorsementEvent {
    UUID endorsementId();     // Which endorsement
    Instant occurredAt();     // When it happened
    UUID employerId();        // Partition key
    String eventType();       // Discriminator
}
```

Each event record adds context-specific fields:

```
ENDORSEMENT_CREATED      + employeeId, type (ADD/DELETE/UPDATE)
EA_DEBITED               + amount (BigDecimal)
ANOMALY_DETECTED         + anomalyType, anomalyScore, explanation
FORECAST_GENERATED       + forecastedNeed, daysAhead, narrative
BATCH_OPTIMIZED          + batchId, optimizationStrategy, savedAmount
ERROR_AUTO_RESOLVED      + errorType, resolution, autoApplied
PROCESS_MINING_INSIGHT   + insightType, insight
```

### 7.4 Why employerId as Partition Key

```
Kafka Topic: endorsement-events
+-------------------------------------------------------------------+
| Partition 0 (employer-A)  | Partition 1 (employer-B)              |
|                           |                                       |
| CREATED(emp-A, e1)        | CREATED(emp-B, e4)                   |
| VALIDATED(emp-A, e1)      | EA_DEBITED(emp-B, e4)                |
| EA_DEBITED(emp-A, e1)     | VALIDATED(emp-B, e4)                 |
| CREATED(emp-A, e2)        | CONFIRMED(emp-B, e4)                 |
| EA_DEBITED(emp-A, e2)     |                                       |
| CONFIRMED(emp-A, e1)      |                                       |
| EA_CREDITED(emp-A, e1)    |                                       |
+-------------------------------------------------------------------+

Guarantee: All events for employer-A are ordered.
This matters for: EA balance calculations, coverage tracking,
                  anomaly detection (requires temporal ordering).
```

### 7.5 Future: WebSocket Integration

The current architecture polls via TanStack Query (30-second stale time). The planned evolution:

```
Current (Phase 1-3):
  Frontend --> HTTP GET (polling every 30s) --> Spring Boot --> PostgreSQL

Future (Phase 4):
  Kafka --> WebSocket Bridge --> Spring WebSocket --> Frontend (instant push)

  +----------+     +----------+     +----------+     +----------+
  | Kafka    | --> | WS Bridge| --> | Spring   | --> | React    |
  | Consumer |     | Service  |     | WebSocket|     | useWS()  |
  +----------+     +----------+     +----------+     +----------+
                                         |
                                    Pushes to specific
                                    employer channels:
                                    /ws/employer/{id}/events
```

**Why not WebSocket now?** The MVP prioritizes correctness over latency. Polling with 30-second intervals is sufficient for the current scale (10K endorsements/day). WebSocket adds operational complexity (connection management, reconnection logic, message ordering). It becomes necessary at Phase 4 scale (1M endorsements/day, 5000 concurrent dashboard users).

---

## 8. Observability Stack Integration

### 8.1 Full Observability Architecture

```
+------------------------------------------------------------------+
|                    OBSERVABILITY STACK                            |
+------------------------------------------------------------------+
|                                                                  |
|  +------------------+          +------------------+              |
|  | Spring Boot App  | -------> | Micrometer       |              |
|  | (port 8080)      |          | (in-process)     |              |
|  +--------+---------+          +--------+---------+              |
|           |                             |                        |
|           |  /actuator/prometheus        |                        |
|           |                             v                        |
|           |                    +------------------+              |
|           |                    | Prometheus       |              |
|           |                    | (port 9090)      |              |
|           |                    | scrape: 15s      |              |
|           |                    +--------+---------+              |
|           |                             |                        |
|           |                             v                        |
|           |                    +------------------+              |
|           |                    | Grafana          |              |
|           |                    | (port 3000)      |              |
|           |                    | 7 dashboards     |              |
|           |                    +------------------+              |
|           |                                                      |
|           |  OTLP (gRPC, port 4317)                              |
|           +----------------------------+                         |
|           |                            v                         |
|           |                    +------------------+              |
|           |                    | Jaeger           |              |
|           |                    | (port 16686)     |              |
|           |                    | Distributed      |              |
|           |                    | Traces           |              |
|           |                    +------------------+              |
|           |                                                      |
|           |  JSON logs (TCP, port 5000)                           |
|           +----------------------------+                         |
|           |                            v                         |
|           |                    +------------------+              |
|           |                    | Logstash         |              |
|           |                    | (port 5000)      |              |
|           |                    +--------+---------+              |
|           |                             |                        |
|           |                             v                        |
|           |                    +------------------+              |
|           |                    | Elasticsearch    |              |
|           |                    | (port 9200)      |              |
|           |                    +--------+---------+              |
|           |                             |                        |
|           |                             v                        |
|           |                    +------------------+              |
|           |                    | Kibana           |              |
|           |                    | (port 5601)      |              |
|           |                    +------------------+              |
|           |                                                      |
|           |  Kafka events (port 9092)                             |
|           +----------------------------+                         |
|                                        v                         |
|                                +------------------+              |
|                                | Kafka            |              |
|                                | endorsement-     |              |
|                                | events topic     |              |
|                                +------------------+              |
|                                                                  |
|  +------------------+                                            |
|  | Spring Actuator  |                                            |
|  | /actuator/health |  <-- Health checks (PostgreSQL, Redis,     |
|  |                  |      Kafka, disk space)                    |
|  +------------------+                                            |
|                                                                  |
+------------------------------------------------------------------+
```

### 8.2 Service Ports Summary

```
Service             Port    Purpose                   Stakeholder
----------------------------------------------------------------------
Spring Boot App     8080    REST API + Swagger UI     Developers, Frontend
React Frontend      5173    Dashboard UI              HR, Finance, Employers
Grafana             3000    Operational dashboards    Operations, Platform
Prometheus          9090    Metrics collection        (internal)
Jaeger              16686   Distributed tracing       Developers, Operations
Elasticsearch       9200    Log storage               (internal)
Kibana              5601    Log search + analysis     Operations, Developers
Kafka               9092    Event streaming           (internal)
PostgreSQL          5432    Primary data store        (internal)
Redis               6379    Cache                     (internal)
Logstash            5000    Log ingestion             (internal)
```

### 8.3 Structured Logging with MDC

Every log entry includes contextual fields via MDC (Mapped Diagnostic Context):

```
{
  "timestamp": "2026-03-13T14:30:22.123Z",
  "level": "INFO",
  "logger": "c.p.e.a.handler.CreateEndorsementHandler",
  "message": "Creating endorsement for employer=..., employee=..., type=ADD",
  "endorsementId": "a1b2c3d4-...",
  "employerId": "11111111-...",
  "traceId": "abc123def456",
  "spanId": "789ghi012",
  "kafkaEventType": "ENDORSEMENT_CREATED"
}
```

This enables:
- **Kibana:** Filter logs by `endorsementId` to see the complete lifecycle
- **Jaeger:** Trace a request across all internal method calls
- **Grafana:** Correlate metrics spikes with log patterns

### 8.4 Health Check Endpoints

```
GET /actuator/health

{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "redis": { "status": "UP" },
    "kafka": { "status": "UP" },
    "diskSpace": { "status": "UP", "details": { "free": "45GB" } },
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "insurerSubmission": "CLOSED",
        "iciciLombard": "CLOSED",
        "bajajAllianz": "CLOSED"
      }
    }
  }
}
```

---

## 9. User Flow Diagrams

### 9.1 HR Admin: Create and Track Endorsement

```
HR Admin                    Frontend                  Backend                 Insurer
   |                           |                         |                      |
   |  1. Navigate to           |                         |                      |
   |     /endorsements/new     |                         |                      |
   |-------------------------->|                         |                      |
   |                           |                         |                      |
   |  2. Fill form:            |                         |                      |
   |     employee, type=ADD,   |                         |                      |
   |     coverage dates,       |                         |                      |
   |     premium               |                         |                      |
   |-------------------------->|                         |                      |
   |                           |  3. POST /endorsements  |                      |
   |                           |------------------------>|                      |
   |                           |                         |                      |
   |                           |                         | 4. Validate          |
   |                           |                         |    Create            |
   |                           |                         |    Grant provisional |
   |                           |                         |    Reserve EA balance|
   |                           |                         |    Publish events    |
   |                           |                         |                      |
   |                           |  5. 201 Created         |                      |
   |                           |<------------------------|                      |
   |                           |                         |                      |
   |  6. Redirect to           |                         |                      |
   |     /endorsements/:id     |                         |                      |
   |<--------------------------|                         |                      |
   |                           |                         |                      |
   |  7. See status timeline:  |                         |                      |
   |     CREATED -> VALIDATED  |                         |                      |
   |     -> PROV. COVERED      |                         |                      |
   |     Coverage card: yellow |                         |                      |
   |     [PROVISIONAL]         |                         |                      |
   |                           |                         |                      |
   |          ... time passes (batch assembly runs) ...                         |
   |                           |                         |                      |
   |  8. Refresh page          |  9. GET /endorsements/:id                     |
   |  (or auto-poll 30s)       |------------------------>|                      |
   |                           |                         |                      |
   |  10. Status timeline      |                         |                      |
   |      updates:             |                         |                      |
   |      -> QUEUED_FOR_BATCH  |                         |                      |
   |      -> BATCH_SUBMITTED   |                         |                      |
   |                           |                         |  11. Batch sent      |
   |                           |                         |--------------------->|
   |                           |                         |                      |
   |          ... insurer processes batch ...                                   |
   |                           |                         |                      |
   |                           |                         |  12. Batch response  |
   |                           |                         |<---------------------|
   |                           |                         |                      |
   |  13. Refresh page         |  14. GET /endorsements/:id                    |
   |                           |------------------------>|                      |
   |                           |                         |                      |
   |  15. Status timeline      |                         |                      |
   |      CONFIRMED (green)    |                         |                      |
   |      Coverage card:       |                         |                      |
   |      [CONFIRMED] (green)  |                         |                      |
   |                           |                         |                      |
   |  DONE: Employee is        |                         |                      |
   |  confirmed covered.       |                         |                      |
```

### 9.2 Finance Team: Monitor Balance and Respond to Forecast Alert

```
Finance Team                Frontend                  Backend
   |                           |                         |
   |  1. Navigate to           |                         |
   |     /ea-accounts          |                         |
   |-------------------------->|                         |
   |                           |  2. GET /ea-accounts    |
   |                           |     ?employerId=...     |
   |                           |     &insurerId=...      |
   |                           |------------------------>|
   |                           |                         |
   |  3. See balance card:     |                         |
   |     Balance: Rs 50,000    |                         |
   |     Reserved: Rs 12,000   |                         |
   |     Available: Rs 38,000  |                         |
   |<--------------------------|                         |
   |                           |                         |
   |  4. Navigate to           |                         |
   |     /intelligence         |                         |
   |     -> Forecasts tab      |                         |
   |-------------------------->|                         |
   |                           |  5. GET /intelligence/  |
   |                           |     forecasts           |
   |                           |     ?employerId=...     |
   |                           |     &insurerId=...      |
   |                           |------------------------>|
   |                           |                         |
   |  6. See forecast:         |                         |
   |     Forecasted need:      |                         |
   |       Rs 75,000 (30 days) |                         |
   |     Current balance:      |                         |
   |       Rs 50,000           |                         |
   |     SHORTFALL ALERT:      |                         |
   |       Rs 25,000 deficit   |                         |
   |<--------------------------|                         |
   |                           |                         |
   |  7. Decision:             |                         |
   |     Initiate top-up of    |                         |
   |     Rs 25,000 to EA       |                         |
   |     account               |                         |
   |                           |                         |
   |  8. Navigate to           |                         |
   |     /ea-optimization      |                         |
   |     to review batch plan  |                         |
   |-------------------------->|                         |
   |                           |                         |
   |  9. See optimization:     |                         |
   |     DELETEs first         |                         |
   |     (release Rs 8,000)    |                         |
   |     then ADDs             |                         |
   |     (consume Rs 14,000)   |                         |
   |     Net: Rs 29,500        |                         |
   |     remaining             |                         |
```

### 9.3 Operations: Investigate Anomaly Alert

```
Ops Engineer                Grafana / Frontend         Backend
   |                           |                         |
   |  1. Grafana alert:        |                         |
   |     Intelligence dashboard|                         |
   |     shows anomaly rate    |                         |
   |     spike                 |                         |
   |<--------------------------|                         |
   |                           |                         |
   |  2. Navigate to           |                         |
   |     Frontend /intelligence|                         |
   |     -> Anomalies tab      |                         |
   |-------------------------->|                         |
   |                           |  3. GET /intelligence/  |
   |                           |     anomalies           |
   |                           |     ?status=FLAGGED     |
   |                           |------------------------>|
   |                           |                         |
   |  4. See anomaly:          |                         |
   |     Type: VOLUME_SPIKE    |                         |
   |     Score: 92%            |                         |
   |     Employer: a1b2c3d4..  |                         |
   |     "47 endorsements in   |                         |
   |      1 hour vs avg 5/hr"  |                         |
   |<--------------------------|                         |
   |                           |                         |
   |  5. Click [Review]        |                         |
   |-------------------------->|                         |
   |                           |  6. PUT /intelligence/  |
   |                           |     anomalies/{id}/     |
   |                           |     review              |
   |                           |     {status: UNDER_     |
   |                           |      REVIEW}            |
   |                           |------------------------>|
   |                           |                         |
   |  7. Investigate:          |                         |
   |     Check employer's      |                         |
   |     endorsement history   |                         |
   |     in /endorsements      |                         |
   |     (filtered by employer)|                         |
   |-------------------------->|                         |
   |                           |                         |
   |  8. See: Employer is      |                         |
   |     onboarding 47 new     |                         |
   |     employees -- legitimate|                        |
   |     business event        |                         |
   |                           |                         |
   |  9. Click [Dismiss]       |                         |
   |     with note: "Legitimate|                         |
   |     bulk onboarding"      |                         |
   |-------------------------->|                         |
   |                           |  10. PUT /intelligence/ |
   |                           |      anomalies/{id}/    |
   |                           |      review             |
   |                           |      {status: DISMISSED,|
   |                           |       notes: "..."}     |
   |                           |------------------------>|
   |                           |                         |
   |  RESOLVED: Anomaly        |                         |
   |  dismissed, audit trail   |                         |
   |  preserved.               |                         |
```

### 9.4 Operations: Diagnose Insurer SLA Breach

```
Ops Engineer                Grafana                    Backend
   |                           |                         |
   |  1. Multi-Insurer         |                         |
   |     dashboard shows       |                         |
   |     Niva Bupa SLA:        |                         |
   |     avg 36h vs 48h SLA    |                         |
   |     [WARNING]             |                         |
   |<--------------------------|                         |
   |                           |                         |
   |  2. Check Process Mining  |                         |
   |     in Frontend           |                         |
   |     /intelligence         |                         |
   |     -> Process Mining tab |                         |
   |                           |  3. GET /intelligence/  |
   |                           |     process-mining/     |
   |                           |     stp-rate            |
   |                           |     ?insurerId=...      |
   |                           |------------------------>|
   |                           |                         |
   |  4. See: Niva Bupa STP    |                         |
   |     rate 78% (below 87%   |                         |
   |     system average)       |                         |
   |                           |                         |
   |  5. Click [Run Analysis]  |                         |
   |                           |  6. POST /intelligence/ |
   |                           |     process-mining/     |
   |                           |     analyze             |
   |                           |------------------------>|
   |                           |                         |
   |  7. See bottleneck        |                         |
   |     insight:              |                         |
   |     "22% rejection rate   |                         |
   |      on CSV format errors.|                         |
   |      Pre-validation       |                         |
   |      recommended."        |                         |
   |                           |                         |
   |  8. Check Error           |                         |
   |     Resolution tab        |                         |
   |                           |  9. GET /intelligence/  |
   |                           |     error-resolutions/  |
   |                           |     stats               |
   |                           |------------------------>|
   |                           |                         |
   |  10. See: 85% of Niva     |                         |
   |      Bupa errors are      |                         |
   |      auto-resolved, but   |                         |
   |      DATE_FORMAT errors   |                         |
   |      only 60% auto-rate   |                         |
   |                           |                         |
   |  11. Action:              |                         |
   |      Add pre-validation   |                         |
   |      rule for date format |                         |
   |      in NivaBupaCsvMapper |                         |
```

---

## 10. Design Decisions and Trade-Offs

### 10.1 Polling vs WebSocket

```
Decision: Use HTTP polling (30s intervals) in Phase 1-3

+---------------------------+----------------------------+
| Polling (chosen)          | WebSocket (Phase 4)        |
+---------------------------+----------------------------+
| Simple implementation     | Complex connection mgmt    |
| Stateless (no connection  | Stateful (connection pool, |
|  management)              |  reconnection, heartbeat)  |
| Works through all proxies | Some proxies break WS      |
| 30s latency acceptable    | Sub-second latency         |
|  at current scale         |  needed at 5K+ concurrent  |
| No additional infra       | Needs WS bridge service    |
+---------------------------+----------------------------+

When to switch: >500 concurrent dashboard users OR
                real-time notification requirements
```

### 10.2 Grafana vs Custom Dashboards

```
Decision: Use both Grafana AND custom React dashboard

+---------------------------+----------------------------+
| Grafana (7 dashboards)    | React Dashboard (8 pages)  |
+---------------------------+----------------------------+
| Infrastructure metrics    | Business operations        |
| Prometheus-native queries | REST API data              |
| Time-series focus         | Record-level detail        |
| Alerting built-in         | Actions (create, review)   |
| Ops/SRE audience          | HR/Finance audience        |
| Pre-provisioned, zero-code| Custom UI components       |
+---------------------------+----------------------------+

Why both? Different stakeholders need different views of
the same system. Grafana excels at time-series metrics;
React excels at interactive business workflows.
```

### 10.3 Event Granularity

```
Decision: Publish 24 fine-grained event types (not 3-5 coarse ones)

Why fine-grained:
1. Anomaly detection needs temporal patterns (not just current state)
2. Balance forecasting needs debit/credit events (not just final balance)
3. Process mining needs every state transition (to compute STP rate)
4. Audit trail requires complete history (compliance requirement)
5. Future consumers can filter to the events they need

Trade-off: Higher event volume (~5 events per endorsement lifecycle)
           vs. richer visibility and analytics capability
```

### 10.4 Employer-Partitioned Events

```
Decision: Use employerId as Kafka partition key

Why not endorsementId:
- EA balance calculations need ordered events per employer
- Anomaly detection compares patterns across one employer's endorsements
- Finance views are employer-scoped

Trade-off: Events for the same employer are strictly ordered,
           but events across employers are processed in parallel.
           At scale, a very active employer could create a hot partition.
           Mitigation: sub-partitioning at Phase 4 scale.
```

### 10.5 Metric Naming Convention

```
Decision: Use hierarchical metric names with consistent tag taxonomy

Pattern: endorsement.{domain}.{metric}
Tags:    type, status, result, insurerId, anomalyType, errorType, scheduler

Examples:
  endorsement.created{type=ADD}
  endorsement.state.transition{from=CREATED,to=VALIDATED}
  endorsement.insurer.icici.duration{method=submitRealTime}
  endorsement.anomaly.detected{anomalyType=VOLUME_SPIKE}
  endorsement.scheduler.duration{scheduler=batch_assembly,result=success}

Why: Consistent naming enables dashboard templates, alerting rules,
     and cross-metric correlation in Grafana.
```

---

## Summary

The Plum Endorsement Management System provides four layers of real-time visibility:

| Layer | Tool | URL | Audience | Refresh Rate |
|-------|------|-----|----------|-------------|
| Business Dashboard | React + Vite | `localhost:5173` | HR, Finance, Employers | 30s polling |
| Operational Dashboards | Grafana (7) | `localhost:3000` | Operations, Platform | 10s |
| API Visibility | Swagger UI | `localhost:8080/swagger-ui` | Developers | On-demand |
| Event Stream | Kafka (24 types) | `localhost:9092` | Internal consumers | Sub-second |

Supporting infrastructure:
- **Prometheus** (`localhost:9090`): Scrapes 40+ custom metrics every 15 seconds
- **Jaeger** (`localhost:16686`): Distributed tracing with 100% sampling in development
- **ELK Stack** (`localhost:5601`): Structured log aggregation with MDC context
- **Spring Actuator** (`localhost:8080/actuator/health`): Health checks for all dependencies

Every stakeholder question -- "Is this employee covered?", "Do we have enough EA balance?", "Why is this insurer slow?", "Is this anomaly real?" -- can be answered within seconds through the appropriate visibility layer.
