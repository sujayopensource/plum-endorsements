#!/usr/bin/env bash
# ============================================================
# Plum Endorsement Service — Kubernetes Local Dev Launcher
# ============================================================
# Usage:  ./k8s-start.sh          # Start all services
#         ./k8s-start.sh stop     # Stop all services
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
NAMESPACE="plum"

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[  OK]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }

banner() {
  echo ""
  echo -e "${CYAN}${BOLD}"
  echo "  ╔══════════════════════════════════════════════╗"
  echo "  ║     Plum Endorsement Management System       ║"
  echo "  ║        Kubernetes Local Dev Launcher          ║"
  echo "  ╚══════════════════════════════════════════════╝"
  echo -e "${NC}"
}

# ---- Stop mode ----
if [[ "${1:-}" == "stop" ]]; then
  info "Stopping all services..."

  # Kill port-forward processes
  if [[ -f "$LOG_DIR/k8s-port-forward.pid" ]]; then
    while IFS= read -r PID; do
      if kill -0 "$PID" 2>/dev/null; then
        kill "$PID" 2>/dev/null && ok "Port-forward stopped (PID $PID)"
      fi
    done < "$LOG_DIR/k8s-port-forward.pid"
    rm -f "$LOG_DIR/k8s-port-forward.pid"
  fi

  # Kill frontend
  if [[ -f "$LOG_DIR/frontend-k8s.pid" ]]; then
    FPID=$(cat "$LOG_DIR/frontend-k8s.pid")
    if kill -0 "$FPID" 2>/dev/null; then
      kill "$FPID" 2>/dev/null && ok "Frontend stopped (PID $FPID)"
    fi
    rm -f "$LOG_DIR/frontend-k8s.pid"
  else
    FPID=$(lsof -ti :5173 2>/dev/null || true)
    if [[ -n "$FPID" ]]; then
      kill $FPID 2>/dev/null && ok "Frontend stopped (PID $FPID)"
    fi
  fi

  # Delete K8s namespace (removes all resources)
  if kubectl get namespace "$NAMESPACE" &>/dev/null; then
    info "Deleting namespace $NAMESPACE (this removes all K8s resources)..."
    kubectl delete namespace "$NAMESPACE" --timeout=60s
    ok "Namespace $NAMESPACE deleted"
  else
    warn "Namespace $NAMESPACE does not exist"
  fi

  echo ""
  ok "All services stopped."
  exit 0
fi

# ---- Main flow ----
banner

# ---- Step 1: Check prerequisites ----
info "Checking prerequisites..."

# kubectl
if command -v kubectl &>/dev/null; then
  ok "kubectl found ($(kubectl version --client -o json 2>/dev/null | grep -o '"gitVersion":"[^"]*"' | head -1 || echo 'version unknown'))"
else
  fail "kubectl not found. Install via: brew install kubectl"
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

# Node.js
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

# Cluster reachable
if kubectl cluster-info &>/dev/null; then
  ok "Kubernetes cluster is reachable"
else
  fail "Cannot reach Kubernetes cluster. Enable Kubernetes in Docker Desktop or start minikube."
fi

echo ""

# ---- Step 2: Detect environment (Docker Desktop vs minikube) ----
K8S_ENV="docker-desktop"
if kubectl config current-context 2>/dev/null | grep -q "minikube"; then
  K8S_ENV="minikube"
fi
info "Detected Kubernetes environment: ${BOLD}$K8S_ENV${NC}"

echo ""

# ---- Step 3: Build backend image ----
info "Building Spring Boot backend..."
cd "$PROJECT_ROOT"
./gradlew bootJar -x test --console=plain -q
ok "Backend JAR built"

info "Building Docker image: plum/endorsement-service:latest"
docker build -t plum/endorsement-service:latest .
ok "Docker image built"

if [[ "$K8S_ENV" == "minikube" ]]; then
  info "Loading image into minikube..."
  minikube image load plum/endorsement-service:latest
  ok "Image loaded into minikube"
fi

echo ""

# ---- Step 4: Apply namespace ----
info "Creating namespace $NAMESPACE..."
kubectl apply -f "$PROJECT_ROOT/k8s/namespace.yaml"
ok "Namespace $NAMESPACE ready"

echo ""

# ---- Step 5: Deploy infrastructure ----
info "Deploying infrastructure (PostgreSQL, Redis, Kafka, Jaeger, ELK, Prometheus, Grafana)..."
kubectl apply \
  -f "$PROJECT_ROOT/k8s/postgres/" \
  -f "$PROJECT_ROOT/k8s/redis/" \
  -f "$PROJECT_ROOT/k8s/kafka/" \
  -f "$PROJECT_ROOT/k8s/jaeger/" \
  -f "$PROJECT_ROOT/k8s/elasticsearch/" \
  -f "$PROJECT_ROOT/k8s/logstash/" \
  -f "$PROJECT_ROOT/k8s/kibana/" \
  -f "$PROJECT_ROOT/k8s/prometheus/" \
  -f "$PROJECT_ROOT/k8s/grafana/"
ok "Infrastructure manifests applied"

info "Waiting for infrastructure pods to be ready..."
kubectl wait --for=condition=ready pod \
  -l "app in (postgres,redis,kafka,jaeger,elasticsearch,logstash,kibana,prometheus,grafana)" \
  -n "$NAMESPACE" \
  --timeout=120s
ok "All infrastructure pods are ready"

echo ""

# ---- Step 6: Deploy backend ----
info "Deploying backend..."
kubectl apply -f "$PROJECT_ROOT/k8s/backend/"
ok "Backend manifests applied"

info "Waiting for backend pod to be ready (this may take a minute)..."
kubectl wait --for=condition=ready pod \
  -l app=backend \
  -n "$NAMESPACE" \
  --timeout=180s
ok "Backend pod is ready"

echo ""

# ---- Step 7: Run seed job ----
info "Running seed data job..."
# Delete previous job if it exists (jobs are immutable)
kubectl delete job seed-data -n "$NAMESPACE" 2>/dev/null || true
kubectl apply -f "$PROJECT_ROOT/k8s/seed-job.yaml"

info "Waiting for seed job to complete..."
kubectl wait --for=condition=complete job/seed-data \
  -n "$NAMESPACE" \
  --timeout=120s
ok "Seed data inserted"

echo ""

# ---- Step 8: Port-forward (background) ----
info "Setting up port-forwarding..."
mkdir -p "$LOG_DIR"
: > "$LOG_DIR/k8s-port-forward.pid"

# Backend: 8080
kubectl port-forward -n "$NAMESPACE" svc/backend 8080:8080 > "$LOG_DIR/k8s-pf-backend.log" 2>&1 &
echo $! >> "$LOG_DIR/k8s-port-forward.pid"
ok "Backend port-forwarded to localhost:8080"

# Jaeger: 16686
kubectl port-forward -n "$NAMESPACE" svc/jaeger 16686:16686 > "$LOG_DIR/k8s-pf-jaeger.log" 2>&1 &
echo $! >> "$LOG_DIR/k8s-port-forward.pid"
ok "Jaeger port-forwarded to localhost:16686"

# Kibana: 5601
kubectl port-forward -n "$NAMESPACE" svc/kibana 5601:5601 > "$LOG_DIR/k8s-pf-kibana.log" 2>&1 &
echo $! >> "$LOG_DIR/k8s-port-forward.pid"
ok "Kibana port-forwarded to localhost:5601"

# Prometheus: 9090
kubectl port-forward -n "$NAMESPACE" svc/prometheus 9090:9090 > "$LOG_DIR/k8s-pf-prometheus.log" 2>&1 &
echo $! >> "$LOG_DIR/k8s-port-forward.pid"
ok "Prometheus port-forwarded to localhost:9090"

# Grafana: 3000
kubectl port-forward -n "$NAMESPACE" svc/grafana 3000:3000 > "$LOG_DIR/k8s-pf-grafana.log" 2>&1 &
echo $! >> "$LOG_DIR/k8s-port-forward.pid"
ok "Grafana port-forwarded to localhost:3000"

echo ""

# ---- Step 9: Install & start frontend ----
info "Installing frontend dependencies..."
cd "$PROJECT_ROOT/frontend"
npm install --silent 2>&1 | tail -1
ok "Frontend dependencies installed"

info "Starting frontend dev server..."
npm run dev > "$LOG_DIR/frontend-k8s.log" 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > "$LOG_DIR/frontend-k8s.pid"

# Wait for frontend
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
  warn "Frontend may still be starting. Check logs: $LOG_DIR/frontend-k8s.log"
else
  ok "Frontend is running (PID $FRONTEND_PID)"
fi

echo ""

# ---- Done ----
echo -e "${GREEN}${BOLD}"
echo "  ╔══════════════════════════════════════════════╗"
echo "  ║       All services are running on K8s!       ║"
echo "  ╚══════════════════════════════════════════════╝"
echo -e "${NC}"
echo -e "  ${BOLD}Frontend:${NC}       http://localhost:5173"
echo -e "  ${BOLD}Backend API:${NC}    http://localhost:8080/api/v1"
echo -e "  ${BOLD}Swagger UI:${NC}     http://localhost:8080/swagger-ui"
echo -e "  ${BOLD}Jaeger:${NC}         http://localhost:16686"
echo -e "  ${BOLD}Kibana:${NC}         http://localhost:5601"
echo -e "  ${BOLD}Prometheus:${NC}     http://localhost:9090"
echo -e "  ${BOLD}Grafana:${NC}        http://localhost:3000  ${CYAN}(admin/plum)${NC}"
echo ""
echo -e "  ${BOLD}Demo Data:${NC}"
echo -e "    Employer ID:  ${CYAN}11111111-1111-1111-1111-111111111111${NC}"
echo -e "    Insurer ID:   ${CYAN}22222222-2222-2222-2222-222222222222${NC}"
echo ""
echo -e "  ${BOLD}K8s Commands:${NC}"
echo -e "    Pods:         kubectl get pods -n $NAMESPACE"
echo -e "    Logs:         kubectl logs -f deploy/backend -n $NAMESPACE"
echo -e "    Dashboard:    kubectl get all -n $NAMESPACE"
echo ""
echo -e "  ${BOLD}Logs:${NC}           $LOG_DIR/"
echo -e "  ${BOLD}Stop all:${NC}       ./k8s-start.sh stop"
echo ""
