#!/usr/bin/env bash
# ============================================================
# Plum Endorsement Service — API Test Runner with Allure Report
# ============================================================
# Usage:
#   ./run-api-tests.sh              Run tests + generate report via Docker
#   ./run-api-tests.sh --report     Regenerate report from last run (skip tests)
#   ./run-api-tests.sh --stop       Stop the Allure server container
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
ALLURE_RESULTS="$PROJECT_ROOT/api-tests/build/allure-results"
ALLURE_CONTAINER="plum-allure-server"
ALLURE_PORT=5050

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[  OK]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; }

banner() {
  echo ""
  echo -e "${CYAN}${BOLD}"
  echo "  ╔══════════════════════════════════════════════╗"
  echo "  ║     Plum Endorsement — API Test Runner       ║"
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
if [[ "$MODE" == "--report" ]]; then
  SKIP_TESTS=true
fi

banner

# ---- Check Docker is running ----
if ! docker info &>/dev/null; then
  fail "Docker is not running. Start Docker Desktop first."
  exit 1
fi
ok "Docker is running"

# ---- Run tests ----
if [[ "$SKIP_TESTS" == "false" ]]; then
  echo ""
  info "Running API tests (Testcontainers will start PostgreSQL, Redis, Kafka)..."
  info "This may take a few minutes on first run while images are pulled."
  echo ""

  cd "$PROJECT_ROOT"
  TEST_EXIT=0
  ./gradlew :api-tests:test --console=plain 2>&1 | tee /tmp/api-tests-output.log || TEST_EXIT=$?

  echo ""

  # Parse results from Gradle output
  RESULTS=$(grep -E "^\d+ tests completed" /tmp/api-tests-output.log 2>/dev/null || true)
  if [[ -n "$RESULTS" ]]; then
    TOTAL=$(echo "$RESULTS" | grep -oE "^[0-9]+" || echo "0")
    FAILED=$(echo "$RESULTS" | grep -oE "[0-9]+ failed" | grep -oE "[0-9]+" || echo "0")
    PASSED=$((TOTAL - FAILED))

    if [[ "$FAILED" == "0" || -z "$FAILED" ]]; then
      echo -e "  ${GREEN}${BOLD}PASSED: $TOTAL tests${NC}"
    else
      echo -e "  ${GREEN}Passed: $PASSED${NC}  |  ${RED}Failed: $FAILED${NC}  |  Total: $TOTAL"
    fi
  fi

  echo ""
else
  info "Skipping tests (--report mode). Using results from last run."
  echo ""
fi

# ---- Verify results exist ----
if [[ ! -d "$ALLURE_RESULTS" ]] || [[ -z "$(ls -A "$ALLURE_RESULTS" 2>/dev/null)" ]]; then
  fail "No Allure results found at $ALLURE_RESULTS"
  fail "Run tests first: ./run-api-tests.sh"
  exit 1
fi

# ---- Launch Allure Docker server ----
info "Starting Allure server via Docker on port $ALLURE_PORT..."

# Stop existing container if running
docker rm -f "$ALLURE_CONTAINER" 2>/dev/null || true

docker run -d \
  --name "$ALLURE_CONTAINER" \
  -p "$ALLURE_PORT:5050" \
  -e CHECK_RESULTS_EVERY_SECONDS=5 \
  -e KEEP_HISTORY=1 \
  -v "$ALLURE_RESULTS:/app/allure-results" \
  frankescobar/allure-docker-service:latest \
  > /dev/null

# Wait for Allure server to be ready
info "Waiting for Allure server to be ready..."
RETRIES=0
MAX_RETRIES=30
while [[ $RETRIES -lt $MAX_RETRIES ]]; do
  if curl -s "http://localhost:$ALLURE_PORT/allure-docker-service/version" > /dev/null 2>&1; then
    break
  fi
  sleep 2
  RETRIES=$((RETRIES + 1))
done

if [[ $RETRIES -ge $MAX_RETRIES ]]; then
  warn "Allure server may still be starting. Check: docker logs $ALLURE_CONTAINER"
else
  ok "Allure server is running"
fi

# Trigger report generation via API
info "Generating Allure report..."
curl -s -X GET "http://localhost:$ALLURE_PORT/allure-docker-service/generate-report" > /dev/null 2>&1
sleep 2
ok "Allure report generated"

REPORT_URL="http://localhost:$ALLURE_PORT/allure-docker-service/latest-report"

echo ""
echo -e "${GREEN}${BOLD}"
echo "  ╔══════════════════════════════════════════════╗"
echo "  ║          Allure Report Ready!                ║"
echo "  ╚══════════════════════════════════════════════╝"
echo -e "${NC}"
echo -e "  ${BOLD}Allure Report:${NC}  $REPORT_URL"
echo -e "  ${BOLD}Allure API:${NC}     http://localhost:$ALLURE_PORT/allure-docker-service/swagger"
echo ""
echo -e "  ${BOLD}Stop server:${NC}    ./run-api-tests.sh --stop"
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
