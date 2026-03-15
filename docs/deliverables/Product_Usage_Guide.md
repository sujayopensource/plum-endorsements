# Plum Endorsement Service — Product Usage Guide

> **Audience**: Engineers, Product Managers, Designers, C-Level Executives
> **Version**: 1.0 | March 13, 2026
> **Application URL**: `http://localhost:5173` (frontend) | `http://localhost:8080` (API)

---

## Table of Contents

1. [What is the Plum Endorsement Service?](#1-what-is-the-plum-endorsement-service)
2. [Quick Start — First 5 Minutes](#2-quick-start--first-5-minutes)
   - [Starting the Application](#starting-the-application)
   - [Your First Endorsement in 90 Seconds](#your-first-endorsement-in-90-seconds)
   - [Running Tests](#running-tests)
3. [Navigation & Layout](#3-navigation--layout)
4. [Screen-by-Screen Guide](#4-screen-by-screen-guide)
   - [4.1 Dashboard](#41-dashboard)
   - [4.2 Endorsements List](#42-endorsements-list)
   - [4.3 Create Endorsement](#43-create-endorsement)
   - [4.4 Endorsement Detail](#44-endorsement-detail)
   - [4.5 Batch Progress](#45-batch-progress)
   - [4.6 EA Accounts](#46-ea-accounts)
   - [4.7 Insurers](#47-insurers)
   - [4.8 Insurer Detail](#48-insurer-detail)
   - [4.9 Reconciliation](#49-reconciliation)
   - [4.10 Intelligence Dashboard](#410-intelligence-dashboard)
5. [Key Workflows — Step by Step](#5-key-workflows--step-by-step)
6. [Understanding the Endorsement Lifecycle](#6-understanding-the-endorsement-lifecycle)
7. [Role-Based Guide — What Matters to You](#7-role-based-guide--what-matters-to-you)
8. [Glossary](#8-glossary)

---

## 1. What is the Plum Endorsement Service?

The Plum Endorsement Service manages **insurance policy changes** (endorsements) in employer-sponsored group health insurance. When an employer needs to add a new employee to their health insurance, remove a departing employee, or update an existing member's details, the change flows through this system.

### The Core Problem It Solves

```
Without Plum                              With Plum
─────────────────                         ──────────────────
Employee joins company                    Employee joins company
  ↓                                         ↓
HR fills insurer portal (manual)          HR creates endorsement (1 click)
  ↓                                         ↓
Wait for insurer confirmation             Employee covered IMMEDIATELY
  ↓  (days to weeks)                        ↓  (provisional coverage)
Employee has NO coverage                  Insurer confirms in background
  ↓                                         ↓
Coverage starts (finally)                 Coverage upgraded to CONFIRMED
```

### What It Does — In One Sentence Per Feature

| Feature | What It Does |
|---------|-------------|
| **Endorsement Management** | Create, track, and process insurance policy changes through an 11-state lifecycle |
| **Provisional Coverage** | Grants employees immediate coverage at endorsement creation — zero gap |
| **Multi-Insurer Routing** | Automatically routes endorsements to the correct insurer (ICICI Lombard, Niva Bupa, Bajaj Allianz) using each insurer's preferred format (JSON, CSV, XML) |
| **Batch Optimization** | Groups endorsements into optimized batches every 15 minutes, prioritizing deletions to free balance for additions |
| **EA Account Tracking** | Monitors Employer Advance account balances, reserves, and available funds in real time |
| **Anomaly Detection** | Flags suspicious patterns — volume spikes, add/delete cycling, unusual premiums, dormancy breaks |
| **Balance Forecasting** | Projects EA balance 30 days ahead and alerts when top-ups are needed |
| **Error Resolution** | Automatically suggests (and often auto-applies) fixes for common submission errors |
| **Process Mining** | Measures straight-through processing (STP) rates and identifies bottlenecks per insurer |
| **Reconciliation** | Matches submitted endorsements against insurer records and flags discrepancies |

---

## 2. Quick Start — First 5 Minutes

### Starting the Application

**Option A — One-Click Installer (recommended)**:
```bash
./start.sh              # Rule-based intelligence (default)
./start.sh --ollama     # With Ollama/GenAI intelligence (downloads LLM automatically)
```
This single command does everything:
1. Checks all prerequisites (Java 21+, Node 18+, npm, Docker, Docker Compose)
2. Starts all infrastructure containers (PostgreSQL, Redis, Kafka, Jaeger, ELK, Prometheus, Grafana)
3. Waits for container health checks to pass
4. **With `--ollama`**: Starts an Ollama Docker container, waits for it to be ready, and pulls the `llama3.2` model (first run downloads ~2 GB)
5. Builds the Spring Boot backend
6. Installs frontend dependencies
7. Starts the backend (port 8080) and frontend (port 5173) — with `--ollama`, activates the `ollama` Spring profile so `OllamaAugmentedAnomalyDetector` and `OllamaErrorResolver` replace their rule-based counterparts
8. Seeds demo EA accounts for all 4 insurers
9. Verifies intelligence schedulers are registered
10. Prints all service URLs and demo data (Employer ID, Insurer IDs)

To stop everything (including Ollama): `./start.sh stop`

> **Tip**: You can override the model via environment variable: `OLLAMA_MODEL=mistral ./start.sh --ollama`

**Option B — Docker + Manual Frontend**:
```bash
docker-compose up -d
```
This starts PostgreSQL, Redis, Kafka, and the backend. Then manually start the frontend:
```bash
cd frontend && npm run dev
```
> Note: This does not seed demo data or verify health checks. Use Option A for the full experience.

**Option C — Kubernetes**:
```bash
./k8s-start.sh
```

### Your First Endorsement in 90 Seconds

1. Open `http://localhost:5173` in your browser
2. You land on the **Dashboard** — note the KPI cards (Total, Pending, Confirmed, Failed)
3. Click **"Create Endorsement"** (blue button, top-right of the endorsements page, or navigate via Sidebar > Endorsements > click "+ Create Endorsement")
4. The form pre-fills Employer ID and Insurer ID. Fill in:
   - **Employee ID**: paste any UUID (e.g., `aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee`)
   - **Employee Name**: `Jane Smith`
   - **Coverage Start Date**: tomorrow's date
   - **Premium Amount**: `1500`
5. Click **"Create Endorsement"**
6. You're redirected to the **Endorsement Detail** page — note:
   - The **Status Timeline** shows the endorsement progressed through Created → Validated → Provisionally Covered
   - The **Coverage Card** shows "PROVISIONAL" — the employee is already covered
   - The **"Submit to Insurer"** button is available as the next action
7. Click **"Submit to Insurer"** → Confirm in the dialog
8. The mock insurer auto-confirms — refresh to see the status update to **Confirmed**

You've just completed the entire endorsement lifecycle.

### Running Tests

The project includes **800+ tests** across 5 suites. Each suite has a one-click runner script that executes the tests and launches an Allure report in your browser.

#### Run All Tests (Combined Allure Report)

```bash
./run-all-tests.sh                # Run API + BDD + E2E + Perf tests → combined Allure at :5050
./run-all-tests.sh --skip-perf    # Skip performance tests (faster)
./run-all-tests.sh --report       # Regenerate combined report from last run (skip tests)
./run-all-tests.sh --stop         # Stop the Allure server container
```

After completion, open `http://localhost:5050/allure-docker-service/latest-report` to see the combined report with all suites segregated into sections.

#### Run Individual Test Suites

| Script | Suite | Tests | Allure Port | Prerequisites |
|--------|-------|-------|-------------|---------------|
| `./gradlew test` | Unit (JUnit 5 + Mockito + AssertJ) | 420 | — | None (uses H2 in-memory) |
| `./run-api-tests.sh` | API Integration (REST Assured + Testcontainers) | 124 | :5050 | Docker running |
| `./run-behaviour-tests.sh` | BDD (Cucumber + Gherkin, 16 feature files) | 92 | :5051 | Docker running |
| `./run-e2e-tests.sh` | E2E (Playwright + Storybook) | 158 | :5052 | Backend on :8080, Frontend on :5173 |
| `./run-perf-tests.sh` | Performance (Gatling) | 6 | — | Backend on :8080 |

#### Individual Script Options

**API Tests**:
```bash
./run-api-tests.sh               # Run tests + launch Allure report
./run-api-tests.sh --report      # Regenerate report (skip tests)
./run-api-tests.sh --stop        # Stop Allure container
```

**BDD Tests**:
```bash
./run-behaviour-tests.sh         # Run tests + launch Allure report
./run-behaviour-tests.sh --report
./run-behaviour-tests.sh --stop
```

**E2E Tests**:
```bash
./run-e2e-tests.sh               # Run all E2E + Storybook tests
./run-e2e-tests.sh --e2e         # Run only E2E flow tests
./run-e2e-tests.sh --storybook   # Run only Storybook component tests
./run-e2e-tests.sh --report      # Regenerate report (skip tests)
./run-e2e-tests.sh --stop        # Stop Allure container
```

**Performance Tests**:
```bash
./run-perf-tests.sh                           # Run BaselineSimulation (default)
./run-perf-tests.sh LoadSimulation            # Run a specific simulation
./run-perf-tests.sh StressSimulation --rps 50 # Run with custom target RPS
./run-perf-tests.sh --list                    # List available simulations
```

> **Tip**: For the fastest feedback loop during development, run `./gradlew test` (unit tests only — no Docker required, completes in seconds). Use the full suite scripts before committing or for CI validation.

---

## 3. Navigation & Layout

### Application Shell

The application uses a standard sidebar + content layout:

```
┌──────────────────────────────────────────────────────────┐
│  TopBar:  [Breadcrumbs]                   [Live] [Bell]  │
├────────────┬─────────────────────────────────────────────┤
│            │                                             │
│  Sidebar   │              Main Content                   │
│            │                                             │
│  Dashboard │      (changes based on current page)        │
│  Endorse.  │                                             │
│  Batches   │                                             │
│  EA Accts  │                                             │
│  Insurers  │                                             │
│  Recon.    │                                             │
│  Intelli.  │                                             │
│            │                                             │
└────────────┴─────────────────────────────────────────────┘
```

### Sidebar Navigation

| Menu Item | Path | Purpose |
|-----------|------|---------|
| Dashboard | `/` | Operations overview with KPI cards |
| Endorsements | `/endorsements` | List, search, filter, bulk-act on endorsements |
| Batches | `/endorsements/batches` | Track batch submission progress |
| EA Accounts | `/ea-accounts` | Look up employer account balances |
| Insurers | `/insurers` | View insurer configurations and capabilities |
| Reconciliation | `/reconciliation` | Monitor insurer reconciliation runs |
| Intelligence | `/intelligence` | AI-powered anomaly detection, forecasting, error resolution, process mining |

### TopBar Features

- **Breadcrumbs** (desktop): Shows your location — e.g., `Endorsements > ENS-abc123`
- **Back button** (mobile): Navigates to the parent page
- **Live indicator** (green dot): Shows WebSocket connection is active for real-time updates
- **Notification bell**: Shows unread notifications with count badge — click to see recent events grouped by Today / Yesterday / Older

### Mobile Experience

On mobile devices (< 768px):
- The sidebar collapses into a hamburger menu (top-left)
- Breadcrumbs are replaced by a back button
- Tables become scrollable horizontally
- Action buttons stack vertically

---

## 4. Screen-by-Screen Guide

### 4.1 Dashboard

**Path**: `/` | **Who uses it**: Everyone

The Dashboard is your command center. It answers the question: *"How are endorsement operations going right now?"*

#### What You See

**KPI Cards (top row)** — Four clickable cards showing key counts:

| Card | Color | Shows | Click Action |
|------|-------|-------|--------------|
| Total | Blue | All endorsements for the employer | Opens endorsements list (no status filter) |
| Pending | Yellow | Endorsements still being processed | Opens list filtered to active statuses |
| Confirmed | Green | Successfully confirmed by insurer | Opens list filtered to CONFIRMED |
| Failed | Red | Rejected or permanently failed | Opens list filtered to REJECTED + FAILED_PERMANENT |

**Summary Cards (second row)**:
- **Active Batches** — Number of batches in progress. Click to go to Batch Progress page.
- **Outstanding Items** — Endorsements needing attention.

**Status Distribution** — A horizontal stacked bar showing the breakdown of endorsements across all 11 statuses. Gives you an instant visual of pipeline health.

**Recent Endorsements Table** — The 5 most recently created endorsements. Each row shows:
- Employee ID (shortened UUID)
- Type badge (ADD / DELETE / UPDATE — color-coded)
- Status badge (color-coded to lifecycle stage)
- Created time (relative — "2 minutes ago")
- Click the row's link icon to view the full detail

**EA Account Card** (right side) — Shows the employer's financial position:
- **Balance**: Total amount in the Employer Advance account
- **Reserved**: Amount reserved for in-flight endorsements
- **Available**: Balance minus reserved — what's available for new endorsements
- Visual progress bar showing available as a percentage of total
- Last updated timestamp

#### How to Use It

1. **Change context**: Edit the Employer ID and Insurer ID fields at the top to view data for a different employer-insurer pair
2. **Drill into problems**: Click the red "Failed" card to immediately see which endorsements need attention
3. **Refresh**: Click the refresh icon next to the "Dashboard updated" timestamp to pull the latest data
4. **Navigate**: Click any KPI card, the "View all" link, or the "Active Batches" card to go deeper

---

### 4.2 Endorsements List

**Path**: `/endorsements` | **Who uses it**: HR Admins, Operations

This is the primary working screen for managing endorsements day-to-day.

#### What You See

**Filters Bar**:
- **Employer ID** — Text input (auto-applied with 400ms debounce). Changing this resets pagination.
- **Status Filter** — Click the filter icon to open a multi-select popover with all 11 statuses. Check/uncheck statuses to filter. A badge shows the number of active filters.

**Action Buttons**:
- **Export CSV** — Downloads the current page's data as a CSV file (Employee ID, Type, Status, Premium, Coverage Start, Insurer Ref, Created)
- **Create Endorsement** — Navigates to the creation form

**Endorsement Table** — Sortable columns (click any header to sort asc/desc):

| Column | Description | Sortable |
|--------|-------------|----------|
| Checkbox | Select row for bulk actions (only for submittable statuses) | No |
| Employee | Shortened employee UUID | Yes |
| Type | ADD (blue) / DELETE (red) / UPDATE (amber) badge | Yes |
| Status | Color-coded status badge (11 possible states) | Yes |
| Premium | Amount in INR (e.g., ₹1,500.00) | Yes |
| Coverage Start | Formatted date | Yes |
| Insurer Ref | Insurer's reference number (shown after confirmation) | Yes |
| Created | Relative time ("3 min ago") | Yes |
| Actions | Link icon — opens endorsement detail page | No |

**Pagination** (below table):
- "Showing X-Y of Z" counter
- Page selector buttons (Previous / 1 / 2 / ... / Next)
- Page size dropdown (10, 25, 50, 100 items per page)

**Last Updated** — Timestamp showing when data was last fetched, with a manual refresh button.

**Bulk Action Bar** (fixed at bottom, appears when rows are selected):
- Shows "X selected" count
- **Submit Selected** button — submits all selected endorsements to the insurer in sequence
- **Clear selection** link — deselects all

#### Key Interactions

- **Filter by status**: Click the filter icon → check "CREATED" and "VALIDATED" → see only new endorsements needing review
- **Sort by premium**: Click "Premium" column header once for ascending, again for descending — quickly find the largest endorsements
- **Bulk submit**: Check multiple endorsements → click "Submit Selected" in the bottom bar → all are submitted to their respective insurers
- **URL persistence**: Filters, page number, and page size are stored in the URL. You can bookmark a filtered view or share the URL with a colleague.

---

### 4.3 Create Endorsement

**Path**: `/endorsements/new` | **Who uses it**: HR Admins

Create a new endorsement to add, remove, or update an employee's insurance coverage.

#### Form Sections

**Section 1: Identifiers**
| Field | Required | Format | Pre-filled |
|-------|----------|--------|------------|
| Employer ID | Yes | UUID | Yes (default employer) |
| Employee ID | Yes | UUID | No |
| Insurer ID | Yes | UUID | Yes (default insurer) |
| Policy ID | Yes | UUID | No |

**Section 2: Endorsement Details**
| Field | Required | Notes |
|-------|----------|-------|
| Type | Yes | ADD, DELETE, or UPDATE — selecting DELETE hides premium and employee data fields |
| Premium Amount | Yes (for ADD/UPDATE) | Number in INR — hidden for DELETE type |
| Coverage Start Date | Yes | Date picker |
| Coverage End Date | No | Optional — hidden for DELETE type |

**Section 3: Employee Data** (hidden for DELETE type)
| Field | Required | Notes |
|-------|----------|-------|
| Name | Yes (for ADD/UPDATE) | Employee's full name |
| Date of Birth | No | Date picker |
| Gender | No | M / F / Other |

**Section 4: Advanced**
| Field | Required | Notes |
|-------|----------|-------|
| Idempotency Key | No | Auto-generated if left blank. Prevents duplicate submissions. |

#### Smart Form Behavior

- **Progressive disclosure**: Selecting "DELETE" as the type automatically hides the Premium Amount, Coverage End Date, and Employee Data sections — only the minimum required fields are shown
- **Low balance alert**: If the EA account's available balance drops below ₹10,000, a warning banner appears at the top of the form: *"Low EA balance. Available: ₹X. Endorsement may be queued."*
- **Auto-scroll to errors**: If validation fails, the form automatically scrolls to and focuses the first field with an error
- **Validation**: UUID format enforcement, required field checks, positive premium amount

#### After Submission

On successful creation, you are automatically redirected to the **Endorsement Detail** page. The endorsement has already progressed through:
1. **CREATED** — Endorsement record saved
2. **VALIDATED** — Business rules passed (balance check, duplicate check)
3. **PROVISIONALLY_COVERED** — Employee is immediately covered (for ADD type)

A success toast notification appears confirming the creation.

---

### 4.4 Endorsement Detail

**Path**: `/endorsements/:id` | **Who uses it**: HR Admins, Operations, Finance

The single most information-rich screen. Shows everything about one endorsement.

#### Status Timeline (top)

A horizontal visual timeline showing the endorsement's journey through the state machine. Two possible paths are shown:

**Real-time path** (synchronous insurer submission):
```
Created → Validated → Prov. Covered → Submitted (RT) → Insurer Processing → Confirmed
```

**Batch path** (asynchronous batch submission):
```
Created → Validated → Prov. Covered → Queued → Batch Submitted → Insurer Processing → Confirmed
```

Each step is shown as a circle:
- **Green** (filled) — Step completed
- **Blue** (pulsing) — Current step (where the endorsement is now)
- **Gray** — Future step (not yet reached)

If the endorsement was **rejected** or **failed**, a red alert box appears below the timeline showing:
- The rejection reason
- The retry count (e.g., "Attempt 2 of 3")
- Whether retry is available

#### Details Card (left column, 2/3 width)

| Field | Description |
|-------|-------------|
| Endorsement ID | Full UUID with copy-to-clipboard button |
| Type | ADD / DELETE / UPDATE |
| Status | Current lifecycle status |
| Employer ID | UUID of the employer |
| Employee ID | UUID of the employee |
| Insurer ID | UUID of the insurer |
| Policy ID | UUID of the policy |
| Coverage Period | Start date and end date |
| Premium Amount | Amount in INR |
| Insurer Reference | Assigned by insurer after confirmation (blank until confirmed) |
| Batch ID | If part of a batch — the batch UUID |
| Retry Count | 0, 1, 2, or 3 — number of retry attempts |
| Idempotency Key | Unique key for duplicate prevention |
| Created | Timestamp of creation |
| Updated | Timestamp of last state change |

#### Action Buttons (right column, 1/3 width)

The available actions depend on the current status:

| Current Status | Available Actions |
|---------------|-------------------|
| PROVISIONALLY_COVERED | **Submit to Insurer** — Opens confirmation dialog |
| RETRY_PENDING | **Submit to Insurer** — Retry submission |
| SUBMITTED_REALTIME, BATCH_SUBMITTED, INSURER_PROCESSING | **Confirm** — Opens dialog for insurer reference number |
| SUBMITTED_REALTIME, BATCH_SUBMITTED, INSURER_PROCESSING | **Reject** — Opens dialog for rejection reason |
| REJECTED (retryCount < 3) | **Retry Submission** — Re-submit to insurer |
| CONFIRMED | "This endorsement has been confirmed" (no actions) |
| FAILED_PERMANENT | "This endorsement has permanently failed" (no actions) |

#### Coverage Card (right column, below actions)

For ADD endorsements, a coverage card shows:
- **Type**: PROVISIONAL or CONFIRMED
- **Coverage Start**: The date coverage begins
- **Confirmed At**: Timestamp when insurer confirmed (blank if still provisional)
- **Active**: Yes/No — whether coverage is currently active

---

### 4.5 Batch Progress

**Path**: `/endorsements/batches` | **Who uses it**: Operations, Finance

Track how endorsement batches are being processed by insurers.

#### What You See

**Employer ID filter** at the top — enter a UUID to see batches for a specific employer.

**Batch Table**:

| Column | Description |
|--------|-------------|
| Batch ID | Shortened UUID of the batch |
| Insurer | Shortened UUID of the target insurer |
| Status | SUBMITTED (blue) / COMPLETED (green) / FAILED (red) badge |
| Endorsements | Number of endorsements in the batch |
| Submitted | When the batch was sent to the insurer (relative time) |
| Completed | When the insurer finished processing (formatted date or "--") |
| Insurer Ref | Reference number assigned by insurer (or "--") |

**Pagination** below the table for navigating through multiple pages of batches.

#### How Batches Work

The system automatically assembles batches every 15 minutes:
1. Endorsements in `QUEUED_FOR_BATCH` status are collected
2. The batch optimizer groups them by insurer and prioritizes: deletions first (to free balance), then additions, then updates
3. Each batch is submitted to the insurer via the appropriate protocol (REST/CSV/SOAP)
4. The batch status updates as the insurer processes it

---

### 4.6 EA Accounts

**Path**: `/ea-accounts` | **Who uses it**: Finance, HR Admins

Look up an employer's Employer Advance (EA) account — the pre-funded account used to pay endorsement premiums.

#### How to Use It

1. Enter the **Employer ID** (UUID)
2. Enter the **Insurer ID** (UUID)
3. Click **"Look Up"**

#### What You See (after lookup)

**Three KPI Cards**:
- **Total Balance** — The full amount in the EA account (in INR)
- **Reserved** (amber border) — Amount reserved for endorsements currently being processed
- **Available** (green border) — Total minus Reserved — what's available for new endorsements

**Balance Breakdown Card**:
- Visual progress bar showing available balance as a percentage of total
- Exact percentage displayed
- Last updated timestamp

#### Why This Matters

When an endorsement is created, the system **reserves** the premium amount from the EA balance. If the endorsement is confirmed, the reserved amount is debited. If rejected, it's released back. If the available balance is too low, new endorsements may be queued rather than submitted immediately.

---

### 4.7 Insurers

**Path**: `/insurers` | **Who uses it**: Operations, Engineers

View all configured insurer integrations and their capabilities.

#### Insurer Table

| Column | Description |
|--------|-------------|
| Name | Display name (e.g., "ICICI Lombard", "Niva Bupa") |
| Code | Short code (e.g., `ICICI_LOMBARD`) — monospace font |
| Adapter Type | Technical integration type (MOCK, ICICI_LOMBARD, NIVA_BUPA, BAJAJ_ALLIANZ) |
| Data Format | JSON, CSV, or XML |
| Real-time? | Green "Yes" or gray "No" badge — whether synchronous submission is supported |
| Batch? | Green "Yes" or gray "No" badge — whether batch submission is supported |
| Rate Limit | Maximum API requests per minute |
| Status | Active (green) or Inactive (gray) badge |
| Actions | Link icon — opens insurer detail page |

#### Currently Configured Insurers

| Insurer | Format | Real-time | Batch | Notes |
|---------|--------|-----------|-------|-------|
| Mock Insurer | JSON | Yes | Yes | Development/testing adapter |
| ICICI Lombard | JSON | Yes | No | REST API, 150ms latency |
| Niva Bupa | CSV | No | Yes | SFTP-based batch only |
| Bajaj Allianz | XML | Yes | Yes | SOAP/XML, 250ms latency |

---

### 4.8 Insurer Detail

**Path**: `/insurers/:insurerId` | **Who uses it**: Operations, Engineers

Deep dive into a single insurer's configuration, capabilities, and reconciliation history.

#### Configuration Card (left column)

Shows all configuration details: ID, Name, Code, Adapter Type, Data Format, Rate Limit, Active/Inactive status, Created and Updated timestamps.

#### Capabilities Card (right column)

- **Real-time**: Supported / Not Supported badge
- **Batch**: Supported / Not Supported badge
- **Max Batch Size**: Maximum endorsements per batch (e.g., 500)
- **Batch SLA**: Expected processing time in hours
- **Rate Limit**: Max requests per minute

#### Reconciliation Runs Card (bottom)

A **"Trigger Reconciliation"** button at the top lets you manually initiate a reconciliation run against this insurer.

**Runs Table** (expandable rows):

| Column | Description |
|--------|-------------|
| Run ID | Shortened UUID |
| Status | RUNNING / COMPLETED badge |
| Checked | Total endorsements checked |
| Matched | Endorsements confirmed by insurer |
| Discrepancies | Partial matches + rejected + missing |
| Started | When the run began |

Click any row to **expand** and see the individual reconciliation items — each item shows the endorsement, outcome (MATCH/PARTIAL_MATCH/REJECTED/MISSING), and any action taken.

---

### 4.9 Reconciliation

**Path**: `/reconciliation` | **Who uses it**: Finance, Operations

Monitor reconciliation across all insurers — ensures submitted endorsements match insurer records.

#### How to Use It

1. Select an insurer from the **dropdown** at the top
2. View the summary cards and reconciliation runs

#### What You See

**Four Summary Cards** (after selecting an insurer):
- **Matched** (green) — Endorsements confirmed by insurer
- **Partial Match** (yellow) — Endorsements with minor discrepancies
- **Rejected** (red) — Endorsements rejected by insurer
- **Missing** (purple) — Endorsements not found in insurer records

**Reconciliation Runs Table** — Same expandable format as the Insurer Detail page. Click to expand and see individual items.

**Actions**:
- **Export CSV** — Download reconciliation runs data
- **Trigger Reconciliation** — Start a new reconciliation run for the selected insurer

#### When Reconciliation Runs

- **Automatically**: Every 15 minutes, the system reconciles all active insurers
- **Manually**: Click the "Trigger Reconciliation" button on this page or the Insurer Detail page

---

### 4.10 Intelligence Dashboard

**Path**: `/intelligence` | **Who uses it**: Operations, Finance, Risk, C-Level

The AI-powered analytics hub with four tabs covering anomaly detection, forecasting, error resolution, and process mining.

#### Tab 1: Anomalies

Flags suspicious endorsement patterns for human review.

**Anomaly Table**:

| Column | Description |
|--------|-------------|
| Employer | Employer UUID (shortened) |
| Type | VOLUME_SPIKE / ADD_DELETE_CYCLING / SUSPICIOUS_TIMING / UNUSUAL_PREMIUM / DORMANCY_BREAK badge |
| Score | Confidence score (0-100%). Red badge ≥ 90%, amber ≥ 70%, gray < 70% |
| Explanation | Human-readable description of why this was flagged |
| Status | FLAGGED / UNDER_REVIEW / DISMISSED / CONFIRMED_FRAUD |
| Flagged At | When the anomaly was detected |
| Actions | **Review** and **Dismiss** buttons (for FLAGGED anomalies) |

**What Each Anomaly Type Means**:

| Type | Trigger | Example |
|------|---------|---------|
| VOLUME_SPIKE | 25+ endorsements for one employer in 1 hour | Bulk onboarding or potential data issue |
| ADD_DELETE_CYCLING | Same employee added, deleted, then re-added | Potential premium arbitrage |
| SUSPICIOUS_TIMING | Employee added very close to a claim window | Adverse selection risk |
| UNUSUAL_PREMIUM | Premium amount is a statistical outlier vs. history | Data entry error or intentional inflation |
| DORMANCY_BREAK | Employee with no endorsement activity for 90+ days suddenly reappears | Potential policy manipulation or data irregularity |

**Actions**:
- **Review**: Opens dialog to change status to UNDER_REVIEW with notes
- **Dismiss**: Opens dialog to mark as false positive with notes (e.g., "Planned bulk onboarding")

#### Tab 2: Forecasts

Projects EA balance 30 days ahead. Warns when a top-up is needed.

**Inputs**: Employer ID and Insurer ID (both required).

**Forecast Cards** (after lookup):
- **Forecasted Amount** — Projected balance in 30 days (INR)
- **Actual Amount** — Real balance at forecast date (if available)
- **Accuracy** — How close the forecast was (% — only after actual is recorded)
- **Forecast Date** — The date of the projection

**Narrative** — A plain-English explanation of the forecast methodology and key assumptions (e.g., "Based on 45-day burn rate of ₹12,000/day with seasonal adjustment factor of 1.15").

**Generate Forecast** button — Triggers a new forecast calculation.

**Forecast History Table** — Shows all historical forecasts with date, projected amount, actual amount, accuracy, and narrative. Use this to evaluate forecast quality over time.

#### Tab 3: Error Resolution

The system automatically analyzes endorsement submission errors, suggests corrections, and can auto-apply high-confidence fixes.

**Stats Cards**:
- **Total Resolutions** — Total errors analyzed
- **Auto-Applied** — Fixes applied automatically (confidence ≥ 95%)
- **Suggested** — Fixes suggested but awaiting human approval
- **Auto-Apply Rate** — Percentage of fixes applied without human intervention
- **Success Count** — Resolutions where the endorsement subsequently reached CONFIRMED
- **Failure Count** — Resolutions where the endorsement was ultimately REJECTED or FAILED_PERMANENT
- **Success Rate** — Percentage of tracked resolutions that led to successful endorsement confirmation

**Recent Resolutions Table**:

| Column | Description |
|--------|-------------|
| Error Type | DATE_FORMAT / MISSING_FIELD / MEMBER_ID / PREMIUM_MISMATCH / UNKNOWN |
| Original | The incorrect value |
| Corrected | The suggested/applied correction |
| Confidence | How confident the system is in the fix (0-100%) |
| Auto-Applied | Yes/No badge |
| Created | When the resolution was generated |
| Actions | **Approve** button (for suggested, non-auto-applied fixes) |

**Error Types Explained**:

| Error Type | What Happened | How It's Fixed |
|------------|--------------|----------------|
| DATE_FORMAT | Date in wrong format (e.g., `07/03/1990` instead of ISO) | Converts to `1990-03-07` |
| MISSING_FIELD | Required field is blank (e.g., employee name) | Fills from previous records |
| MEMBER_ID | Employee ID format incorrect | Reformats to match insurer's expected pattern |
| PREMIUM_MISMATCH | Premium doesn't match policy rate | Recalculates from base rate |
| UNKNOWN | Unrecognized error | Logged for manual review |

#### Tab 4: Process Mining

Analyzes endorsement processing efficiency across insurers.

**Overall STP Rate Card** — The percentage of endorsements that go straight through without human intervention (Straight-Through Processing rate). Target: > 80%.

**Per-Insurer STP Rate Cards** — Individual STP rate for each insurer. Quickly identify which insurer integrations need optimization.

**STP Rate Trend** — Historical STP rate data captured daily by the `ProcessMiningScheduler`. Query via `GET /api/v1/intelligence/process-mining/stp-rate/trend?insurerId={id}&days=30` to see how STP rates have changed over time for a given insurer.

**Bottleneck Insights**:
- Lists identified bottlenecks in the processing pipeline
- Each insight shows the type (BOTTLENECK), insurer name, and a description
- **"Run Analysis"** button triggers a fresh analysis
- A bottleneck is flagged when P95 duration exceeds 2x the average OR average exceeds 4 hours

**Transition Metrics Table** — Shows how long endorsements spend in each state transition:

| Column | Description |
|--------|-------------|
| From Status | Starting state |
| To Status | Ending state |
| Avg Duration | Average time for this transition |
| P95 | 95th percentile duration |
| P99 | 99th percentile duration |
| Samples | Number of endorsements measured |
| Relative | Visual bar chart showing duration relative to the longest transition |

The row with the longest average duration is highlighted — this is your primary optimization target.

---

## 5. Key Workflows — Step by Step

### Workflow 1: Add a New Employee to Insurance

**Role**: HR Administrator

| Step | Screen | Action |
|------|--------|--------|
| 1 | Sidebar | Click **Endorsements** |
| 2 | Endorsements List | Click **"Create Endorsement"** (top-right) |
| 3 | Create Endorsement | Fill in Employee ID, Name, Coverage Start Date, Premium Amount. Type defaults to "ADD". |
| 4 | Create Endorsement | Click **"Create Endorsement"** |
| 5 | Endorsement Detail | Verify status shows **PROVISIONALLY_COVERED**. The employee is now covered. |
| 6 | Endorsement Detail | Click **"Submit to Insurer"** → Confirm in dialog |
| 7 | Endorsement Detail | Status updates to SUBMITTED_REALTIME or QUEUED_FOR_BATCH |
| 8 | (automatic) | Insurer confirms → Status becomes **CONFIRMED** |
| 9 | Endorsement Detail | Coverage card updates from PROVISIONAL to **CONFIRMED** |

### Workflow 2: Remove a Departing Employee

**Role**: HR Administrator

| Step | Screen | Action |
|------|--------|--------|
| 1 | Create Endorsement | Select Type = **DELETE** |
| 2 | Create Endorsement | Only Identifiers and Coverage Start Date are shown (no premium, no employee data) |
| 3 | Create Endorsement | Fill in Employee ID and Coverage Start Date, click **"Create Endorsement"** |
| 4 | Endorsement Detail | No coverage card is shown (DELETE type doesn't grant coverage) |
| 5 | Endorsement Detail | Submit to insurer as normal |

### Workflow 3: Handle a Rejected Endorsement

**Role**: Operations

| Step | Screen | Action |
|------|--------|--------|
| 1 | Dashboard | Notice the "Failed" KPI card count has increased. Click it. |
| 2 | Endorsements List | See endorsements filtered to REJECTED / FAILED_PERMANENT statuses |
| 3 | Endorsements List | Click the link icon on a REJECTED endorsement |
| 4 | Endorsement Detail | Timeline shows a red alert with rejection reason and retry count |
| 5 | Endorsement Detail | If retry count < 3, click **"Retry Submission"** |
| 6 | (automatic) | System re-submits to insurer. If confirmed → status becomes CONFIRMED. If rejected again → retry count increments. |
| 7 | Endorsement Detail | After 3 failed retries → status becomes FAILED_PERMANENT |

### Workflow 4: Investigate an Anomaly

**Role**: Risk / Intelligence Analyst

| Step | Screen | Action |
|------|--------|--------|
| 1 | Sidebar | Click **Intelligence** |
| 2 | Intelligence Dashboard | Anomalies tab shows flagged items |
| 3 | Intelligence Dashboard | See a VOLUME_SPIKE anomaly with score 92% for employer XYZ |
| 4 | Intelligence Dashboard | Click **"Review"** → Enter notes: "Checking with HR team" |
| 5 | Intelligence Dashboard | Status changes to UNDER_REVIEW |
| 6 | (investigate offline) | Confirm with HR that this was a planned bulk onboarding |
| 7 | Intelligence Dashboard | Click **"Dismiss"** → Enter notes: "Planned Q2 onboarding batch" |
| 8 | Intelligence Dashboard | Anomaly status changes to DISMISSED |

### Workflow 5: Check if EA Balance Needs Top-Up

**Role**: Finance

| Step | Screen | Action |
|------|--------|--------|
| 1 | Sidebar | Click **EA Accounts** |
| 2 | EA Accounts | Enter Employer ID and Insurer ID → Click **"Look Up"** |
| 3 | EA Accounts | Review: Total ₹500,000 / Reserved ₹180,000 / Available ₹320,000 |
| 4 | Sidebar | Click **Intelligence** → Go to **Forecasts** tab |
| 5 | Intelligence Dashboard | Enter same Employer ID and Insurer ID → Click **"Generate Forecast"** |
| 6 | Intelligence Dashboard | See: Forecasted Amount ₹150,000 in 30 days. Narrative: "At current burn rate, balance will be insufficient in 18 days." |
| 7 | (action) | Initiate EA top-up with Finance team |

### Workflow 6: Bulk Submit Endorsements

**Role**: Operations

| Step | Screen | Action |
|------|--------|--------|
| 1 | Endorsements List | Filter by status: check only "CREATED" and "PROVISIONALLY_COVERED" |
| 2 | Endorsements List | Check the "Select all" checkbox in the header |
| 3 | Endorsements List | The bulk action bar appears at the bottom: "X selected" |
| 4 | Endorsements List | Click **"Submit Selected"** |
| 5 | (automatic) | Each selected endorsement is submitted to its respective insurer |
| 6 | Endorsements List | Toast notifications confirm each submission |

### Workflow 7: Run and Review Reconciliation

**Role**: Finance / Operations

| Step | Screen | Action |
|------|--------|--------|
| 1 | Sidebar | Click **Reconciliation** |
| 2 | Reconciliation | Select an insurer from the dropdown |
| 3 | Reconciliation | Click **"Trigger Reconciliation"** |
| 4 | Reconciliation | Wait for the run to complete (status changes from RUNNING to COMPLETED) |
| 5 | Reconciliation | Review summary cards: Matched (green), Partial (yellow), Rejected (red), Missing (purple) |
| 6 | Reconciliation | Click a run row to expand and see individual items |
| 7 | Reconciliation | For discrepancies: note the endorsement IDs and investigate |
| 8 | Reconciliation | Click **"Export CSV"** to download for offline analysis |

---

## 6. Understanding the Endorsement Lifecycle

### The 11-State Machine

Every endorsement moves through a defined set of states. Understanding this flow is essential for using the product effectively.

```
                              ┌──────────────────────────────────────────────┐
                              │         ENDORSEMENT LIFECYCLE                │
                              └──────────────────────────────────────────────┘

                                          ┌─────────┐
                                          │ CREATED  │
                                          └────┬─────┘
                                               │ auto-validated
                                          ┌────▼─────┐
                                          │VALIDATED  │
                                          └────┬─────┘
                                               │ coverage granted
                                     ┌─────────▼──────────┐
                                     │PROVISIONALLY_COVERED│
                                     └────┬──────────┬─────┘
                                          │          │
                               ┌──────────▼──┐  ┌───▼──────────┐
                               │ SUBMITTED   │  │ QUEUED_FOR   │
                               │ _REALTIME   │  │ _BATCH       │
                               └──────┬──────┘  └───┬──────────┘
                                      │              │
                                      │         ┌────▼─────────┐
                                      │         │BATCH_SUBMITTED│
                                      │         └────┬─────────┘
                                      │              │
                                 ┌────▼──────────────▼──┐
                                 │  INSURER_PROCESSING   │
                                 └────┬─────────────┬────┘
                                      │             │
                              ┌───────▼──┐   ┌──────▼───┐
                              │CONFIRMED │   │ REJECTED  │
                              │(terminal)│   └──────┬────┘
                              └──────────┘          │
                                                    │ retry?
                                            ┌───────▼───────┐
                                      Yes ← │ retryCount<3? │ → No
                                            └───┬───────┬───┘
                                                │       │
                                        ┌───────▼──┐  ┌─▼──────────────┐
                                        │  RETRY   │  │FAILED_PERMANENT│
                                        │ _PENDING │  │  (terminal)    │
                                        └──────────┘  └────────────────┘
```

### State-by-State Reference

| State | Duration | What's Happening | Coverage Status | User Action |
|-------|----------|-----------------|-----------------|-------------|
| CREATED | < 1 second | Endorsement record created | None | Wait (auto-transitions) |
| VALIDATED | < 1 second | Business rules checked (balance, duplicates) | None | Wait (auto-transitions) |
| PROVISIONALLY_COVERED | Until submitted | Employee covered, awaiting submission | **PROVISIONAL** | Submit to Insurer |
| SUBMITTED_REALTIME | Seconds | Sent to insurer via real-time API | PROVISIONAL | Wait / Confirm / Reject |
| QUEUED_FOR_BATCH | Up to 15 min | Waiting for next batch assembly cycle | PROVISIONAL | Wait |
| BATCH_SUBMITTED | Hours to days | Sent in batch, awaiting insurer response | PROVISIONAL | Wait |
| INSURER_PROCESSING | Hours to days | Insurer is processing | PROVISIONAL | Wait / Confirm / Reject |
| CONFIRMED | Terminal | Insurer confirmed the endorsement | **CONFIRMED** | None |
| REJECTED | Until retry/fail | Insurer rejected — can be retried | PROVISIONAL | Retry / Review reason |
| RETRY_PENDING | Until resubmitted | Awaiting re-submission | PROVISIONAL | Submit to Insurer |
| FAILED_PERMANENT | Terminal | All retries exhausted | Expired (30-day TTL) | Manual investigation |

### The Coverage Guarantee

The system guarantees **zero coverage gap**:

1. When an ADD endorsement is created, **provisional coverage is granted immediately** — the employee is covered from day one
2. Coverage remains active (PROVISIONAL) while the endorsement is being processed
3. When the insurer confirms, coverage is upgraded to **CONFIRMED**
4. If all retries are exhausted (FAILED_PERMANENT), coverage has a 30-day TTL before expiration, and notifications are sent to the employer

---

## 7. Role-Based Guide — What Matters to You

### For HR Administrators

**Your screens**: Dashboard, Endorsements List, Create Endorsement, Endorsement Detail

**Your daily tasks**:
1. **Morning**: Check Dashboard for failed endorsements (red KPI card). Investigate and retry.
2. **Throughout day**: Create endorsements as employees join, leave, or need updates.
3. **Tip**: Use bulk submit to process multiple endorsements at once.
4. **Tip**: Bookmark filtered views (filters are in the URL) — e.g., bookmark "pending endorsements for my employer".

### For Finance Team

**Your screens**: Dashboard (EA Account card), EA Accounts, Reconciliation, Intelligence (Forecasts tab)

**Your daily tasks**:
1. **Morning**: Check EA balance on Dashboard. If available balance is low, check forecasts.
2. **Weekly**: Run reconciliation for each insurer. Review discrepancies.
3. **Monthly**: Export reconciliation data (CSV) for month-end reporting.
4. **Tip**: Use the Forecasts tab to proactively top up EA accounts before they run dry.

### For Operations / Platform Engineers

**Your screens**: All screens, especially Intelligence, Insurers, Reconciliation

**Your daily tasks**:
1. **Monitor**: Check Dashboard KPIs for anomalies in volumes.
2. **Investigate**: Review Intelligence > Anomalies for flagged items.
3. **Optimize**: Check Intelligence > Process Mining for bottlenecks and STP rates.
4. **Debug**: Use Insurer Detail to check circuit breaker state and reconciliation history.
5. **Tip**: The Intelligence > Process Mining > Transition Metrics table shows you exactly where endorsements are getting stuck.

### For Product Managers

**Your screens**: Dashboard, Intelligence Dashboard

**Key metrics to track**:
- **STP Rate**: Target > 80%. Found in Intelligence > Process Mining tab.
- **Confirmation Rate**: Confirmed / Total on Dashboard.
- **Error Auto-Apply Rate**: Found in Intelligence > Error Resolution tab. Higher = less manual work.
- **Forecast Accuracy**: Found in Intelligence > Forecasts tab. Tracks prediction quality.
- **Anomaly Detection**: Volume of flagged vs. dismissed anomalies indicates system sensitivity.

### For Designers

**Design system components used**:
- **StatusBadge**: 11 color-coded status variants (green/blue/yellow/red/gray/amber/indigo/sky)
- **TypeBadge**: 3 endorsement type variants (blue ADD / red DELETE / amber UPDATE)
- **PageHeader**: Consistent title + description + action button pattern across all pages
- **Pagination**: Reusable component with page size selector and "Showing X of Y"
- **EmptyState**: Consistent empty state with icon, title, description, and optional CTA
- **StatusTimeline**: Horizontal step indicator with branching paths
- **InlineAlert**: Warning/info banners (used for low EA balance alerts)

**Design patterns**:
- Expandable table rows (Reconciliation, Insurer Detail)
- Dialog confirmations for destructive actions (Reject, Submit)
- Progressive disclosure in forms (DELETE hides irrelevant fields)
- URL-persisted filters (shareable views)
- Optimistic UI updates (Confirm/Reject reflect immediately)

### For C-Level Executives

**Your screen**: Dashboard + Intelligence Dashboard

**What to look at**:
1. **Dashboard** — Are endorsements flowing? Check Total (volume), Pending (backlog), Confirmed (success), Failed (problems).
2. **Intelligence > Process Mining** — What's our STP rate? Are endorsements being processed efficiently?
3. **Intelligence > Anomalies** — Are there fraud risks or data quality issues?
4. **Intelligence > Forecasts** — Are employer EA accounts healthy? Any upcoming shortfalls?
5. **Reconciliation** — Are insurer records matching ours? Any financial discrepancies?

**Key executive metrics** (all available in the UI):

| Metric | Where to Find It | What "Good" Looks Like |
|--------|-------------------|----------------------|
| STP Rate | Intelligence > Process Mining | > 80% |
| Confirmation Rate | Dashboard (Confirmed / Total) | > 85% |
| Error Auto-Apply Rate | Intelligence > Error Resolution | > 60% |
| Anomaly False Positive Rate | Intelligence > Anomalies (Dismissed / Total) | < 30% |
| Forecast Accuracy | Intelligence > Forecasts (History) | > 85% |
| Reconciliation Match Rate | Reconciliation (Matched / Checked) | > 95% |
| EA Balance Health | EA Accounts (Available / Total) | > 30% available |

---

## 8. Glossary

| Term | Definition |
|------|-----------|
| **Endorsement** | A change to an insurance policy — adding, removing, or updating an employee |
| **EA Account** | Employer Advance Account — a pre-funded account from which endorsement premiums are debited |
| **Provisional Coverage** | Immediate insurance coverage granted when an endorsement is created, before insurer confirmation |
| **STP Rate** | Straight-Through Processing Rate — percentage of endorsements processed without human intervention |
| **Batch** | A group of endorsements submitted together to an insurer (assembled every 15 minutes) |
| **Reconciliation** | The process of matching submitted endorsements against insurer records to find discrepancies |
| **Anomaly** | A suspicious endorsement pattern flagged by the AI detection system |
| **Circuit Breaker** | A resilience pattern that stops sending requests to a failing insurer, preventing cascade failures |
| **Idempotency Key** | A unique identifier that prevents the same endorsement from being created twice |
| **Premium** | The insurance cost amount for an endorsement (in INR) |
| **Insurer Reference** | A tracking number assigned by the insurer when they confirm an endorsement |
| **Retry** | Re-submitting a rejected endorsement to the insurer (up to 3 attempts) |
| **Terminal State** | CONFIRMED or FAILED_PERMANENT — no further transitions possible |
| **CQRS** | Command Query Responsibility Segregation — separates read and write operations |
| **Hexagonal Architecture** | Also called Ports & Adapters — the domain core has no infrastructure dependencies |
| **KPI** | Key Performance Indicator — the summary metric cards on the Dashboard |
| **Tombstone Coverage** | Coverage state after FAILED_PERMANENT — expires after 30 days (safety net) |

---

> **Need help?** This guide covers the full product UI. For API documentation, see the Swagger UI at `http://localhost:8080/swagger-ui.html`. For architecture details, see `docs/deliverables/High_Level_Architecture.md`. For the functional specification, see `docs/Functional_Specification.md`.
