#!/usr/bin/env bash
k# ============================================================
# Plum Endorsement Service — Run All Tests with Combined Allure
# ============================================================
# Usage:
#   ./run-all-tests.sh              Run API + BDD + E2E + Perf tests, combined Allure report
#   ./run-all-tests.sh --report     Regenerate combined report (skip tests)
#   ./run-all-tests.sh --stop       Stop the Allure server container
#   ./run-all-tests.sh --skip-perf  Run API + BDD + E2E only (skip performance tests)
# ============================================================

set -euo pipefail

# ---- Colors ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
API_RESULTS="$PROJECT_ROOT/api-tests/build/allure-results"
BDD_RESULTS="$PROJECT_ROOT/behaviour-tests/build/allure-results"
E2E_RESULTS="$PROJECT_ROOT/e2e-tests/allure-results"
PERF_RESULTS="$PROJECT_ROOT/performance-tests/build/allure-results"
COMBINED_RESULTS="$PROJECT_ROOT/build/allure-results-combined"
ALLURE_CONTAINER="plum-allure-combined-server"
ALLURE_PORT=5050

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[  OK]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; }

banner() {
  echo ""
  echo -e "${CYAN}${BOLD}"
  echo "  ╔══════════════════════════════════════════════╗"
  echo "  ║   Plum Endorsement — Combined Test Runner    ║"
  echo "  ║  API + BDD + E2E + Performance (Gatling)   ║"
  echo "  ╚══════════════════════════════════════════════╝"
  echo -e "${NC}"
}

MODE="${1:-}"

# ---- Stop mode ----
if [[ "$MODE" == "--stop" ]]; then
  info "Stopping Allure server..."
  docker rm -f "$ALLURE_CONTAINER" 2>/dev/null && ok "Allure server stopped" || warn "Allure server was not running"
  exit 0
fi

SKIP_TESTS=false
SKIP_PERF=false
if [[ "$MODE" == "--report" ]]; then
  SKIP_TESTS=true
elif [[ "$MODE" == "--skip-perf" ]]; then
  SKIP_PERF=true
fi

banner

# ---- Detect if Ollama is running (adjusts timeouts) ----
OLLAMA_RUNNING=false
if curl -s --connect-timeout 2 "http://localhost:11434/api/version" > /dev/null 2>&1; then
  OLLAMA_RUNNING=true
  ok "Ollama detected — E2E timeouts increased to 120s, Gatling p95 threshold relaxed to 60s"
else
  info "Ollama not detected — using default timeouts"
fi

# ---- Check Docker is running ----
if ! docker info &>/dev/null; then
  fail "Docker is not running. Start Docker Desktop first."
  exit 1
fi
ok "Docker is running"

# ---- Run tests ----
if [[ "$SKIP_TESTS" == "false" ]]; then
  echo ""
  info "Running API + Behaviour + E2E tests (Testcontainers will start PostgreSQL, Redis, Kafka)..."
  info "This may take a few minutes on first run while images are pulled."
  echo ""

  cd "$PROJECT_ROOT"

  # ---- API Tests ----
  info "━━━ Running API Tests ━━━"
  API_EXIT=0
  ./gradlew :api-tests:test --console=plain 2>&1 | tee /tmp/api-tests-output.log || API_EXIT=$?

  API_RESULTS_LINE=$(grep -E "^\d+ tests completed" /tmp/api-tests-output.log 2>/dev/null || true)
  if [[ -n "$API_RESULTS_LINE" ]]; then
    API_TOTAL=$(echo "$API_RESULTS_LINE" | grep -oE "^[0-9]+" || echo "0")
    API_FAILED=$(echo "$API_RESULTS_LINE" | grep -oE "[0-9]+ failed" | grep -oE "[0-9]+" || echo "0")
    if [[ "$API_FAILED" == "0" || -z "$API_FAILED" ]]; then
      echo -e "  ${GREEN}API Tests: $API_TOTAL passed${NC}"
    else
      echo -e "  ${RED}API Tests: $((API_TOTAL - API_FAILED)) passed, $API_FAILED failed${NC}"
    fi
  fi

  echo ""

  # ---- Behaviour Tests ----
  info "━━━ Running Behaviour Tests (Cucumber BDD) ━━━"
  BDD_EXIT=0
  ./gradlew :behaviour-tests:test --console=plain 2>&1 | tee /tmp/behaviour-tests-output.log || BDD_EXIT=$?

  BDD_RESULTS_LINE=$(grep -E "^\d+ tests completed" /tmp/behaviour-tests-output.log 2>/dev/null || true)
  if [[ -n "$BDD_RESULTS_LINE" ]]; then
    BDD_TOTAL=$(echo "$BDD_RESULTS_LINE" | grep -oE "^[0-9]+" || echo "0")
    BDD_FAILED=$(echo "$BDD_RESULTS_LINE" | grep -oE "[0-9]+ failed" | grep -oE "[0-9]+" || echo "0")
    if [[ "$BDD_FAILED" == "0" || -z "$BDD_FAILED" ]]; then
      echo -e "  ${GREEN}BDD Tests: $BDD_TOTAL passed${NC}"
    else
      echo -e "  ${RED}BDD Tests: $((BDD_TOTAL - BDD_FAILED)) passed, $BDD_FAILED failed${NC}"
    fi
  fi

  echo ""

  # ---- E2E Tests (Playwright + Storybook) ----
  info "━━━ Running E2E Tests (Playwright + Storybook) ━━━"
  E2E_EXIT=0

  if [[ -d "$PROJECT_ROOT/e2e-tests/node_modules" ]]; then
    cd "$PROJECT_ROOT/e2e-tests"
    rm -rf allure-results
    OLLAMA_ENABLED="$OLLAMA_RUNNING" npx playwright test 2>&1 | tee /tmp/e2e-tests-output.log || E2E_EXIT=$?

    E2E_PASSED=$(grep -oE "[0-9]+ passed" /tmp/e2e-tests-output.log | grep -oE "^[0-9]+" || echo "0")
    E2E_FAILED=$(grep -oE "[0-9]+ failed" /tmp/e2e-tests-output.log | grep -oE "^[0-9]+" || echo "0")
    if [[ "$E2E_FAILED" == "0" || -z "$E2E_FAILED" ]]; then
      echo -e "  ${GREEN}E2E Tests: $E2E_PASSED passed${NC}"
    else
      echo -e "  ${RED}E2E Tests: $E2E_PASSED passed, $E2E_FAILED failed${NC}"
    fi
    cd "$PROJECT_ROOT"
  else
    warn "E2E tests not set up. Run: cd e2e-tests && npm install && npx playwright install chromium"
  fi

  echo ""

  # ---- Performance Tests (Gatling Baseline) ----
  PERF_EXIT=0
  if [[ "$SKIP_PERF" == "false" ]]; then
    info "━━━ Running Performance Tests (Gatling Baseline) ━━━"

    # Check if the application is running (required for Gatling)
    if curl -s --connect-timeout 5 "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
      ./gradlew :performance-tests:gatlingRunSimulation \
        -Dgatling.simulation=com.plum.endorsements.perf.simulations.BaselineSimulation \
        -DollamaEnabled="$OLLAMA_RUNNING" \
        --console=plain 2>&1 | tee /tmp/perf-tests-output.log || PERF_EXIT=$?

      # Convert Gatling results to Allure format
      CONVERTER="$PROJECT_ROOT/scripts/gatling-to-allure.sh"
      if [[ -f "$CONVERTER" ]] && command -v jq &>/dev/null; then
        bash "$CONVERTER" && ok "Gatling results converted to Allure format" || warn "Allure conversion failed"
      fi

      if [[ "$PERF_EXIT" -eq 0 ]]; then
        echo -e "  ${GREEN}Performance Tests: Baseline passed${NC}"
      else
        echo -e "  ${RED}Performance Tests: Baseline failed${NC}"
      fi
    else
      warn "Application not running at localhost:8080 — skipping performance tests"
      warn "Start the application first: ./start.sh or docker-compose up"
    fi

    echo ""
  else
    info "Skipping performance tests (--skip-perf)"
    echo ""
  fi

  TEST_EXIT=$((API_EXIT + BDD_EXIT + E2E_EXIT + PERF_EXIT))
else
  info "Skipping tests (--report mode). Using results from last run."
  echo ""
fi

# ---- Merge Allure results with label post-processing ----
info "Merging Allure results from API, Behaviour, E2E, and Performance tests..."
rm -rf "$COMBINED_RESULTS"
mkdir -p "$COMBINED_RESULTS"

POST_PROCESS="$PROJECT_ROOT/scripts/allure-post-process.sh"
JQ_FILTER="$PROJECT_ROOT/scripts/allure-label-filter.jq"

# Check jq is available (needed for label injection)
if ! command -v jq &>/dev/null; then
  warn "jq not found — labels will not be post-processed. Install jq for proper report segregation."
fi

# ---- Copy + Post-process API results ----
if [[ -d "$API_RESULTS" ]] && [[ -n "$(ls -A "$API_RESULTS" 2>/dev/null)" ]]; then
  rsync -a "$API_RESULTS/" "$COMBINED_RESULTS/"
  ok "API test results copied"
else
  warn "No API test results found"
fi

# ---- Copy + Post-process BDD results ----
if [[ -d "$BDD_RESULTS" ]] && [[ -n "$(ls -A "$BDD_RESULTS" 2>/dev/null)" ]]; then
  rsync -a "$BDD_RESULTS/" "$COMBINED_RESULTS/"
  ok "Behaviour test results copied"
else
  warn "No Behaviour test results found"
fi

# ---- Copy + Post-process E2E results ----
if [[ -d "$E2E_RESULTS" ]] && [[ -n "$(ls -A "$E2E_RESULTS" 2>/dev/null)" ]]; then
  rsync -a "$E2E_RESULTS/" "$COMBINED_RESULTS/"
  ok "E2E test results copied"
else
  warn "No E2E test results found"
fi

# ---- Copy Performance results ----
if [[ -d "$PERF_RESULTS" ]] && [[ -n "$(ls -A "$PERF_RESULTS" 2>/dev/null)" ]]; then
  rsync -a "$PERF_RESULTS/" "$COMBINED_RESULTS/"
  ok "Performance test results copied"
else
  warn "No Performance test results found"
fi

if [[ -z "$(ls -A "$COMBINED_RESULTS" 2>/dev/null)" ]]; then
  fail "No test results found. Run tests first: ./run-all-tests.sh"
  exit 1
fi

# ---- Post-process labels for clean 3-section segregation ----
if command -v jq &>/dev/null && [[ -f "$JQ_FILTER" ]]; then
  info "Post-processing Allure labels for report segregation..."

  # Post-process each source's result files individually before they're merged
  # We need to identify which files came from which source. Use a temp approach:
  # re-copy from source and process in-place in the combined dir.
  rm -rf "$COMBINED_RESULTS"
  mkdir -p "$COMBINED_RESULTS"

  # API tests → parentSuite = "API Tests", epic = "Endorsement API"
  if [[ -d "$API_RESULTS" ]] && [[ -n "$(ls -A "$API_RESULTS" 2>/dev/null)" ]]; then
    TEMP_DIR=$(mktemp -d)
    rsync -a "$API_RESULTS/" "$TEMP_DIR/"
    bash "$POST_PROCESS" "$TEMP_DIR" "API Tests" "Endorsement API"
    rsync -a "$TEMP_DIR/" "$COMBINED_RESULTS/"
    rm -rf "$TEMP_DIR"
  fi

  # BDD tests → parentSuite = "BDD Tests", epic = "Endorsement BDD"
  if [[ -d "$BDD_RESULTS" ]] && [[ -n "$(ls -A "$BDD_RESULTS" 2>/dev/null)" ]]; then
    TEMP_DIR=$(mktemp -d)
    rsync -a "$BDD_RESULTS/" "$TEMP_DIR/"
    bash "$POST_PROCESS" "$TEMP_DIR" "BDD Tests" "Endorsement BDD"
    rsync -a "$TEMP_DIR/" "$COMBINED_RESULTS/"
    rm -rf "$TEMP_DIR"
  fi

  # E2E tests → parentSuite = "E2E Tests", epic = "Endorsement E2E"
  if [[ -d "$E2E_RESULTS" ]] && [[ -n "$(ls -A "$E2E_RESULTS" 2>/dev/null)" ]]; then
    TEMP_DIR=$(mktemp -d)
    rsync -a "$E2E_RESULTS/" "$TEMP_DIR/"
    bash "$POST_PROCESS" "$TEMP_DIR" "E2E Tests" "Endorsement E2E"
    rsync -a "$TEMP_DIR/" "$COMBINED_RESULTS/"
    rm -rf "$TEMP_DIR"
  fi

  # Performance tests → parentSuite = "Performance Tests", epic = "Endorsement Performance"
  if [[ -d "$PERF_RESULTS" ]] && [[ -n "$(ls -A "$PERF_RESULTS" 2>/dev/null)" ]]; then
    TEMP_DIR=$(mktemp -d)
    rsync -a "$PERF_RESULTS/" "$TEMP_DIR/"
    bash "$POST_PROCESS" "$TEMP_DIR" "Performance Tests" "Endorsement Performance"
    rsync -a "$TEMP_DIR/" "$COMBINED_RESULTS/"
    rm -rf "$TEMP_DIR"
  fi

  ok "Labels post-processed for 4-section segregation"
else
  warn "Skipping label post-processing (jq or filter file not found)"
fi

# ---- Add categories.json for result classification ----
CATEGORIES_FILE="$PROJECT_ROOT/scripts/allure-categories.json"
if [[ -f "$CATEGORIES_FILE" ]]; then
  cp "$CATEGORIES_FILE" "$COMBINED_RESULTS/categories.json"
fi

# ---- Add environment.properties ----
cat > "$COMBINED_RESULTS/environment.properties" <<ENVEOF
Service=Plum Endorsement Service
Java=$(java -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/' || echo "N/A")
Node=$(node --version 2>/dev/null || echo "N/A")
Spring.Boot=3.4.x
Database=PostgreSQL (Testcontainers)
Cache=Redis (Testcontainers)
Messaging=Kafka (Testcontainers)
Browser=Chromium (Playwright)
Performance.Framework=Gatling 3.10.5
Date=$(date '+%Y-%m-%d %H:%M:%S')
ENVEOF

API_COUNT=$(find "$API_RESULTS" -name "*-result.json" 2>/dev/null | wc -l | tr -d ' ' || echo "0")
BDD_COUNT=$(find "$BDD_RESULTS" -name "*-result.json" 2>/dev/null | wc -l | tr -d ' ' || echo "0")
E2E_COUNT=$(find "$E2E_RESULTS" -name "*-result.json" 2>/dev/null | wc -l | tr -d ' ' || echo "0")
PERF_COUNT=$(find "$PERF_RESULTS" -name "*-result.json" 2>/dev/null | wc -l | tr -d ' ' || echo "0")
TOTAL_COUNT=$(find "$COMBINED_RESULTS" -name "*-result.json" 2>/dev/null | wc -l | tr -d ' ' || echo "0")
info "Combined: ${API_COUNT} API + ${BDD_COUNT} BDD + ${E2E_COUNT} E2E + ${PERF_COUNT} Perf = ${TOTAL_COUNT} total test results"

echo ""

# ---- Launch Allure Docker server ----
info "Starting Allure server via Docker on port $ALLURE_PORT..."

# Stop existing containers (combined + individual)
docker rm -f "$ALLURE_CONTAINER" 2>/dev/null || true
docker rm -f plum-allure-server 2>/dev/null || true
docker rm -f plum-allure-bdd-server 2>/dev/null || true
docker rm -f plum-allure-e2e-server 2>/dev/null || true

docker run -d \
  --name "$ALLURE_CONTAINER" \
  -p "$ALLURE_PORT:5050" \
  -e CHECK_RESULTS_EVERY_SECONDS=NONE \
  -e KEEP_HISTORY=1 \
  -e JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseParallelGC" \
  -v "$COMBINED_RESULTS:/app/allure-results" \
  frankescobar/allure-docker-service:latest \
  > /dev/null

# Wait for Allure report to be generated
info "Waiting for Allure server to generate combined report (this may take a moment)..."
RETRIES=0
MAX_RETRIES=60
while [[ $RETRIES -lt $MAX_RETRIES ]]; do
  REPORT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://localhost:$ALLURE_PORT/allure-docker-service/projects/default/reports/latest/index.html?redirect=false" 2>/dev/null || echo "000")
  if [[ "$REPORT_STATUS" == "200" ]]; then
    break
  fi
  sleep 3
  RETRIES=$((RETRIES + 1))
done

if [[ $RETRIES -ge $MAX_RETRIES ]]; then
  warn "Allure report may still be generating. Check: docker logs $ALLURE_CONTAINER"
else
  ok "Allure combined report is ready"
fi

REPORT_URL="http://localhost:$ALLURE_PORT/allure-docker-service/latest-report"

echo ""
echo -e "${GREEN}${BOLD}"
echo "  ╔══════════════════════════════════════════════╗"
echo "  ║     Combined Allure Report Ready!            ║"
echo "  ╚══════════════════════════════════════════════╝"
echo -e "${NC}"
echo -e "  ${BOLD}Allure Report:${NC}   $REPORT_URL"
echo -e "  ${BOLD}Allure API:${NC}      http://localhost:$ALLURE_PORT/allure-docker-service/swagger"
echo ""
echo -e "  ${BOLD}How to navigate:${NC}"
echo -e "    Suites tab → Group by Parent Suite:"
echo -e "      ${CYAN}API Tests${NC}          — API integration tests (RestAssured)"
echo -e "      ${CYAN}BDD Tests${NC}          — Behaviour tests (Cucumber BDD)"
echo -e "      ${CYAN}E2E Tests${NC}          — E2E tests (Playwright + Storybook)"
echo -e "      ${CYAN}Performance Tests${NC}  — Performance tests (Gatling)"
echo ""
echo -e "    Behaviors tab → Group by Epic:"
echo -e "      ${CYAN}Endorsement API${NC}          — API integration tests"
echo -e "      ${CYAN}Endorsement BDD${NC}          — Behaviour tests"
echo -e "      ${CYAN}Endorsement E2E${NC}          — E2E tests"
echo -e "      ${CYAN}Endorsement Performance${NC}  — Performance tests"
echo ""
echo -e "  ${BOLD}Cucumber HTML:${NC}   behaviour-tests/build/reports/cucumber/cucumber-report.html"
echo ""
echo -e "  ${BOLD}Stop server:${NC}     ./run-all-tests.sh --stop"
echo ""

# Open in browser on macOS
if command -v open &>/dev/null; then
  info "Opening report in browser..."
  open "$REPORT_URL"
fi

if [[ "$SKIP_TESTS" == "false" ]]; then
  echo ""
  if [[ "${TEST_EXIT:-0}" -ne 0 ]]; then
    fail "Some tests failed. Review the Allure report for details."
    exit 1
  else
    ok "All tests passed!"
  fi
fi
