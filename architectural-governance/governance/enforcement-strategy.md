# Architectural Governance Enforcement Strategy

## Overview

This document defines the three-tier governance model for the Plum Endorsement System. It ensures architectural decisions are machine-verifiable, continuously enforced, and traceable to design patterns from **Cloud Native Patterns** (Cornelia Davis) and **Head First Design Patterns** (Freeman & Robson).

## Three-Tier Enforcement Model

### Tier 1: Schema Validation (Automated, Blocking)

| Aspect | Detail |
|--------|--------|
| **Tool** | `calm validate` (FINOS CALM CLI) |
| **Trigger** | PR touching `architectural-governance/`, `src/main/`, `k8s/`, `docker-compose.yml` |
| **What it checks** | Architecture instantiation satisfies all 5 pattern definitions |
| **Blocking?** | Yes — PR cannot merge if validation fails |
| **Run locally** | `./scripts/validate.sh` or `npm run validate:patterns` |

**Patterns validated:**

| Pattern | Key Enforcement |
|---------|----------------|
| `hexagonal-service` | Dependencies flow inward; domain core has no outbound infra connections |
| `event-driven-service` | Partitioned topics, event publishing through ports |
| `multi-insurer-integration` | Strategy pattern, mandatory resilience controls on external calls |
| `observability-stack` | All 3 pillars required (metrics + tracing + logging) |
| `resilient-external-integration` | Circuit breaker + retry controls on every external API call |

### Tier 2: Custom Rules (Automated, Blocking)

| Aspect | Detail |
|--------|--------|
| **Tool** | Spectral CLI with custom ruleset |
| **Trigger** | Same PR trigger as Tier 1 |
| **What it checks** | Naming conventions, relationship completeness, description quality |
| **Blocking?** | Yes — errors block merge; warnings are informational |
| **Run locally** | `npm run validate:spectral` |

**Rules enforced** (see `spectral-rules/.spectral.yml`):

| Rule | Severity | Description |
|------|----------|-------------|
| `external-resilience-controls` | error | Every service→system relationship must reference resilience controls |
| `database-protocol-required` | error | Database connections must specify protocol |
| `node-descriptions-required` | warning | All nodes must have descriptions |
| `plum-naming-convention` | warning | Internal node IDs must follow `plum-*` kebab-case naming |
| `flow-minimum-transitions` | error | Flows must have ≥2 transitions |
| `service-interfaces-required` | warning | Service nodes should declare interfaces |

### Tier 3: Architecture Decision Records (Manual, Blocking)

| Aspect | Detail |
|--------|--------|
| **Tool** | Manual review by architecture owner |
| **Trigger** | New pattern definition or control modification |
| **What it checks** | Architectural fitness, pattern applicability, risk assessment |
| **Blocking?** | Yes — new patterns require ADR approval before merge |

**When a Tier 3 review is required:**

1. Adding a new pattern file to `patterns/`
2. Modifying an existing pattern's constraints
3. Adding/removing a control definition
4. Changing the architecture instantiation's node topology (adding/removing nodes)
5. Modifying partition key strategy or Kafka topic structure

## How to Extend

### Adding a New External Integration

1. Add node to `architecture/plum-endorsement-system.json` with `"node-type": "system"`
2. Add relationship with `controls` referencing resilience controls
3. Add circuit breaker + retry config to `controls/resilience-controls.json`
4. Run `./scripts/validate.sh` — must pass
5. Submit PR — Tier 1 + Tier 2 run automatically

### Adding a New Observability Component

1. Verify it maps to one of the three pillars (metrics, tracing, logging)
2. Add node to architecture instantiation
3. Add relationship from observed service
4. Run validation

### Modifying Resilience Thresholds

1. Update `controls/resilience-controls.json` with new values
2. Ensure corresponding `application.yml` values match
3. Document the change rationale (production data, incident analysis)
4. Tier 2 review not required for threshold tuning (values only)

## Drift Detection

Architecture drift occurs when the running system diverges from the CALM model. To detect drift:

1. **Pre-commit**: `./scripts/validate.sh` catches model inconsistencies locally
2. **CI**: GitHub Actions runs validation on every PR
3. **Manual audit**: Quarterly comparison of CALM model vs running infrastructure
4. **Future**: Automated runtime validation comparing CALM model with K8s state

## Governance Metrics

Track these to measure governance effectiveness:

| Metric | Target | Measurement |
|--------|--------|-------------|
| Validation pass rate | 100% on merge | CI pipeline success rate |
| Mean time to resolve validation failure | < 1 hour | PR timeline analysis |
| Architecture coverage | All external integrations modeled | Node count vs actual services |
| Control adherence | All external calls have CB + retry | Spectral rule pass rate |
