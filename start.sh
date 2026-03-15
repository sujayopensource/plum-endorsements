#!/usr/bin/env bash
# ============================================================
# Plum Endorsement Service — One-Click Installer & Launcher
# ============================================================
# Usage:  ./start.sh              Start all services (rule-based intelligence)
#         ./start.sh --ollama     Start all services + Ollama LLM (GenAI intelligence)
#         ./start.sh stop         Stop all services (including Ollama)
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
LOG_DIR="$PROJECT_ROOT/.logs"
OLLAMA_CONTAINER="plum-ollama"
OLLAMA_MODEL="${OLLAMA_MODEL:-llama3.2}"
OLLAMA_PORT=11434
ENABLE_OLLAMA=false

# Parse flags
for arg in "$@"; do
  case "$arg" in
    --ollama) ENABLE_OLLAMA=true ;;
    stop) ;; # handled below
    *) ;;
  esac
done

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[  OK]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }

banner() {
  echo ""
  echo -e "${CYAN}${BOLD}"
  echo "  ╔══════════════════════════════════════════════╗"
  echo "  ║     Plum Endorsement Management System       ║"
  echo "  ║          One-Click Installer                 ║"
  echo "  ╚══════════════════════════════════════════════╝"
  echo -e "${NC}"
}

# ---- Stop mode ----
if [[ "${1:-}" == "stop" ]]; then
  info "Stopping all services..."

  if [[ -f "$LOG_DIR/backend.pid" ]]; then
    BPID=$(cat "$LOG_DIR/backend.pid")
    if kill -0 "$BPID" 2>/dev/null; then
      kill "$BPID" 2>/dev/null && ok "Backend stopped (PID $BPID)"
    fi
    rm -f "$LOG_DIR/backend.pid"
  else
    # Fallback: kill by port
    BPID=$(lsof -ti :8080 2>/dev/null || true)
    if [[ -n "$BPID" ]]; then
      kill $BPID 2>/dev/null && ok "Backend stopped (PID $BPID)"
    fi
  fi

  if [[ -f "$LOG_DIR/frontend.pid" ]]; then
    FPID=$(cat "$LOG_DIR/frontend.pid")
    if kill -0 "$FPID" 2>/dev/null; then
      kill "$FPID" 2>/dev/null && ok "Frontend stopped (PID $FPID)"
    fi
    rm -f "$LOG_DIR/frontend.pid"
  else
    FPID=$(lsof -ti :5173 2>/dev/null || true)
    if [[ -n "$FPID" ]]; then
      kill $FPID 2>/dev/null && ok "Frontend stopped (PID $FPID)"
    fi
  fi

  cd "$PROJECT_ROOT" && docker compose down 2>/dev/null && ok "Docker containers stopped"

  # Stop Ollama container if running
  if docker ps -q -f "name=$OLLAMA_CONTAINER" 2>/dev/null | grep -q .; then
    docker stop "$OLLAMA_CONTAINER" 2>/dev/null && docker rm "$OLLAMA_CONTAINER" 2>/dev/null
    ok "Ollama container stopped"
  fi

  echo ""
  ok "All services stopped."
  exit 0
fi

# ---- Main flow ----
banner

# ---- Step 1: Check prerequisites ----
info "Checking prerequisites..."

# Java 21+
if command -v java &>/dev/null; then
  JAVA_VER_RAW=$(java -version 2>&1 | head -1)
  JAVA_VER=$(echo "$JAVA_VER_RAW" | sed -E 's/.*"([0-9]+)\..*/\1/')
  if [[ "$JAVA_VER" =~ ^[0-9]+$ ]] && [[ "$JAVA_VER" -ge 21 ]]; then
    ok "Java $JAVA_VER found ($JAVA_VER_RAW)"
  else
    fail "Java 21+ required (found: $JAVA_VER_RAW). Install via: brew install openjdk@21"
  fi
else
  fail "Java not found. Install via: brew install openjdk@21"
fi

# Node.js 18+
if command -v node &>/dev/null; then
  NODE_VER=$(node -v | sed 's/v\([0-9]*\).*/\1/')
  if [[ "$NODE_VER" -ge 18 ]]; then
    ok "Node.js v$(node -v | sed 's/v//') found"
  else
    fail "Node.js 18+ required (found v$(node -v)). Install via: brew install node"
  fi
else
  fail "Node.js not found. Install via: brew install node"
fi

# npm
if command -v npm &>/dev/null; then
  ok "npm $(npm -v) found"
else
  fail "npm not found. Install Node.js: brew install node"
fi

# Docker
if command -v docker &>/dev/null; then
  if docker info &>/dev/null; then
    ok "Docker is running"
  else
    fail "Docker is installed but not running. Start Docker Desktop first."
  fi
else
  fail "Docker not found. Install Docker Desktop: https://www.docker.com/products/docker-desktop"
fi

# docker compose
if docker compose version &>/dev/null; then
  ok "Docker Compose found"
else
  fail "Docker Compose not found. Update Docker Desktop or install docker-compose-plugin."
fi

echo ""

# ---- Step 2: Start infrastructure containers ----
info "Starting infrastructure containers (PostgreSQL, Redis, Kafka, Jaeger, ELK, Prometheus, Grafana)..."
cd "$PROJECT_ROOT"
docker compose up -d

info "Waiting for containers to be healthy..."
RETRIES=0
MAX_RETRIES=60
while [[ $RETRIES -lt $MAX_RETRIES ]]; do
  PG_HEALTH=$(docker compose ps postgres --format json 2>/dev/null | grep -o '"Health":"[^"]*"' | head -1 || echo "")
  REDIS_HEALTH=$(docker compose ps redis --format json 2>/dev/null | grep -o '"Health":"[^"]*"' | head -1 || echo "")

  if [[ "$PG_HEALTH" == *"healthy"* ]] && [[ "$REDIS_HEALTH" == *"healthy"* ]]; then
    break
  fi

  sleep 2
  RETRIES=$((RETRIES + 1))
done

if [[ $RETRIES -ge $MAX_RETRIES ]]; then
  warn "Timed out waiting for containers. Continuing anyway..."
else
  ok "All containers are healthy"
fi

# Wait for Elasticsearch to be healthy
info "Waiting for Elasticsearch to be healthy..."
ES_RETRIES=0
ES_MAX_RETRIES=30
while [[ $ES_RETRIES -lt $ES_MAX_RETRIES ]]; do
  ES_STATUS=$(curl -s http://localhost:9200/_cluster/health 2>/dev/null | grep -o '"status":"[^"]*"' | head -1 || echo "")
  if [[ "$ES_STATUS" == *"green"* ]] || [[ "$ES_STATUS" == *"yellow"* ]]; then
    break
  fi
  sleep 3
  ES_RETRIES=$((ES_RETRIES + 1))
done
if [[ $ES_RETRIES -ge $ES_MAX_RETRIES ]]; then
  warn "Elasticsearch may still be starting. Continuing..."
else
  ok "Elasticsearch is healthy"
fi

echo ""

# ---- Step 2b: Start Ollama (if --ollama flag is set) ----
if [[ "$ENABLE_OLLAMA" == "true" ]]; then
  info "Setting up Ollama LLM for GenAI intelligence..."

  # Check if Ollama container is already running
  if docker ps -q -f "name=$OLLAMA_CONTAINER" 2>/dev/null | grep -q .; then
    ok "Ollama container already running"
  else
    # Remove stopped container if it exists
    docker rm "$OLLAMA_CONTAINER" 2>/dev/null || true

    info "Starting Ollama container (port $OLLAMA_PORT)..."
    docker run -d \
      --name "$OLLAMA_CONTAINER" \
      -p "$OLLAMA_PORT:11434" \
      -v ollama-data:/root/.ollama \
      ollama/ollama:latest

    # Wait for Ollama API to be ready
    info "Waiting for Ollama to be ready..."
    OLLAMA_RETRIES=0
    OLLAMA_MAX_RETRIES=30
    while [[ $OLLAMA_RETRIES -lt $OLLAMA_MAX_RETRIES ]]; do
      if curl -s http://localhost:$OLLAMA_PORT/api/tags > /dev/null 2>&1; then
        break
      fi
      sleep 2
      OLLAMA_RETRIES=$((OLLAMA_RETRIES + 1))
    done

    if [[ $OLLAMA_RETRIES -ge $OLLAMA_MAX_RETRIES ]]; then
      warn "Ollama may still be starting. Continuing..."
    else
      ok "Ollama is ready"
    fi
  fi

  # Pull the model if not already present
  info "Checking for model '$OLLAMA_MODEL'..."
  MODEL_EXISTS=$(curl -s http://localhost:$OLLAMA_PORT/api/tags 2>/dev/null | grep -o "\"$OLLAMA_MODEL" || true)
  if [[ -n "$MODEL_EXISTS" ]]; then
    ok "Model '$OLLAMA_MODEL' already available"
  else
    info "Pulling model '$OLLAMA_MODEL' (this may take a few minutes on first run)..."
    docker exec "$OLLAMA_CONTAINER" ollama pull "$OLLAMA_MODEL"
    if [[ $? -eq 0 ]]; then
      ok "Model '$OLLAMA_MODEL' pulled successfully"
    else
      warn "Failed to pull model '$OLLAMA_MODEL'. GenAI features will fall back to rule-based."
    fi
  fi

  echo ""
fi

# ---- Step 3: Build the backend ----
info "Building Spring Boot backend (this may take a minute on first run)..."
cd "$PROJECT_ROOT"
./gradlew build -x test --console=plain -q
ok "Backend built successfully"

echo ""

# ---- Step 4: Install frontend dependencies ----
info "Installing frontend dependencies..."
cd "$PROJECT_ROOT/frontend"
npm install --silent 2>&1 | tail -1
ok "Frontend dependencies installed"

echo ""

# ---- Step 5: Kill any stale backend/frontend processes ----
info "Checking for stale processes on ports 8080 and 5173..."
STALE_8080=$(lsof -ti :8080 2>/dev/null || true)
if [[ -n "$STALE_8080" ]]; then
  kill $STALE_8080 2>/dev/null || true
  ok "Killed stale process on port 8080 (PID $STALE_8080)"
  sleep 1
fi
STALE_5173=$(lsof -ti :5173 2>/dev/null || true)
if [[ -n "$STALE_5173" ]]; then
  kill $STALE_5173 2>/dev/null || true
  ok "Killed stale process on port 5173 (PID $STALE_5173)"
  sleep 1
fi

echo ""

# ---- Step 6: Start backend ----
if [[ "$ENABLE_OLLAMA" == "true" ]]; then
  info "Starting backend on port 8080 (with Ollama/GenAI intelligence)..."
else
  info "Starting backend on port 8080..."
fi
mkdir -p "$LOG_DIR"
cd "$PROJECT_ROOT"
if [[ "$ENABLE_OLLAMA" == "true" ]]; then
  SPRING_PROFILES_ACTIVE=ollama ./gradlew bootRun --console=plain -q > "$LOG_DIR/backend.log" 2>&1 &
else
  ./gradlew bootRun --console=plain -q > "$LOG_DIR/backend.log" 2>&1 &
fi
BACKEND_PID=$!
echo "$BACKEND_PID" > "$LOG_DIR/backend.pid"

# Wait for backend to be ready (Flyway migrations run during startup)
RETRIES=0
MAX_RETRIES=60
while [[ $RETRIES -lt $MAX_RETRIES ]]; do
  if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    break
  fi
  sleep 2
  RETRIES=$((RETRIES + 1))
done

if [[ $RETRIES -ge $MAX_RETRIES ]]; then
  warn "Backend may still be starting. Check logs: $LOG_DIR/backend.log"
else
  ok "Backend is running (PID $BACKEND_PID)"
fi

echo ""

# ---- Step 7: Seed demo data (after Flyway migrations) ----
info "Seeding demo EA accounts for multi-insurer setup..."

EMPLOYER_ID="11111111-1111-1111-1111-111111111111"
MOCK_INSURER_ID="22222222-2222-2222-2222-222222222222"
ICICI_INSURER_ID="33333333-3333-3333-3333-333333333333"
NIVA_INSURER_ID="44444444-4444-4444-4444-444444444444"
BAJAJ_INSURER_ID="55555555-5555-5555-5555-555555555555"

docker compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T postgres psql -U plum -d endorsements -q <<SQL
-- Seed EA accounts for demo employer across all insurers
INSERT INTO ea_accounts (employer_id, insurer_id, balance, reserved, updated_at)
VALUES
  ('$EMPLOYER_ID', '$MOCK_INSURER_ID',  500000.00, 0.00, now()),
  ('$EMPLOYER_ID', '$ICICI_INSURER_ID', 500000.00, 0.00, now()),
  ('$EMPLOYER_ID', '$NIVA_INSURER_ID',  500000.00, 0.00, now()),
  ('$EMPLOYER_ID', '$BAJAJ_INSURER_ID', 500000.00, 0.00, now())
ON CONFLICT (employer_id, insurer_id)
DO UPDATE SET balance = GREATEST(ea_accounts.balance, 500000.00), updated_at = now();
SQL

ok "Demo EA accounts seeded (4 insurers)"
echo -e "    Employer ID:          ${CYAN}$EMPLOYER_ID${NC}"
echo -e "    Mock Insurer ID:      ${CYAN}$MOCK_INSURER_ID${NC}"
echo -e "    ICICI Lombard ID:     ${CYAN}$ICICI_INSURER_ID${NC}"
echo -e "    Niva Bupa ID:         ${CYAN}$NIVA_INSURER_ID${NC}"
echo -e "    Bajaj Allianz ID:     ${CYAN}$BAJAJ_INSURER_ID${NC}"

# ---- Phase 3: Verify intelligence schedulers registered ----
info "Verifying Phase 3 intelligence schedulers..."
SCHED_CHECK=$(curl -s http://localhost:8080/actuator/health 2>/dev/null || echo "{}")
if echo "$SCHED_CHECK" | grep -q '"status":"UP"'; then
  ok "Intelligence schedulers registered (anomaly detection, balance forecast, process mining)"
else
  warn "Could not verify intelligence schedulers. Backend may still be initializing."
fi

info "Phase 3 intelligence demo data seeded via Flyway migration V13"

echo ""

# ---- Step 8: Start frontend ----
info "Starting frontend dev server on port 5173..."
cd "$PROJECT_ROOT/frontend"
npm run dev > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > "$LOG_DIR/frontend.pid"

# Wait for frontend to be ready
RETRIES=0
MAX_RETRIES=30
while [[ $RETRIES -lt $MAX_RETRIES ]]; do
  if curl -s http://localhost:5173 > /dev/null 2>&1; then
    break
  fi
  sleep 1
  RETRIES=$((RETRIES + 1))
done

if [[ $RETRIES -ge $MAX_RETRIES ]]; then
  warn "Frontend may still be starting. Check logs: $LOG_DIR/frontend.log"
else
  ok "Frontend is running (PID $FRONTEND_PID)"
fi

echo ""

# ---- Done ----
echo -e "${GREEN}${BOLD}"
echo "  ╔══════════════════════════════════════════════╗"
echo "  ║           All services are running!          ║"
echo "  ╚══════════════════════════════════════════════╝"
echo -e "${NC}"
echo -e "  ${BOLD}Frontend:${NC}       http://localhost:5173"
echo -e "  ${BOLD}Backend API:${NC}    http://localhost:8080/api/v1"
echo -e "  ${BOLD}Swagger UI:${NC}     http://localhost:8080/swagger-ui"
echo -e "  ${BOLD}Intelligence:${NC}   http://localhost:5173/intelligence"
if [[ "$ENABLE_OLLAMA" == "true" ]]; then
echo -e "  ${BOLD}Ollama LLM:${NC}     http://localhost:$OLLAMA_PORT  ${CYAN}(model: $OLLAMA_MODEL)${NC}"
fi
echo -e "  ${BOLD}Jaeger:${NC}         http://localhost:16686"
echo -e "  ${BOLD}Kibana:${NC}         http://localhost:5601"
echo -e "  ${BOLD}Prometheus:${NC}     http://localhost:9090"
echo -e "  ${BOLD}Grafana:${NC}        http://localhost:3000  ${CYAN}(admin/plum)${NC}"
echo ""
echo -e "  ${BOLD}Demo Data:${NC}"
echo -e "    Employer ID:        ${CYAN}11111111-1111-1111-1111-111111111111${NC}"
echo -e "    Mock Insurer:       ${CYAN}22222222-2222-2222-2222-222222222222${NC}"
echo -e "    ICICI Lombard:      ${CYAN}33333333-3333-3333-3333-333333333333${NC}"
echo -e "    Niva Bupa:          ${CYAN}44444444-4444-4444-4444-444444444444${NC}"
echo -e "    Bajaj Allianz:      ${CYAN}55555555-5555-5555-5555-555555555555${NC}"
echo ""
echo -e "  ${BOLD}Logs:${NC}           $LOG_DIR/"
echo -e "  ${BOLD}Stop all:${NC}       ./start.sh stop"
echo ""
if [[ "$ENABLE_OLLAMA" == "true" ]]; then
echo -e "  ${BOLD}Intelligence:${NC}   ${GREEN}Ollama/GenAI mode${NC} (OllamaAugmentedAnomalyDetector, OllamaErrorResolver)"
else
echo -e "  ${YELLOW}Tip:${NC} For GenAI intelligence, use ${BOLD}./start.sh --ollama${NC} (downloads Ollama + $OLLAMA_MODEL model)"
fi
echo -e "  ${YELLOW}Tip:${NC} For Kubernetes local dev, use ${BOLD}./k8s-start.sh${NC} instead."
echo ""
