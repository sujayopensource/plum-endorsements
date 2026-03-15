# AI Automation Vision

**Project:** Plum Endorsement Management System
**Date:** March 14, 2026 (Updated)
**Scope:** Future AI/ML capabilities requiring new infrastructure, training data, external services, or fundamental architecture extensions
**Prerequisite:** [GenAI Augmentation Strategy](../GenAI_Augmentation_Strategy.md) -- the Ollama/LLM bridge between rule-based and full ML

---

## Table of Contents

- [1. Executive Summary](#1-executive-summary)
- [2. Pillar 1: ML-Based Anomaly Detection](#2-pillar-1-ml-based-anomaly-detection)
  - [2.1 Isolation Forest Anomaly Detector](#21-isolation-forest-anomaly-detector)
  - [2.2 Autoencoder Second-Stage Classifier](#22-autoencoder-second-stage-classifier)
  - [2.3 Graph-Based Fraud Network Detection](#23-graph-based-fraud-network-detection)
- [3. Pillar 2: Advanced Time-Series Forecasting](#3-pillar-2-advanced-time-series-forecasting)
  - [3.1 Facebook Prophet Forecasting](#31-facebook-prophet-forecasting)
  - [3.2 LSTM Neural Network Forecasting](#32-lstm-neural-network-forecasting)
  - [3.3 Ensemble Forecasting](#33-ensemble-forecasting)
- [4. Pillar 3: LLM-Powered Error Resolution](#4-pillar-3-llm-powered-error-resolution)
  - [4.1 RAG Error Resolver (Retrieval-Augmented Generation)](#41-rag-error-resolver-retrieval-augmented-generation)
  - [4.2 Natural Language Anomaly Explanations via LLM](#42-natural-language-anomaly-explanations-via-llm)
- [5. Pillar 4: Advanced Process Mining](#5-pillar-4-advanced-process-mining)
  - [5.1 PM4Py Process Mining Engine](#51-pm4py-process-mining-engine)
  - [5.2 Real-Time Process Monitoring with Kafka Streams](#52-real-time-process-monitoring-with-kafka-streams)
- [6. Pillar 5: Advanced Batch Optimization](#6-pillar-5-advanced-batch-optimization)
  - [6.1 Google OR-Tools Linear Programming Optimizer](#61-google-or-tools-linear-programming-optimizer)
  - [6.2 Reinforcement Learning Optimizer](#62-reinforcement-learning-optimizer)
- [7. Cross-Cutting Vision Capabilities](#7-cross-cutting-vision-capabilities)
  - [7.1 Feature Store](#71-feature-store)
  - [7.2 MLOps Pipeline](#72-mlops-pipeline)
  - [7.3 A/B Testing Framework for Intelligence](#73-ab-testing-framework-for-intelligence)
  - [7.4 Natural Language Query Interface](#74-natural-language-query-interface)
- [8. Rollout Strategy: Rule-Based → ML/AI](#8-rollout-strategy-rule-based--mlai)
  - [8.1 The Four-Phase Migration](#81-the-four-phase-migration)
  - [8.2 Spring Boot Implementation of Adapter Swap](#82-spring-boot-implementation-of-adapter-swap)
- [9. Technology Stack Summary](#9-technology-stack-summary)
- [10. Prioritized Vision Roadmap](#10-prioritized-vision-roadmap)
- [11. Key Architectural Principle: The Port/Adapter Contract](#11-key-architectural-principle-the-portadapter-contract)

---

## 1. Executive Summary

This document describes the **long-term AI vision** for the Plum Endorsement system -- capabilities that cannot be implemented today because they require one or more of:

- **Training data** that does not yet exist (labeled fraud cases, 6+ months of forecast history)
- **ML/AI infrastructure** not currently deployed (model serving, feature stores, vector databases)
- **External AI services** (LLM APIs, embedding models, ML platforms)
- **New runtime environments** (Python sidecars, gRPC services, GPU compute)
- **Organizational readiness** (data science team, MLOps practices, model governance)

The current system uses **rule-based intelligence** behind hexagonal port interfaces, with **two Ollama/LLM-augmented adapters already deployed** (anomaly explanation enrichment and error resolution). This architecture is deliberately designed to enable a **zero-application-change swap** to ML-backed adapters. The proven upgrade path is:

```
1. Implement new adapter (e.g., IsolationForestDetector) implementing existing port
2. Annotate with @ConditionalOnProperty for feature-flag rollout
3. Deploy -- domain, application, controller, and scheduler code unchanged
4. Run in shadow mode alongside rule-based/Ollama adapters
5. Promote when metrics meet targets; retain previous adapter as fallback
```

> **Important**: The Ollama/GenAI augmentation layer (see [GenAI Augmentation Strategy](../GenAI_Augmentation_Strategy.md)) serves as a **concrete bridge** between rule-based and full ML. It proves the adapter swap pattern works in production, with 2 adapters already operational and 3 more planned. The vision capabilities below build on top of this proven foundation.

### Vision Capabilities: 15 Items Across 5 Pillars

| Pillar | Rule-Based | Ollama/GenAI (Bridge) | Vision (Full ML) | Prerequisite |
|--------|-----------|----------------------|-----------------|-------------|
| Anomaly Detection | 4 scoring rules | LLM-enriched explanations (DEPLOYED) | Isolation Forest + Autoencoder | 90+ days labeled fraud data |
| Balance Forecasting | Linear + seasonality | LLM-generated narratives (PLANNED) | Prophet / ARIMA / LSTM | 6+ months historical data |
| Error Resolution | Keyword matching | LLM-powered resolution (DEPLOYED) | RAG pipeline (vector store) | LLM API + vector DB |
| Process Mining | Event pair analysis | LLM bottleneck recommendations (PLANNED) | PM4Py conformance checking | Python sidecar + gRPC |
| Batch Optimization | 0-1 knapsack DP | LLM optimization reports (PLANNED) | OR-Tools LP | Google OR-Tools Java API |

---

## 2. Pillar 1: ML-Based Anomaly Detection

### 2.1 Isolation Forest Anomaly Detector

**What it does**: Unsupervised anomaly detection that learns "normal" endorsement patterns from historical data and flags endorsements that deviate from the learned distribution. Unlike the current 4-rule system, Isolation Forest detects **novel anomaly patterns** without explicit rule coding.

**How it works**:
```
Training Phase:
  Input: 90+ days of endorsement data (features extracted)
  Features per endorsement:
    - premiumAmount (normalized by employer mean)
    - endorsementCount_24h (for this employer)
    - daysSinceLastEndorsement (for this employee)
    - coverageStartDaysAhead
    - employeeAge
    - endorsementType (one-hot: ADD, DELETE, UPDATE)
    - timeOfDay (hour of creation)
    - dayOfWeek
    - insurerIndex
    - previousRejectionCount (for this employee)

  Model: scikit-learn IsolationForest(n_estimators=100, contamination=0.05)
  Training: Fit on all historical data (unsupervised -- no labels needed initially)

Inference Phase:
  Input: New endorsement → extract same feature vector
  Output: anomaly_score (-1.0 to 1.0, where negative = anomalous)
  Map to 0.0-1.0 range: score = (1 - isolation_score) / 2
  If score > threshold → FLAGGED
```

**Why not implementable now**:
- Requires Python runtime for scikit-learn (no Java equivalent with comparable quality)
- Needs feature engineering pipeline (extract, normalize, encode features consistently)
- Needs at least 90 days of production data to train a meaningful model
- Requires model serving infrastructure (Flask/FastAPI microservice or sidecar)
- Needs ongoing retraining pipeline (model drift as employer patterns change)

**Architecture**:
```
┌──────────────────┐     ┌─────────────────────────┐
│ Spring Boot App  │     │ Python Sidecar           │
│                  │     │                          │
│ IsolationForest  │ gRPC│ Flask/FastAPI service     │
│ Detector         │────>│   scikit-learn model      │
│ (@Component      │     │   Feature extraction      │
│  @Primary)       │<────│   Anomaly scoring         │
│                  │     │                          │
│ Fallback:        │     │ Model stored in:         │
│ RuleBasedDetector│     │   - MLflow registry       │
│ (circuit breaker)│     │   - S3/GCS bucket         │
└──────────────────┘     └─────────────────────────┘
```

**Prerequisites**:
- [ ] Python sidecar container in Docker Compose and K8s
- [ ] gRPC contract definition (protobuf schema)
- [ ] Feature engineering pipeline (consistent between training and inference)
- [ ] 90+ days of endorsement data in production
- [ ] MLflow or similar model registry
- [ ] Retraining schedule (weekly or monthly)
- [ ] Model performance monitoring (precision/recall over time)

**Estimated effort**: 6-8 weeks (including infrastructure, training, validation)

### 2.2 Autoencoder Second-Stage Classifier

**What it does**: A neural network that learns compressed representations of "normal" endorsements. Endorsements that cannot be well-reconstructed are flagged as anomalous. Used as a **second stage** after Isolation Forest to reduce false positives.

**How it works**:
```
Architecture:
  Encoder: Input(10 features) → Dense(64) → Dense(32) → Dense(8) [latent space]
  Decoder: Dense(8) → Dense(32) → Dense(64) → Output(10 features)

  Training: Minimize reconstruction error on normal endorsements only
  Inference: If reconstruction_error > threshold → anomalous

  Two-stage pipeline:
    Stage 1: IsolationForest flags candidates (high recall, lower precision)
    Stage 2: Autoencoder confirms (lower recall, higher precision)
    Combined: High precision AND recall
```

**Why not implementable now**:
- Requires labeled data (need to know which endorsements are truly normal)
- Requires deep learning framework (PyTorch or TensorFlow)
- Requires GPU for training (CPU inference is acceptable)
- Needs hyperparameter tuning (latent dimension, learning rate, epochs)
- Complex to explain to auditors (black box model)

**Prerequisites**:
- [ ] 5,000+ labeled endorsements (normal vs. fraudulent)
- [ ] PyTorch or TensorFlow in Python sidecar
- [ ] GPU instance for training (cloud or on-premise)
- [ ] Model interpretability layer (SHAP values or attention weights)

**Estimated effort**: 4-6 weeks (after Isolation Forest is deployed)

### 2.3 Graph-Based Fraud Network Detection

**What it does**: Models relationships between employers, employees, insurers, and endorsements as a graph. Uses graph neural networks (GNNs) to detect fraud rings -- clusters of entities that collaborate on fraudulent endorsements.

**Example detection**:
```
Normal: Employee A works for Employer X, covered by Insurer Y
Suspicious: Employee A appears at Employer X, Y, and Z within 30 days
            AND Employers X, Y, Z share the same HR contact person
            AND All three employers recently had volume spikes

Graph representation:
  Node: Employee A ──── Employer X ──── HR Contact "John"
                    \── Employer Y ──── HR Contact "John"
                    \── Employer Z ──── HR Contact "John"

  GNN detects: Unusual clustering pattern → flag all 3 employers
```

**Why not implementable now**:
- Requires graph database (Neo4j or Amazon Neptune)
- Requires GNN framework (PyTorch Geometric or DGL)
- Requires employer relationship data not currently captured
- Needs significant labeled fraud ring data (rare events)
- Complex deployment and inference pipeline

**Estimated effort**: 12-16 weeks

---

## 3. Pillar 2: Advanced Time-Series Forecasting

### 3.1 Facebook Prophet Forecasting

**What it does**: Facebook's Prophet library handles time-series forecasting with automatic changepoint detection, holiday effects, and uncertainty intervals. Produces probabilistic forecasts (80% and 95% confidence intervals) instead of point estimates.

**How it differs from current**:
```
Current StatisticalForecastEngine:
  - Linear burn rate × hardcoded seasonality factors
  - No changepoint detection (employer doubles in size → forecast is wrong)
  - No holiday handling (Diwali, Holi pause processing but factors don't account)
  - Point estimate only (₹750,000 needed) -- no uncertainty range
  - Same factors for all employers

Prophet:
  - Automatically detects trend changes (employer grows, shrinks, pauses)
  - Indian holiday calendar built-in (Diwali, Holi, Independence Day)
  - Returns: ₹750,000 (80% CI: ₹680,000 - ₹820,000; 95% CI: ₹620,000 - ₹900,000)
  - Per-employer model (learns each employer's unique patterns)
  - Automatic weekly + yearly seasonality decomposition
```

**Why not implementable now**:
- Prophet is Python-only (no mature Java implementation)
- Requires 6+ months of daily data per employer-insurer pair
- Requires per-employer model training (100K employers = 100K models)
- Model storage and serving at scale is non-trivial
- Indian holiday calendar needs manual curation

**Architecture**:
```
┌──────────────┐      ┌────────────────────┐
│ Spring Boot  │ REST │ Prophet Service     │
│              │─────>│ (Python FastAPI)    │
│ ProphetForecast     │                    │
│ Engine       │<─────│ train(employer_data)│
│ (@Primary)   │      │ predict(days_ahead) │
│              │      │                    │
│ Fallback:    │      │ Model Store:       │
│ Statistical  │      │   S3 / Redis       │
│ ForecastEngine│     │   Per-employer .pkl │
└──────────────┘      └────────────────────┘
```

**Prerequisites**:
- [ ] 6+ months of EA transaction and endorsement data per employer
- [ ] Python forecasting microservice (FastAPI + Prophet)
- [ ] Indian holiday calendar dataset
- [ ] Model storage (S3 or Redis for serialized models)
- [ ] Batch retraining pipeline (weekly, per employer)
- [ ] Forecast accuracy comparison: Prophet vs. current engine

**Estimated effort**: 6-8 weeks

### 3.2 LSTM Neural Network Forecasting

**What it does**: Long Short-Term Memory network for capturing complex temporal dependencies in endorsement volume and EA balance patterns. Handles multi-step forecasting with variable-length input sequences.

**When to use instead of Prophet**:
- When endorsement patterns have complex non-linear relationships
- When multiple features interact (premium + type + insurer + time)
- When you need real-time forecast updates (LSTM inference is fast)

**Why not implementable now**:
- Requires deep learning framework (PyTorch)
- Needs large training dataset (12+ months per employer)
- Hyperparameter tuning is computationally expensive
- Less interpretable than Prophet (harder to explain to stakeholders)
- Overfitting risk on small employers with few endorsements

**Estimated effort**: 8-10 weeks (after Prophet is validated)

### 3.3 Ensemble Forecasting

**What it does**: Combines multiple forecasting methods (statistical, Prophet, LSTM) and uses a meta-learner to weight their predictions based on recent accuracy.

```
Ensemble Pipeline:
  1. StatisticalForecastEngine → forecast₁ (current system)
  2. ProphetForecastEngine    → forecast₂
  3. LSTMForecastEngine       → forecast₃

  Meta-learner (XGBoost or weighted average):
    weights = [0.15, 0.50, 0.35]  (learned from backtesting)
    final_forecast = Σ(weight_i × forecast_i)

  Advantage: Robust to individual model failures. If Prophet
  breaks on one employer, Statistical + LSTM compensate.
```

**Prerequisites**:
- [ ] Prophet and LSTM models deployed and validated
- [ ] Backtesting pipeline producing per-model accuracy scores
- [ ] Meta-learner training data (6+ months of model predictions vs. actuals)

**Estimated effort**: 4-6 weeks (after Prophet + LSTM are deployed)

---

## 4. Pillar 3: LLM-Powered Error Resolution

### 4.1 RAG Error Resolver (Retrieval-Augmented Generation)

> **Bridge already deployed**: `OllamaErrorResolver` provides LLM-powered error resolution using local Ollama (llama3.2) with structured JSON output. See [GenAI Augmentation Strategy §2.2](../GenAI_Augmentation_Strategy.md#22-existing-ollama-integration-2-adapters). The RAG approach below extends this with vector similarity search for higher accuracy.

**What it does**: Instead of matching errors to hardcoded keyword patterns, uses a **vector database** of historical error-resolution pairs and an **LLM** to generate contextual fixes for novel errors.

**How it works**:
```
Step 1: EMBED the error message
  Input:  "ICICI Lombard rejected: Member ID A1B2C3D4 not found in master database"
  Output: Vector embedding [0.12, -0.45, 0.78, ...] (768 dimensions)

Step 2: RETRIEVE similar past errors from vector database
  Query:  Nearest 5 neighbors to the error embedding
  Results:
    1. "Member ID format incorrect" → Fix: "Add PLM- prefix" (success: 97%)
    2. "Member not enrolled"        → Fix: "Check enrollment date" (success: 85%)
    3. "ID mismatch"                → Fix: "Verify with HR system" (success: 80%)

Step 3: GENERATE resolution using LLM
  Prompt: "Given this insurer error: '{error_message}'
           and these similar past resolutions: {top_5_examples}
           and this endorsement context: {endorsement_data}
           Generate a specific fix with confidence score."

  LLM Response:
    {
      "errorType": "MEMBER_ID_FORMAT",
      "correctedValue": "PLM-A1B2C3D4",
      "explanation": "ICICI Lombard requires PLM- prefix. Based on 97 similar
                      past cases, adding the prefix resolves this error 97% of the time.",
      "confidence": 0.96,
      "suggestedSteps": [
        "1. Verify employee ID matches HR system",
        "2. Apply PLM- prefix transformation",
        "3. Resubmit to ICICI Lombard"
      ]
    }
```

**Why not implementable now**:
- Requires LLM API access (Claude API, OpenAI, or self-hosted model)
- Requires vector database (Pinecone, Weaviate, pgvector, or Qdrant)
- Requires embedding model (OpenAI text-embedding-3-small or sentence-transformers)
- LLM latency (100-500ms) may impact SLA for real-time resolution
- Non-deterministic outputs need guardrails (output validation, confidence calibration)
- Cost: LLM inference at scale (1M errors/day × $0.01/call = $10K/day)
- Needs historical error-resolution dataset (1000+ labeled pairs minimum)

**Architecture**:
```
┌──────────────┐      ┌────────────────────┐      ┌───────────────┐
│ Spring Boot  │      │ RAG Service        │      │ Vector DB     │
│              │ REST │ (LangChain4j or    │      │ (Pinecone /   │
│ RAGError     │─────>│  Spring AI)        │─────>│  pgvector)    │
│ Resolver     │      │                    │      │               │
│ (@Primary)   │<─────│ 1. Embed error     │<─────│ Top-K similar │
│              │      │ 2. Retrieve similar│      │ error vectors │
│ Fallback:    │      │ 3. Prompt LLM      │      └───────────────┘
│ Simulated    │      │ 4. Parse response  │
│ ErrorResolver│      │                    │      ┌───────────────┐
└──────────────┘      │                    │─────>│ LLM API       │
                      │                    │<─────│ (Claude /     │
                      └────────────────────┘      │  GPT-4)       │
                                                  └───────────────┘
```

**Technology options**:

| Component | Option A | Option B | Option C |
|-----------|----------|----------|----------|
| LLM | Claude API (Anthropic) | GPT-4 (OpenAI) | Self-hosted Llama 3 |
| Embedding | text-embedding-3-small | sentence-transformers | Cohere embed |
| Vector DB | pgvector (PostgreSQL) | Pinecone | Weaviate |
| Orchestration | LangChain4j (Java) | Spring AI | Custom HTTP client |

**Recommended**: LangChain4j + Claude API + pgvector (PostgreSQL extension). Keeps the stack Java-native, uses existing PostgreSQL, and leverages Claude's strong reasoning for error analysis.

**Prerequisites**:
- [ ] LLM API key and budget approval ($500-2000/month estimated)
- [ ] pgvector extension added to PostgreSQL (or separate vector DB)
- [ ] 1000+ historical error-resolution pairs for embedding
- [ ] LangChain4j dependency added to project
- [ ] Output validation layer (ensure LLM responses conform to expected schema)
- [ ] Latency budget analysis (can the endorsement processing pipeline tolerate 200-500ms?)
- [ ] Fallback strategy (circuit breaker to SimulatedErrorResolver when LLM is unavailable)

**Estimated effort**: 8-10 weeks

### 4.2 Natural Language Anomaly Explanations via LLM

> **Bridge already deployed**: `OllamaAugmentedAnomalyDetector` enriches rule-based anomaly explanations via local Ollama LLM with a fraud analyst persona prompt. See [GenAI Augmentation Strategy §2.2](../GenAI_Augmentation_Strategy.md#22-existing-ollama-integration-2-adapters). The vision below extends this with cloud-hosted LLMs for richer, multi-signal explanations.

**What it does**: Instead of template-based anomaly explanations, uses an LLM to generate contextual, human-readable explanations that consider the full endorsement history and employer context.

**Example**:
```
Template-based (current):
  "Volume spike detected: 15 endorsements in 24h vs average of 2.5/day"

LLM-generated:
  "Acme Corporation submitted 15 endorsements yesterday -- 6 times their usual
   daily volume of 2.5. This is their third volume spike in 90 days. The previous
   two were during their April hiring wave and were legitimate. However, this spike
   is unusual because: (1) it's occurring in March outside typical hiring seasons,
   (2) all 15 endorsements are for the same insurance plan, and (3) the total
   premium of ₹1.87 lakhs represents 37% of their EA balance. I recommend reviewing
   the employee records to verify these are legitimate new hires."
```

**Why not implementable now**: Same LLM prerequisites as RAG Error Resolver (API access, cost, latency). Additionally, explanation generation is lower priority than error resolution (explanations can wait; error fixes are time-sensitive).

**Estimated effort**: 2-3 weeks (after RAG infrastructure is deployed)

---

## 5. Pillar 4: Advanced Process Mining

### 5.1 PM4Py Process Mining Engine

**What it does**: PM4Py is the leading open-source process mining library. It provides capabilities far beyond the current `EventStreamAnalyzer`:

| Capability | Current (EventStreamAnalyzer) | PM4Py |
|-----------|------------------------------|-------|
| Transition duration stats | Mean, P95, P99 | Same + distribution analysis |
| Happy path detection | Binary (has rejection?) | Full variant analysis with frequency |
| Conformance checking | None | Compare actual vs. expected process model |
| Social network mining | None | Discover handoff patterns between systems |
| Root cause analysis | None | Correlate bottlenecks with attributes |
| Process model discovery | None | Auto-generate BPMN model from event logs |
| Performance spectrum | None | Visualize performance across all transitions |

**Conformance checking example**:
```
Expected Process Model (BPMN):
  CREATED → VALIDATED → PROVISIONALLY_COVERED → QUEUED_FOR_BATCH → BATCH_SUBMITTED → CONFIRMED

Variant Analysis Output:
  Variant 1 (78%): CREATED → VALIDATED → PROV_COVERED → QUEUED → BATCH_SUB → CONFIRMED  [Happy]
  Variant 2 (12%): CREATED → VALIDATED → SUBMITTED_RT → CONFIRMED                       [RT Path]
  Variant 3 (7%):  CREATED → VALIDATED → QUEUED → REJECTED → RETRY → QUEUED → CONFIRMED [Retry]
  Variant 4 (3%):  CREATED → VALIDATED → QUEUED → REJECTED → FAILED_PERMANENT           [Failure]

Conformance Score: 78% (variant 1 matches expected model)
Deviations: 22% of endorsements deviate from the expected process
```

**Why not implementable now**:
- PM4Py is Python-only
- Requires Python sidecar with gRPC interface
- Event log format conversion (EndorsementEvent → XES event log format)
- Visualization outputs (BPMN diagrams, directly rendered graphs) need frontend integration
- Computational cost: processing millions of events requires batch analysis infrastructure

**Architecture**:
```
┌──────────────┐      ┌────────────────────┐
│ Spring Boot  │ gRPC │ PM4Py Service      │
│              │─────>│ (Python)           │
│ PM4PyProcess │      │                    │
│ Analyzer     │<─────│ Inputs:            │
│ (@Primary)   │      │   - XES event log  │
│              │      │                    │
│ Fallback:    │      │ Outputs:           │
│ EventStream  │      │   - Variant list   │
│ Analyzer     │      │   - Conformance %  │
└──────────────┘      │   - Bottleneck map │
                      │   - Social network │
                      │   - BPMN model     │
                      └────────────────────┘
```

**Prerequisites**:
- [ ] Python sidecar container
- [ ] gRPC service contract (protobuf)
- [ ] XES event log conversion utility
- [ ] Frontend BPMN rendering (bpmn-js or custom SVG)
- [ ] Batch data pipeline (export events to PM4Py-consumable format)

**Estimated effort**: 8-12 weeks

### 5.2 Real-Time Process Monitoring with Kafka Streams

**What it does**: Instead of daily batch analysis, processes endorsement events in real-time via Kafka Streams to detect bottlenecks and STP rate drops within minutes.

```
Current: Daily batch analysis at 3 AM → results available next morning
Vision:  Real-time streaming → bottleneck alert within 5 minutes of detection

Kafka Streams Pipeline:
  endorsement-events topic
       |
       v
  [Window: 15-minute tumbling]
       |
       v
  [Group by: insurerId + transition]
       |
       v
  [Aggregate: count, avgDuration, maxDuration]
       |
       v
  [Detect: avgDuration > 2× baseline? → ALERT]
       |
       v
  intelligence-alerts topic → WebSocket → Dashboard
```

**Why not implementable now**:
- Kafka Streams application requires separate deployment topology
- Windowed aggregation state needs RocksDB or similar state store
- Baseline computation requires initial historical data
- Alert routing infrastructure (separate from batch scheduler flow)
- Testing Kafka Streams topologies requires specialized test harness

**Estimated effort**: 6-8 weeks

---

## 6. Pillar 5: Advanced Batch Optimization

### 6.1 Google OR-Tools Linear Programming Optimizer

**What it does**: Replaces the heuristic 0-1 knapsack with a true linear programming solver that handles multiple hard and soft constraints simultaneously, finding a provably optimal solution.

**Constraints it can model**:
```
Hard constraints (must satisfy):
  1. Total premium ≤ EA available balance
  2. Batch size ≤ insurer max batch size
  3. Processing within insurer's submission window (e.g., 9 AM - 5 PM)
  4. No duplicate employees in same batch

Soft constraints (objectives to maximize):
  1. Maximize total urgency score
  2. Maximize EA balance utilization (minimize idle balance)
  3. Minimize total processing time (prefer fast insurers)
  4. Balance distribution across time windows (fairness)

Multi-employer constraints (not possible with current optimizer):
  5. "Process employer A before employer B" (dependency ordering)
  6. "Maximum Rs. 10 lakhs premium per employer per batch" (insurer limit)
  7. "Even distribution across daily submission windows" (load balancing)
```

**Why not implementable now**:
- OR-Tools Java API requires native library installation (platform-specific)
- Constraint modeling requires domain expert knowledge (operations team input)
- Testing provably optimal solutions needs validation harness
- Performance: LP solver may be slower than heuristic for large batches (> 10,000 items)
- The current heuristic (0-1 knapsack) works well for the current scale

**When to invest**: When the batch size exceeds 1,000 items per batch, or when multi-employer/multi-insurer constraints become a real business need.

**Estimated effort**: 6-8 weeks

### 6.2 Reinforcement Learning Optimizer

**What it does**: An RL agent learns optimal batch assembly strategies by trial-and-error interaction with the environment (simulator of the endorsement processing pipeline).

```
RL Formulation:
  State:  (queue contents, EA balances, insurer availability, time of day)
  Action: Select next endorsement to add to batch (or submit batch)
  Reward: +1 for confirmed endorsement, -1 for rejected, +0.5 for balance utilization

  Agent: DQN or PPO (Deep Q-Network or Proximal Policy Optimization)
  Environment: Simulator trained on historical endorsement processing data
```

**Why not implementable now**:
- Requires building a realistic endorsement processing simulator
- RL training is computationally expensive (thousands of episodes)
- Non-deterministic behavior is risky for financial operations
- Current heuristic optimizer provides near-optimal results in O(n log n) time
- RL benefits emerge only at very large scale (10K+ endorsements per batch)

**When to invest**: When batch volumes exceed 10,000 per insurer per day, and the heuristic optimizer shows measurable suboptimality.

**Estimated effort**: 12-16 weeks

---

## 7. Cross-Cutting Vision Capabilities

### 7.1 Feature Store

**What it does**: A centralized system for computing, storing, and serving ML features consistently between training and inference. Prevents the most common ML production bug: training/serving skew.

```
Feature Store Architecture:
  ┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
  │ Raw Data    │────>│ Feature      │────>│ Online Store     │
  │ (PostgreSQL,│     │ Engineering  │     │ (Redis/DynamoDB) │
  │  Kafka)     │     │ Pipeline     │     │ Low-latency      │
  └─────────────┘     │ (Spark/Flink)│     │ serving for      │
                      └──────────────┘     │ inference        │
                             |             └─────────────────┘
                             v
                      ┌──────────────┐
                      │ Offline Store│
                      │ (S3/BigQuery)│
                      │ Historical   │
                      │ features for │
                      │ training     │
                      └──────────────┘

  Features served:
    - employer_avg_premium_30d
    - employer_endorsement_velocity_7d
    - employee_coverage_gap_days
    - insurer_rejection_rate_30d
    - employer_seasonality_factor
    - ea_balance_burn_rate
```

**Technology options**: Feast (open source), Tecton (managed), or custom Redis-based solution.

**Prerequisites**:
- [ ] At least 2 ML models in production (to justify the investment)
- [ ] Data engineering capacity (feature pipeline development)
- [ ] Cloud data warehouse (Snowflake, BigQuery, or Redshift)

**Estimated effort**: 8-12 weeks

### 7.2 MLOps Pipeline

**What it does**: End-to-end automation for ML model lifecycle: training, validation, deployment, monitoring, and retraining.

```
MLOps Pipeline:
  1. Data Collection  → PostgreSQL/Kafka → Data Warehouse
  2. Feature Eng.     → Feature Store → Training Dataset
  3. Model Training   → MLflow Experiment Tracking
  4. Validation       → Backtesting + A/B comparison
  5. Deployment       → Model Registry → Serving Infra
  6. Monitoring       → Performance Dashboards
  7. Drift Detection  → Auto-trigger retraining
  8. Retraining       → Go to step 3

  ┌─────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
  │ Train   │────>│ Validate │────>│ Deploy   │────>│ Monitor  │
  │ (MLflow)│     │ (backtest)│    │ (registry)│    │ (Grafana)│
  └─────────┘     └──────────┘     └──────────┘     └──────────┘
       ^                                                  |
       |                                                  v
       +──────────── Retrain trigger ◄───── Drift detected
```

**Prerequisites**:
- [ ] ML models in production (at least 1)
- [ ] MLflow or Weights & Biases deployment
- [ ] CI/CD pipeline for model artifacts (separate from application CI/CD)
- [ ] Data quality monitoring (Great Expectations or similar)
- [ ] Model governance policy (approval workflow for production models)

**Estimated effort**: 10-14 weeks

### 7.3 A/B Testing Framework for Intelligence

**What it does**: Runs two intelligence adapters (e.g., rule-based vs. ML) simultaneously on different subsets of traffic, measuring which produces better outcomes.

```
A/B Testing Framework:
  Request arrives → Router checks feature flag
       |
       +──── Group A (control): RuleBasedAnomalyDetector
       |     └── Measure: precision, recall, F1, latency
       |
       +──── Group B (treatment): IsolationForestDetector
              └── Measure: precision, recall, F1, latency

  Routing strategies:
    1. Random (50/50 split)
    2. Per-employer (all traffic for employer X goes to group B)
    3. Gradual rollout (10% → 25% → 50% → 100%)

  Statistical significance:
    - Minimum 1000 samples per group
    - Chi-squared test for categorical outcomes
    - Two-sample t-test for continuous metrics
    - Require p < 0.05 before promoting treatment to production
```

**Prerequisites**:
- [ ] At least 2 adapter implementations for the same port
- [ ] Feature flag system (LaunchDarkly or custom @ConditionalOnProperty)
- [ ] Metrics comparison dashboard
- [ ] Statistical significance calculator

**Estimated effort**: 4-6 weeks

### 7.4 Natural Language Query Interface

**What it does**: Allows operators to query the endorsement system using natural language instead of structured filters.

```
Operator types: "Show me all rejected endorsements for Acme Corp this week"

NLQ Pipeline:
  1. Parse intent: QUERY
  2. Extract entities: employer="Acme Corp", status="REJECTED", timeRange="this week"
  3. Generate SQL/API call:
     GET /endorsements?employerId={acme-uuid}&statuses=REJECTED&createdAfter=2026-03-07
  4. Return results to operator

Advanced queries:
  "Why was endorsement #abc123 rejected?"
  → Retrieves endorsement + error resolution + insurer response
  → LLM generates narrative explanation

  "What's the trend in STP rate for ICICI Lombard?"
  → Queries STP rate history
  → LLM generates trend analysis with chart
```

**Why not implementable now**:
- Requires LLM for intent parsing and response generation
- Entity extraction needs domain-specific NER (endorsement IDs, employer names)
- Query translation (NL → API) needs careful validation to prevent injection
- Cost and latency of LLM inference for every query

**Estimated effort**: 6-8 weeks (after LLM infrastructure is deployed)

---

## 8. Rollout Strategy: Rule-Based → ML/AI

### 8.1 The Five-Phase Migration

```
Phase 0: Ollama/GenAI Augmentation (AVAILABLE NOW)
┌────────────────────────────────────────────┐
│ Enable SPRING_PROFILES_ACTIVE=ollama       │
│ Rule-based scoring unchanged (decisions)   │
│ LLM enriches explanations/narratives       │
│ @CircuitBreaker → fallback to rules        │
│ Zero risk: rules still make all decisions  │
│ Status: 2/5 adapters DEPLOYED              │
│ See: GenAI Augmentation Strategy           │
└────────────────────────────────────────────┘
                    |
                    v
Phase 1: Shadow Mode (Weeks 1-4)
┌────────────────────────────────────────────┐
│ Both ML + Ollama adapters run              │
│ Ollama: serves production traffic          │
│ ML-based: runs in background, logs results │
│ Compare: precision, recall, latency        │
│ Metric: shadow_comparison_dashboard        │
│ Exit criteria: ML precision >= Ollama      │
└────────────────────────────────────────────┘
                    |
                    v
Phase 2: Canary Rollout (Weeks 5-8)
┌────────────────────────────────────────────┐
│ ML adapter serves 10% of traffic           │
│ Monitor: precision, recall, latency, cost  │
│ Feature flag: per-employer opt-in          │
│ Circuit breaker: fallback to Ollama        │
│   if ML error rate > 5% or latency > 500ms│
│ Exit criteria: No degradation at 10%       │
└────────────────────────────────────────────┘
                    |
                    v
Phase 3: Graduated Rollout (Weeks 9-12)
┌────────────────────────────────────────────┐
│ ML adapter: 10% → 25% → 50% → 90%        │
│ Each step: 1 week observation period       │
│ Rollback trigger: precision drop > 5%      │
│ Fallback chain: ML → Ollama → Rules        │
│ Exit criteria: Stable at 90% for 7 days    │
└────────────────────────────────────────────┘
                    |
                    v
Phase 4: Full Migration
┌────────────────────────────────────────────┐
│ ML adapter: @Primary (100% of traffic)     │
│ Ollama + rule-based: retained as fallbacks │
│ Continuous monitoring: daily accuracy check │
│ Retraining: weekly on new labeled data     │
│ Drift alert: if precision drops 10%        │
└────────────────────────────────────────────┘
```

### 8.2 Spring Boot Implementation of Adapter Swap

**Already deployed** — the `@ConditionalOnProperty` pattern:

```java
// Default: Rule-based (active when ollama.enabled=false or not set)
@Component
@ConditionalOnProperty(name = "endorsement.intelligence.ollama.enabled",
                       havingValue = "false", matchIfMissing = true)
public class RuleBasedAnomalyDetector implements AnomalyDetectionPort { ... }

// Ollama/GenAI bridge (active when ollama.enabled=true) — DEPLOYED
@Component
@ConditionalOnProperty(name = "endorsement.intelligence.ollama.enabled",
                       havingValue = "true")
public class OllamaAugmentedAnomalyDetector implements AnomalyDetectionPort {
    private final RuleBasedAnomalyScorer scorer;  // deterministic scoring
    private final ChatClient.Builder chatClientBuilder;  // LLM enrichment
    // Rules make decisions; LLM enriches explanations only
}
```

**Future** — gradual rollout decorator for full ML:

```java
// Phase 2-3: Gradual rollout via decorator
@Component @Primary
public class GradualRolloutDetector implements AnomalyDetectionPort {
    private final OllamaAugmentedAnomalyDetector ollamaBased;  // current best
    private final IsolationForestDetector mlBased;               // new ML adapter

    @Value("${endorsement.intelligence.anomaly.ml-traffic-pct:0}")
    private int mlTrafficPercentage; // 0, 10, 25, 50, 90, 100

    @Override
    public AnomalyResult analyzeEndorsement(Endorsement e, List<Endorsement> history) {
        if (ThreadLocalRandom.current().nextInt(100) < mlTrafficPercentage) {
            try {
                return mlBased.analyzeEndorsement(e, history);
            } catch (Exception ex) {
                log.warn("ML adapter failed, falling back to Ollama: {}", ex.getMessage());
                return ollamaBased.analyzeEndorsement(e, history);
            }
        }
        return ollamaBased.analyzeEndorsement(e, history);
    }
}

// Phase 4: Full migration
// Set ml-traffic-pct=100
// Ollama + rule-based adapters remain as fallback chain
```

---

## 9. Technology Stack Summary

### Already Deployed (Ollama/GenAI Bridge)

| Component | Purpose | Status |
|-----------|---------|--------|
| **Spring AI** (`spring-ai-ollama-spring-boot-starter`) | LLM abstraction layer, ChatClient API | Deployed |
| **Ollama** (local, Docker) | Local LLM inference (llama3.2) | Deployed |
| **application-ollama.yml** | Spring profile: temp 0.3, 512 tokens | Deployed |
| **Resilience4j** (Ollama instances) | Circuit breaker + retry per adapter | Deployed |
| **@ConditionalOnProperty** | Adapter toggle (`ollama.enabled=true/false`) | Deployed |

### Required Infrastructure (Not Yet Deployed)

| Component | Purpose | Options | Estimated Cost |
|-----------|---------|---------|---------------|
| **Python Sidecar** | ML model serving (scikit-learn, Prophet, PM4Py) | FastAPI container | Infrastructure only |
| **LLM API** | Error resolution, explanations, NLQ | Claude API, GPT-4 | $500-2000/month |
| **Vector Database** | RAG error resolution (embedding storage) | pgvector, Pinecone | $0-200/month |
| **Embedding Model** | Text → vector conversion | text-embedding-3-small | $50-100/month |
| **Model Registry** | ML model versioning and serving | MLflow (self-hosted) | Infrastructure only |
| **Feature Store** | Consistent feature engineering | Feast (open source) | Infrastructure only |
| **GPU Compute** | Model training (Autoencoder, LSTM) | Cloud GPU instances | $200-500/training run |
| **Graph Database** | Fraud network detection | Neo4j, Amazon Neptune | $300-500/month |
| **OR-Tools** | Batch optimization LP solver | Google OR-Tools (free) | Free (Java library) |

### Java Dependencies

| Dependency | Purpose | Status |
|-----------|---------|--------|
| `spring-ai-ollama-spring-boot-starter` | Spring AI + Ollama integration | **Already added** |
| `langchain4j-core` | LLM orchestration framework | RAG error resolver (future) |
| `langchain4j-anthropic` | Claude API integration | RAG error resolver (future) |
| `pgvector` (PostgreSQL extension) | Vector similarity search | RAG error resolver (future) |
| `com.google.ortools` | Linear programming solver | OR-Tools optimizer (future) |

---

## 10. Prioritized Vision Roadmap

### Priority Order (based on business impact / implementation complexity)

| Priority | Capability | Impact | Effort | Prerequisites |
|----------|-----------|--------|--------|---------------|
| **V1** | RAG Error Resolver (LLM + pgvector) | Very High | 8-10 weeks | LLM API key, 1000+ error-resolution pairs |
| **V2** | Prophet Forecasting | High | 6-8 weeks | 6 months data, Python sidecar |
| **V3** | Isolation Forest Anomaly Detection | High | 6-8 weeks | 90 days data, Python sidecar |
| **V4** | A/B Testing Framework | Medium | 4-6 weeks | V2 or V3 deployed |
| **V5** | PM4Py Process Mining | Medium | 8-12 weeks | Python sidecar |
| **V6** | Real-Time Streaming (Kafka Streams) | Medium | 6-8 weeks | Baseline data |
| **V7** | OR-Tools Batch Optimizer | Medium | 6-8 weeks | OR-Tools Java library |
| **V8** | MLOps Pipeline | Medium | 10-14 weeks | V2 + V3 deployed |
| **V9** | Autoencoder Classifier | Low | 4-6 weeks | 5000+ labeled records |
| **V10** | Feature Store | Low | 8-12 weeks | 2+ ML models in production |
| **V11** | Natural Language Query | Low | 6-8 weeks | LLM infrastructure (from V1) |
| **V12** | LSTM Forecasting | Low | 8-10 weeks | V2 validated |
| **V13** | Ensemble Forecasting | Low | 4-6 weeks | V2 + V12 validated |
| **V14** | Graph Fraud Detection | Low | 12-16 weeks | Graph DB, relationship data |
| **V15** | RL Batch Optimizer | Very Low | 12-16 weeks | Simulator, 10K+ daily volume |

### Recommended Sequence

```
Quarter 1: Foundation
  V1: RAG Error Resolver (highest ROI -- auto-resolve rate jumps from ~40% to ~80%)
  V4: A/B Testing Framework (enables safe deployment of all future ML adapters)

Quarter 2: Core ML
  V2: Prophet Forecasting (replaces linear extrapolation with adaptive prediction)
  V3: Isolation Forest (detects novel fraud patterns without explicit rules)

Quarter 3: Scale & Depth
  V5: PM4Py Process Mining (advanced conformance checking and variant analysis)
  V7: OR-Tools Optimizer (provably optimal batching for complex constraints)
  V8: MLOps Pipeline (automate model lifecycle for V2 + V3)

Quarter 4+: Advanced
  V6, V9-V15 (based on business priorities and data availability)
```

---

## 11. Key Architectural Principle: The Port/Adapter Contract

The most important design decision in the current system is that **all intelligence is behind port interfaces**. This means every vision item in this document follows the same deployment pattern:

```
1. Create new adapter implementing existing port interface
2. Add @Component @Primary annotation
3. Configure via @ConditionalOnProperty for gradual rollout
4. Deploy -- zero changes to domain, application, controller, or scheduler code
5. Run in shadow mode alongside rule-based adapter
6. Promote to production when metrics meet targets
7. Retain rule-based adapter as circuit-breaker fallback

This pattern applies to ALL 15 vision items.
```

The port interfaces are **stable contracts**:

| Port | Method | Adapters (current) | Stable Since |
|------|--------|--------------------|-------------|
| `AnomalyDetectionPort` | `analyzeEndorsement` | RuleBasedAnomalyDetector, **OllamaAugmentedAnomalyDetector** | Day 1 |
| `BalanceForecastPort` | `generateForecast` | StatisticalForecastEngine | Day 1 |
| `ErrorResolutionPort` | `analyzeError` | SimulatedErrorResolver, **OllamaErrorResolver** | Day 1 |
| `ProcessMiningPort` | `analyzeWorkflow` | EventStreamAnalyzer | Day 1 |
| `BatchOptimizerPort` | `optimizeBatch` | ConstraintBatchOptimizer | Day 1 |

No matter how sophisticated the backend becomes -- Ollama, Isolation Forest, Prophet, RAG+LLM, PM4Py, OR-Tools -- the interfaces remain the same. The adapters are the only code that changes.

**This is no longer theoretical.** The Ollama adapters (bolded above) prove the pattern works:
- `OllamaAugmentedAnomalyDetector` implements `AnomalyDetectionPort` — same interface, enriched output
- `OllamaErrorResolver` implements `ErrorResolutionPort` — same interface, LLM-powered resolution
- Both use `@ConditionalOnProperty` for zero-downtime toggle
- Both have `@CircuitBreaker` + `@Retry` with typed fallback to rule-based

This is why the rule-based approach was the right starting point: it validated the interfaces, established the data flow, and proved the architecture. The Ollama augmentation proved the adapter swap pattern. The ML upgrade is the same deployment-time adapter swap, not an application rewrite.
