#!/usr/bin/env bash
# ============================================================
# Post-process Allure result JSON files to inject proper labels
# for 3-section segregation in the combined Allure report.
#
# Usage:
#   ./scripts/allure-post-process.sh <results-dir> <parent-suite> <epic>
# ============================================================

set -euo pipefail

RESULTS_DIR="$1"
PARENT_SUITE="$2"
EPIC="$3"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JQ_FILTER="$SCRIPT_DIR/allure-label-filter.jq"

if [[ ! -d "$RESULTS_DIR" ]]; then
  echo "Directory not found: $RESULTS_DIR"
  exit 1
fi

if [[ ! -f "$JQ_FILTER" ]]; then
  echo "jq filter not found: $JQ_FILTER"
  exit 1
fi

count=0
for f in "$RESULTS_DIR"/*-result.json; do
  [[ -f "$f" ]] || continue
  jq --arg ps "$PARENT_SUITE" --arg epic "$EPIC" -f "$JQ_FILTER" "$f" > "${f}.tmp" && mv "${f}.tmp" "$f"
  count=$((count + 1))
done

echo "Processed $count result files → parentSuite=\"$PARENT_SUITE\", epic=\"$EPIC\""
