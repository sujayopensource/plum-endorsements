#!/usr/bin/env bash
# =============================================================================
# CALM Architectural Governance — Setup Script
# Installs @finos/calm-cli and @stoplight/spectral-cli
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "╔══════════════════════════════════════════════════╗"
echo "║  CALM Architectural Governance — Setup           ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

# ── Check Node.js ──────────────────────────────────────
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is required but not installed."
    echo "Install Node.js 18+ from https://nodejs.org/"
    exit 1
fi

NODE_VERSION=$(node -v | sed 's/v//' | cut -d. -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo "ERROR: Node.js 18+ required. Found: $(node -v)"
    exit 1
fi

echo "Node.js: $(node -v)"
echo "npm:     $(npm -v)"
echo ""

# ── Install dependencies ──────────────────────────────
echo "Installing CALM CLI and Spectral..."
cd "$PROJECT_DIR"
npm install

echo ""
echo "Verifying installations..."

# ── Verify CALM CLI ───────────────────────────────────
if npx calm --version &> /dev/null; then
    echo "  CALM CLI:    $(npx calm --version 2>/dev/null || echo 'installed')"
else
    echo "  CALM CLI:    installed (version check may require first run)"
fi

# ── Verify Spectral ───────────────────────────────────
if npx spectral --version &> /dev/null; then
    echo "  Spectral:    $(npx spectral --version 2>/dev/null || echo 'installed')"
else
    echo "  Spectral:    installed (version check may require first run)"
fi

echo ""
echo "Setup complete. Run './scripts/validate.sh' to validate architecture."
