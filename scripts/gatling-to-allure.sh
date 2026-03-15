#!/usr/bin/env bash
# ============================================================
# Convert Gatling stats.json to Allure-compatible result JSONs
# ============================================================
# Usage:
#   ./scripts/gatling-to-allure.sh [gatling-report-dir]
#
# If no argument, auto-detects latest report under
# performance-tests/build/reports/gatling/
#
# Output: performance-tests/build/allure-results/
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PERF_BUILD="$PROJECT_ROOT/performance-tests/build"
ALLURE_OUT="$PERF_BUILD/allure-results"

# ---- Locate stats.json ----
if [[ -n "${1:-}" ]]; then
  REPORT_DIR="$1"
else
  REPORT_DIR=$(ls -td "$PERF_BUILD/reports/gatling"/*/ 2>/dev/null | head -1 || true)
fi

if [[ -z "$REPORT_DIR" ]]; then
  echo "ERROR: No Gatling report directory found."
  echo "Run a simulation first: ./run-perf-tests.sh BaselineSimulation"
  exit 1
fi

STATS_FILE="$REPORT_DIR/js/stats.json"

if [[ ! -f "$STATS_FILE" ]]; then
  echo "ERROR: stats.json not found at $STATS_FILE"
  exit 1
fi

# ---- Check jq ----
if ! command -v jq &>/dev/null; then
  echo "ERROR: jq is required. Install: brew install jq"
  exit 1
fi

# ---- Prepare output ----
rm -rf "$ALLURE_OUT"
mkdir -p "$ALLURE_OUT"

# ---- Extract simulation name from report directory ----
SIM_DIR_NAME=$(basename "$REPORT_DIR")
# Gatling report dirs look like: baselinesimulation-20260307120000
SIM_NAME=$(echo "$SIM_DIR_NAME" | sed 's/-[0-9]*$//')

NOW_MS=$(date +%s000)
DURATION_MS=$(jq -r '
  if .stats.minResponseTime.total != null
  then ((.stats.maxResponseTime.total // 0) * 1000)
  else 0
  end
' "$STATS_FILE" 2>/dev/null || echo "0")

# ---- Generate Allure result for each request type ----
# stats.json structure: { type, name, stats: {...}, contents: { "req_xxx": { type, name, stats } } }

jq -r '.contents | keys[]' "$STATS_FILE" 2>/dev/null | while read -r key; do
  REQ_NAME=$(jq -r ".contents[\"$key\"].name" "$STATS_FILE")
  REQ_TYPE=$(jq -r ".contents[\"$key\"].type" "$STATS_FILE")

  # Skip GROUP entries, only process REQUESTs
  if [[ "$REQ_TYPE" != "REQUEST" ]]; then
    continue
  fi

  # Extract stats
  TOTAL=$(jq -r ".contents[\"$key\"].stats.numberOfRequests.total // 0" "$STATS_FILE")
  OK=$(jq -r ".contents[\"$key\"].stats.numberOfRequests.ok // 0" "$STATS_FILE")
  KO=$(jq -r ".contents[\"$key\"].stats.numberOfRequests.ko // 0" "$STATS_FILE")
  MEAN=$(jq -r ".contents[\"$key\"].stats.meanResponseTime.total // 0" "$STATS_FILE")
  MIN_RT=$(jq -r ".contents[\"$key\"].stats.minResponseTime.total // 0" "$STATS_FILE")
  MAX_RT=$(jq -r ".contents[\"$key\"].stats.maxResponseTime.total // 0" "$STATS_FILE")
  P50=$(jq -r ".contents[\"$key\"].stats.percentiles1.total // 0" "$STATS_FILE")
  P75=$(jq -r ".contents[\"$key\"].stats.percentiles2.total // 0" "$STATS_FILE")
  P95=$(jq -r ".contents[\"$key\"].stats.percentiles3.total // 0" "$STATS_FILE")
  P99=$(jq -r ".contents[\"$key\"].stats.percentiles4.total // 0" "$STATS_FILE")
  RPS=$(jq -r ".contents[\"$key\"].stats.meanNumberOfRequestsPerSecond.total // 0" "$STATS_FILE")

  # Calculate error rate
  if [[ "$TOTAL" -gt 0 ]]; then
    ERROR_RATE=$(echo "scale=2; $KO * 100 / $TOTAL" | bc)
  else
    ERROR_RATE="0"
  fi

  # Determine status
  if [[ "$KO" -eq 0 ]]; then
    STATUS="passed"
    STATUS_MSG=""
  elif (( $(echo "$ERROR_RATE > 5" | bc -l) )); then
    STATUS="failed"
    STATUS_MSG="Error rate ${ERROR_RATE}% exceeds 5% threshold ($KO of $TOTAL requests failed)"
  else
    STATUS="broken"
    STATUS_MSG="Error rate ${ERROR_RATE}% ($KO of $TOTAL requests failed)"
  fi

  # Generate UUID
  UUID=$(uuidgen | tr '[:upper:]' '[:lower:]' 2>/dev/null || cat /proc/sys/kernel/random/uuid 2>/dev/null || echo "$(date +%s)-$RANDOM")

  # SLA lookup for feature label
  FEATURE="Throughput"
  case "$REQ_NAME" in
    *[Cc]reate*)  FEATURE="Endorsement Creation" ;;
    *[Gg]et*[Ee]ndorsement*|*[Gg]et*[Cc]overage*) FEATURE="Endorsement Retrieval" ;;
    *[Ll]ist*)    FEATURE="Endorsement Listing" ;;
    *[Ss]ubmit*)  FEATURE="Insurer Submission" ;;
    *[Cc]onfirm*) FEATURE="Endorsement Confirmation" ;;
    *[Rr]eject*)  FEATURE="Endorsement Rejection" ;;
    *[Bb]alance*|*EA*) FEATURE="EA Account" ;;
  esac

  # Build Allure result JSON
  cat > "$ALLURE_OUT/${UUID}-result.json" <<RESULT_EOF
{
  "uuid": "$UUID",
  "historyId": "$(echo -n "$SIM_NAME-$REQ_NAME" | md5sum 2>/dev/null | cut -d' ' -f1 || echo "$SIM_NAME-$key")",
  "name": "$REQ_NAME",
  "fullName": "com.plum.endorsements.perf.simulations.${SIM_NAME}#${REQ_NAME}",
  "status": "$STATUS",
  "statusDetails": $(if [[ -n "$STATUS_MSG" ]]; then echo "{\"message\": \"$STATUS_MSG\"}"; else echo "{}"; fi),
  "stage": "finished",
  "start": $NOW_MS,
  "stop": $((NOW_MS + MEAN)),
  "labels": [
    { "name": "suite", "value": "$SIM_NAME" },
    { "name": "subSuite", "value": "$REQ_NAME" },
    { "name": "feature", "value": "$FEATURE" },
    { "name": "severity", "value": "normal" },
    { "name": "framework", "value": "gatling" },
    { "name": "language", "value": "scala" }
  ],
  "parameters": [
    { "name": "Total Requests", "value": "$TOTAL" },
    { "name": "Successful", "value": "$OK" },
    { "name": "Failed", "value": "$KO" },
    { "name": "Error Rate", "value": "${ERROR_RATE}%" },
    { "name": "Throughput", "value": "${RPS} req/s" },
    { "name": "Min", "value": "${MIN_RT} ms" },
    { "name": "Mean", "value": "${MEAN} ms" },
    { "name": "p50", "value": "${P50} ms" },
    { "name": "p75", "value": "${P75} ms" },
    { "name": "p95", "value": "${P95} ms" },
    { "name": "p99", "value": "${P99} ms" },
    { "name": "Max", "value": "${MAX_RT} ms" }
  ],
  "steps": [
    {
      "name": "Request volume: $TOTAL total ($OK ok, $KO ko)",
      "status": "$STATUS",
      "start": $NOW_MS,
      "stop": $((NOW_MS + 1))
    },
    {
      "name": "Response times: min=${MIN_RT}ms, mean=${MEAN}ms, p95=${P95}ms, max=${MAX_RT}ms",
      "status": $(if [[ "$P95" -gt 2000 ]]; then echo "\"failed\""; else echo "\"passed\""; fi),
      "start": $((NOW_MS + 1)),
      "stop": $((NOW_MS + 2))
    },
    {
      "name": "Throughput: ${RPS} req/s, error rate: ${ERROR_RATE}%",
      "status": "$STATUS",
      "start": $((NOW_MS + 2)),
      "stop": $((NOW_MS + 3))
    }
  ]
}
RESULT_EOF

done

# ---- Generate global summary result ----
GLOBAL_TOTAL=$(jq -r '.stats.numberOfRequests.total // 0' "$STATS_FILE")
GLOBAL_OK=$(jq -r '.stats.numberOfRequests.ok // 0' "$STATS_FILE")
GLOBAL_KO=$(jq -r '.stats.numberOfRequests.ko // 0' "$STATS_FILE")
GLOBAL_MEAN=$(jq -r '.stats.meanResponseTime.total // 0' "$STATS_FILE")
GLOBAL_P50=$(jq -r '.stats.percentiles1.total // 0' "$STATS_FILE")
GLOBAL_P95=$(jq -r '.stats.percentiles3.total // 0' "$STATS_FILE")
GLOBAL_P99=$(jq -r '.stats.percentiles4.total // 0' "$STATS_FILE")
GLOBAL_MAX=$(jq -r '.stats.maxResponseTime.total // 0' "$STATS_FILE")
GLOBAL_RPS=$(jq -r '.stats.meanNumberOfRequestsPerSecond.total // 0' "$STATS_FILE")

if [[ "$GLOBAL_TOTAL" -gt 0 ]]; then
  GLOBAL_ERR=$(echo "scale=2; $GLOBAL_KO * 100 / $GLOBAL_TOTAL" | bc)
else
  GLOBAL_ERR="0"
fi

if [[ "$GLOBAL_KO" -eq 0 ]]; then
  GLOBAL_STATUS="passed"
  GLOBAL_MSG=""
elif (( $(echo "$GLOBAL_ERR > 5" | bc -l) )); then
  GLOBAL_STATUS="failed"
  GLOBAL_MSG="Overall error rate ${GLOBAL_ERR}% ($GLOBAL_KO of $GLOBAL_TOTAL requests failed)"
else
  GLOBAL_STATUS="broken"
  GLOBAL_MSG="Overall error rate ${GLOBAL_ERR}% ($GLOBAL_KO of $GLOBAL_TOTAL requests failed)"
fi

GLOBAL_UUID=$(uuidgen | tr '[:upper:]' '[:lower:]' 2>/dev/null || echo "global-$(date +%s)")

cat > "$ALLURE_OUT/${GLOBAL_UUID}-result.json" <<GLOBAL_EOF
{
  "uuid": "$GLOBAL_UUID",
  "historyId": "$(echo -n "$SIM_NAME-global-summary" | md5sum 2>/dev/null | cut -d' ' -f1 || echo "$SIM_NAME-global")",
  "name": "Simulation Summary: $SIM_NAME",
  "fullName": "com.plum.endorsements.perf.simulations.${SIM_NAME}#Summary",
  "status": "$GLOBAL_STATUS",
  "statusDetails": $(if [[ -n "$GLOBAL_MSG" ]]; then echo "{\"message\": \"$GLOBAL_MSG\"}"; else echo "{}"; fi),
  "stage": "finished",
  "start": $NOW_MS,
  "stop": $((NOW_MS + GLOBAL_MEAN)),
  "labels": [
    { "name": "suite", "value": "$SIM_NAME" },
    { "name": "subSuite", "value": "Summary" },
    { "name": "feature", "value": "Overall Performance" },
    { "name": "severity", "value": "critical" },
    { "name": "framework", "value": "gatling" },
    { "name": "language", "value": "scala" }
  ],
  "parameters": [
    { "name": "Total Requests", "value": "$GLOBAL_TOTAL" },
    { "name": "Successful", "value": "$GLOBAL_OK" },
    { "name": "Failed", "value": "$GLOBAL_KO" },
    { "name": "Error Rate", "value": "${GLOBAL_ERR}%" },
    { "name": "Throughput", "value": "${GLOBAL_RPS} req/s" },
    { "name": "Mean", "value": "${GLOBAL_MEAN} ms" },
    { "name": "p50", "value": "${GLOBAL_P50} ms" },
    { "name": "p95", "value": "${GLOBAL_P95} ms" },
    { "name": "p99", "value": "${GLOBAL_P99} ms" },
    { "name": "Max", "value": "${GLOBAL_MAX} ms" }
  ],
  "steps": [
    {
      "name": "Total: $GLOBAL_TOTAL requests ($GLOBAL_OK passed, $GLOBAL_KO failed)",
      "status": "$GLOBAL_STATUS",
      "start": $NOW_MS,
      "stop": $((NOW_MS + 1))
    },
    {
      "name": "Response: mean=${GLOBAL_MEAN}ms, p50=${GLOBAL_P50}ms, p95=${GLOBAL_P95}ms, p99=${GLOBAL_P99}ms, max=${GLOBAL_MAX}ms",
      "status": $(if [[ "$GLOBAL_P95" -gt 2000 ]]; then echo "\"failed\""; else echo "\"passed\""; fi),
      "start": $((NOW_MS + 1)),
      "stop": $((NOW_MS + 2))
    },
    {
      "name": "Throughput: ${GLOBAL_RPS} req/s | Error rate: ${GLOBAL_ERR}%",
      "status": "$GLOBAL_STATUS",
      "start": $((NOW_MS + 2)),
      "stop": $((NOW_MS + 3))
    }
  ]
}
GLOBAL_EOF

RESULT_COUNT=$(find "$ALLURE_OUT" -name "*-result.json" | wc -l | tr -d ' ')
echo "Generated $RESULT_COUNT Allure result files in $ALLURE_OUT"
