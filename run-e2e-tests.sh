#!/usr/bin/env bash
# ============================================================
# Plum Endorsement Service — E2E Tests (Playwright + Storybook)
# ============================================================
# Usage:
#   ./run-e2e-tests.sh              Run all E2E + Storybook tests, launch Allure
#   ./run-e2e-tests.sh --e2e        Run only E2E flow tests
#   ./run-e2e-tests.sh --storybook  Run only Storybook component tests
#   ./run-e2e-tests.sh --report     Regenerate Allure report (skip tests)
#   ./run-e2e-tests.sh --stop       Stop the Allure server container
# ============================================================
# Prerequisites:
#   - Docker running (for Allure server)
#   - Backend running on port 8080 (./gradlew bootRun)
#   - Frontend dev server on port 5173 (cd frontend && npm run dev)
#   - Storybook on port 6006 (cd frontend && npm run storybook)
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
E2E_DIR="$PROJECT_ROOT/e2e-tests"
E2E_RESULTS="$E2E_DIR/allure-results"
ALLURE_CONTAINER="plum-allure-e2e-server"
ALLURE_PORT=5051

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[  OK]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; }

banner() {
  echo ""
  echo -e "${CYAN}${BOLD}"
  echo "  ╔══════════════════════════════════════════════╗"
  echo "  ║   Plum Endorsement — E2E Test Runner         ║"
  echo "  ║     Playwright + Storybook Tests              ║"
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
PLAYWRIGHT_PROJECT=""
if [[ "$MODE" == "--report" ]]; then
  SKIP_TESTS=true
elif [[ "$MODE" == "--e2e" ]]; then
  PLAYWRIGHT_PROJECT="--project=e2e"
elif [[ "$MODE" == "--storybook" ]]; then
  PLAYWRIGHT_PROJECT="--project=storybook"
fi

banner

# ---- Detect if Ollama is running (adjusts timeouts) ----
OLLAMA_RUNNING=false
if curl -s --connect-timeout 2 "http://localhost:11434/api/version" > /dev/null 2>&1; then
  OLLAMA_RUNNING=true
  ok "Ollama detected — E2E timeouts increased to 120s"
else
  info "Ollama not detected — using default 30s timeouts"
fi

# ---- Check Docker is running ----
if ! docker info &>/dev/null; then
  fail "Docker is not running. Start Docker Desktop first."
  exit 1
fi
ok "Docker is running"

# ---- Check prerequisites ----
if [[ "$SKIP_TESTS" == "false" ]]; then
  echo ""

  # Check e2e-tests dependencies
  if [[ ! -d "$E2E_DIR/node_modules" ]]; then
    info "Installing E2E test dependencies..."
    cd "$E2E_DIR" && npm install
    ok "Dependencies installed"
  fi

  # Check Playwright browsers
  if ! npx --prefix "$E2E_DIR" playwright --version &>/dev/null; then
    info "Installing Playwright browsers..."
    cd "$E2E_DIR" && npx playwright install chromium
    ok "Playwright browsers installed"
  fi

  # Check backend is running
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null | grep -q "200"; then
    ok "Backend is running on port 8080"
  else
    warn "Backend may not be running on port 8080. E2E tests may fail."
    warn "Start it with: ./gradlew bootRun"
  fi

  # Check frontend is running
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:5173 2>/dev/null | grep -qE "200|304"; then
    ok "Frontend dev server is running on port 5173"
  else
    warn "Frontend dev server may not be running on port 5173."
    warn "Start it with: cd frontend && npm run dev"
  fi

  # Check Storybook is running (if needed)
  if [[ -z "$PLAYWRIGHT_PROJECT" || "$PLAYWRIGHT_PROJECT" == "--project=storybook" ]]; then
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:6006 2>/dev/null | grep -qE "200|304"; then
      ok "Storybook is running on port 6006"
    else
      warn "Storybook may not be running on port 6006."
      warn "Start it with: cd frontend && npm run storybook"
    fi
  fi

  echo ""

  # ---- Run Playwright tests ----
  info "━━━ Running E2E Tests (Playwright) ━━━"
  E2E_EXIT=0

  cd "$E2E_DIR"

  # Clean previous results
  rm -rf allure-results

  OLLAMA_ENABLED="$OLLAMA_RUNNING" npx playwright test $PLAYWRIGHT_PROJECT 2>&1 | tee /tmp/e2e-tests-output.log || E2E_EXIT=$?

  E2E_TOTAL=$(grep -oE "[0-9]+ passed" /tmp/e2e-tests-output.log | grep -oE "^[0-9]+" || echo "0")
  E2E_FAILED=$(grep -oE "[0-9]+ failed" /tmp/e2e-tests-output.log | grep -oE "^[0-9]+" || echo "0")

  if [[ "$E2E_FAILED" == "0" || -z "$E2E_FAILED" ]]; then
    echo -e "  ${GREEN}E2E Tests: $E2E_TOTAL passed${NC}"
  else
    echo -e "  ${RED}E2E Tests: $E2E_TOTAL passed, $E2E_FAILED failed${NC}"
  fi

  echo ""
else
  info "Skipping tests (--report mode). Using results from last run."
  echo ""
fi

# ---- Launch Allure Docker server ----
if [[ ! -d "$E2E_RESULTS" ]] || [[ -z "$(ls -A "$E2E_RESULTS" 2>/dev/null)" ]]; then
  fail "No E2E test results found. Run tests first: ./run-e2e-tests.sh"
  exit 1
fi

RESULT_COUNT=$(find "$E2E_RESULTS" -name "*-result.json" 2>/dev/null | wc -l | tr -d ' ')
info "E2E test results: ${RESULT_COUNT} result files"

echo ""
info "Starting Allure server via Docker on port $ALLURE_PORT..."

# Stop existing container
docker rm -f "$ALLURE_CONTAINER" 2>/dev/null || true

docker run -d \
  --name "$ALLURE_CONTAINER" \
  -p "$ALLURE_PORT:5050" \
  -e CHECK_RESULTS_EVERY_SECONDS=NONE \
  -e KEEP_HISTORY=1 \
  -v "$E2E_RESULTS:/app/allure-results" \
  frankescobar/allure-docker-service:latest \
  > /dev/null

# Wait for Allure report to be generated
info "Waiting for Allure server to generate report..."
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
  ok "Allure E2E report is ready"
fi

REPORT_URL="http://localhost:$ALLURE_PORT/allure-docker-service/latest-report"

echo ""
echo -e "${GREEN}${BOLD}"
echo "  ╔══════════════════════════════════════════════╗"
echo "  ║        E2E Allure Report Ready!              ║"
echo "  ╚══════════════════════════════════════════════╝"
echo -e "${NC}"
echo -e "  ${BOLD}Allure Report:${NC}   $REPORT_URL"
echo -e "  ${BOLD}Allure API:${NC}      http://localhost:$ALLURE_PORT/allure-docker-service/swagger"
echo ""
echo -e "  ${BOLD}How to navigate:${NC}"
echo -e "    Behaviors tab  → Epic: ${CYAN}Endorsement_E2E${NC}"
echo -e "    Suites tab     → By spec file"
echo ""
echo -e "  ${BOLD}Stop server:${NC}     ./run-e2e-tests.sh --stop"
echo ""

# Open in browser on macOS
if command -v open &>/dev/null; then
  info "Opening report in browser..."
  open "$REPORT_URL"
fi

if [[ "$SKIP_TESTS" == "false" ]]; then
  echo ""
  if [[ "${E2E_EXIT:-0}" -ne 0 ]]; then
    fail "Some E2E tests failed. Review the Allure report for details."
    exit 1
  else
    ok "All E2E tests passed!"
  fi
fi
