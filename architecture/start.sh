#!/usr/bin/env bash
#
# Plum Endorsement Architecture — One-Click Visualization
#
# Launches Structurizr Lite in a Docker container to render the C4 model.
# Open http://localhost:8200 in your browser after launch.
#

set -euo pipefail

PORT="${STRUCTURIZR_PORT:-8200}"
CONTAINER_NAME="plum-structurizr"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Pre-flight checks ───────────────────────────────────────────────────────

if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed or not in PATH."
    echo "Install Docker Desktop from https://www.docker.com/products/docker-desktop/"
    exit 1
fi

if ! docker info &> /dev/null 2>&1; then
    echo "Error: Docker daemon is not running. Please start Docker Desktop."
    exit 1
fi

if [ ! -f "${SCRIPT_DIR}/workspace.dsl" ]; then
    echo "Error: workspace.dsl not found in ${SCRIPT_DIR}"
    exit 1
fi

# ── Stop existing container if running ───────────────────────────────────────

if docker ps -q --filter "name=${CONTAINER_NAME}" | grep -q .; then
    echo "Stopping existing ${CONTAINER_NAME} container..."
    docker stop "${CONTAINER_NAME}" > /dev/null 2>&1
fi

if docker ps -aq --filter "name=${CONTAINER_NAME}" | grep -q .; then
    docker rm "${CONTAINER_NAME}" > /dev/null 2>&1
fi

# ── Launch Structurizr Lite ──────────────────────────────────────────────────

echo ""
echo "  Plum Endorsement Architecture Viewer"
echo "  ====================================="
echo ""
echo "  Starting Structurizr Lite on port ${PORT}..."
echo ""

docker run -d \
    --name "${CONTAINER_NAME}" \
    -p "${PORT}:8080" \
    -v "${SCRIPT_DIR}:/usr/local/structurizr" \
    structurizr/lite

echo ""
echo "  Structurizr Lite is starting up..."
echo ""
echo "  Open in your browser:"
echo ""
echo "    http://localhost:${PORT}"
echo ""
echo "  Available views:"
echo "    L1  System Context        — who uses the system"
echo "    L2  Container Diagram     — deployable units & protocols"
echo "    L3  Component Diagram     — hexagonal architecture internals"
echo "    L3  Filtered: API, Domain, Intelligence, Insurer Adapters"
echo "    Dyn Create Endorsement    — end-to-end creation flow"
echo "    Dyn Insurer Submission    — real-time submission path"
echo "    Dyn Batch Assembly        — scheduled batch processing"
echo "    Dyn Anomaly Detection     — GenAI augmented detection"
echo "    Dev Docker Compose        — development deployment"
echo "    Prod Kubernetes           — production deployment"
echo ""
echo "  To stop:  docker stop ${CONTAINER_NAME}"
echo "  To logs:  docker logs -f ${CONTAINER_NAME}"
echo ""
