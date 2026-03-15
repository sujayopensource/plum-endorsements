#!/usr/bin/env bash
# ============================================================
# Plum Endorsement Service — Performance Test Runner (Gatling)
# ============================================================
# Usage:
#   ./run-perf-tests.sh                          Run BaselineSimulation (default)
#   ./run-perf-tests.sh LoadSimulation            Run specific simulation
#   ./run-perf-tests.sh StressSimulation --rps 50 Run with custom target RPS
#   ./run-perf-tests.sh --list                    List available simulations
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
SIMULATION_PKG="com.plum.endorsements.perf.simulations"

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[  OK]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; }

banner() {
  echo ""
  echo -e "${CYAN}${BOLD}"
  echo "  ╔══════════════════════════════════════════════╗"
  echo "  ║   Plum Endorsement — Performance Tests       ║"
  echo "  ║                  (Gatling)                    ║"
  echo "  ╚══════════════════════════════════════════════╝"
  echo -e "${NC}"
}

AVAILABLE_SIMS=(
  "BaselineSimulation"
  "LoadSimulation"
  "StressSimulation"
  "SoakSimulation"
  "SpikeSimulation"
  "EAContentionSimulation"
  "FullLifecycleSimulation"
  "MixedWorkloadSimulation"
)

# ---- Parse args ----
SIMULATION="${1:-BaselineSimulation}"
shift || true

BASE_URL="${BASE_URL:-http://localhost:8080}"
DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/endorsements}"
DB_USER="${DB_USER:-plum}"
DB_PASSWORD="${DB_PASSWORD:-plum_dev}"
DURATION_MINUTES="${DURATION_MINUTES:-15}"
TARGET_RPS="${TARGET_RPS:-20}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --rps)       TARGET_RPS="$2"; shift 2 ;;
    --duration)  DURATION_MINUTES="$2"; shift 2 ;;
    --url)       BASE_URL="$2"; shift 2 ;;
    *)           shift ;;
  esac
done

# ---- List mode ----
if [[ "$SIMULATION" == "--list" ]]; then
  banner
  echo -e "  ${BOLD}Available Simulations:${NC}"
  echo ""
  for sim in "${AVAILABLE_SIMS[@]}"; do
    echo -e "    ${GREEN}•${NC} $sim"
  done
  echo ""
  echo -e "  ${BOLD}Usage:${NC} ./run-perf-tests.sh <SimulationName>"
  echo ""
  exit 0
fi

banner

info "Simulation:  $SIMULATION"
info "Base URL:    $BASE_URL"
info "Target RPS:  $TARGET_RPS"
info "Duration:    ${DURATION_MINUTES} min"
echo ""

# ---- Verify application is reachable ----
info "Checking application health..."
if curl -s --connect-timeout 5 "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
  ok "Application is reachable at $BASE_URL"
else
  warn "Application may not be running at $BASE_URL"
  warn "Proceeding anyway — Gatling will report connection errors."
fi
echo ""

# ---- Run Gatling simulation ----
FULL_SIM="${SIMULATION_PKG}.${SIMULATION}"
info "Running: ${FULL_SIM}"
echo ""

cd "$PROJECT_ROOT"

TEST_EXIT=0
./gradlew :performance-tests:gatlingRunSimulation \
  -Dgatling.simulation="${FULL_SIM}" \
  -DbaseUrl="$BASE_URL" \
  -DdbUrl="$DB_URL" \
  -DdbUser="$DB_USER" \
  -DdbPassword="$DB_PASSWORD" \
  -DdurationMinutes="$DURATION_MINUTES" \
  -DtargetRps="$TARGET_RPS" \
  --console=plain 2>&1 || TEST_EXIT=$?

echo ""

# ---- Report location ----
REPORT_DIR="$PROJECT_ROOT/performance-tests/build/reports/gatling"

if [[ -d "$REPORT_DIR" ]]; then
  LATEST_REPORT=$(ls -td "$REPORT_DIR"/*/ 2>/dev/null | head -1)
  if [[ -n "$LATEST_REPORT" ]]; then
    echo -e "${GREEN}${BOLD}"
    echo "  ╔══════════════════════════════════════════════╗"
    echo "  ║          Gatling Report Ready!                ║"
    echo "  ╚══════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo -e "  ${BOLD}Report:${NC} ${LATEST_REPORT}index.html"
    echo ""

    # ---- Convert Gatling results to Allure format ----
    CONVERTER="$PROJECT_ROOT/scripts/gatling-to-allure.sh"
    if [[ -f "$CONVERTER" ]] && command -v jq &>/dev/null; then
      info "Converting Gatling results to Allure format..."
      bash "$CONVERTER" "$LATEST_REPORT" && ok "Allure results generated" || warn "Allure conversion failed"
    fi

    # Open in browser on macOS
    if command -v open &>/dev/null; then
      info "Opening report in browser..."
      open "${LATEST_REPORT}index.html"
    fi
  fi
fi

if [[ "$TEST_EXIT" -ne 0 ]]; then
  fail "Simulation completed with errors or assertion failures (exit code: $TEST_EXIT)"
  exit 1
else
  ok "Simulation completed successfully!"
fi
