#!/usr/bin/env bash
#
# build-demo.sh — Master build script for the Plum Endorsement video demo
#
# This script:
#   1. Generates narration audio (if not already done)
#   2. Concatenates audio segments with pauses into a single track
#   3. Runs Playwright to record the browser demo
#   4. Combines video + audio into a final .mp4
#
# Usage:
#   bash build-demo.sh                  # Full build
#   bash build-demo.sh --audio-only     # Generate audio only
#   bash build-demo.sh --video-only     # Record video only (assumes audio exists)
#   bash build-demo.sh --combine-only   # Combine existing video+audio
#   bash build-demo.sh --no-audio       # Record video without audio playback
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AUDIO_DIR="$SCRIPT_DIR/audio"
OUTPUT_DIR="$SCRIPT_DIR/output"
FINAL_VIDEO="$SCRIPT_DIR/plum-endorsement-demo.mp4"

AUDIO_ONLY=false
VIDEO_ONLY=false
COMBINE_ONLY=false
NO_AUDIO=""

for arg in "$@"; do
  case "$arg" in
    --audio-only)   AUDIO_ONLY=true ;;
    --video-only)   VIDEO_ONLY=true ;;
    --combine-only) COMBINE_ONLY=true ;;
    --no-audio)     NO_AUDIO="--no-audio" ;;
  esac
done

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     Plum Endorsement Service — Video Demo Builder          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# ── Step 0: Check prerequisites ──────────────────────────────────────────
echo "🔍 Checking prerequisites..."
command -v ffmpeg >/dev/null 2>&1 || { echo "❌ ffmpeg required. Install: brew install ffmpeg"; exit 1; }
command -v say    >/dev/null 2>&1 || { echo "❌ macOS 'say' command not found. This script requires macOS."; exit 1; }
command -v node   >/dev/null 2>&1 || { echo "❌ Node.js required."; exit 1; }

# Check if app is running
if ! curl -sf http://localhost:5173 >/dev/null 2>&1; then
  echo "⚠️  Frontend (localhost:5173) not responding. Start with: ./start.sh"
  echo "   Continuing anyway — title card slides will still work."
fi
if ! curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
  echo "⚠️  Backend (localhost:8080) not responding. Start with: ./start.sh"
fi

# Install playwright if needed
if [ ! -d "$SCRIPT_DIR/node_modules" ]; then
  echo "📦 Installing dependencies..."
  cd "$SCRIPT_DIR" && npm install --silent
fi

echo "✅ Prerequisites OK"
echo ""

# ── Step 1: Generate narration audio ─────────────────────────────────────
if [ "$COMBINE_ONLY" = false ] && [ "$VIDEO_ONLY" = false ]; then
  echo "═══════════════════════════════════════════════════════════"
  echo "Step 1/4: Generating narration audio"
  echo "═══════════════════════════════════════════════════════════"
  bash "$SCRIPT_DIR/generate-narration.sh"
  echo ""

  if [ "$AUDIO_ONLY" = true ]; then
    echo "✅ Audio-only build complete."
    exit 0
  fi
fi

# ── Step 2: Build combined narration track ────────────────────────────────
if [ "$VIDEO_ONLY" = false ]; then
  echo "═══════════════════════════════════════════════════════════"
  echo "Step 2/4: Building combined narration track"
  echo "═══════════════════════════════════════════════════════════"

  COMBINED_AUDIO="$OUTPUT_DIR/narration-combined.wav"
  mkdir -p "$OUTPUT_DIR"

  # Use node to generate the ffmpeg concat file with silence gaps
  node -e "
const { SEGMENTS } = require('./narration');
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const audioDir = path.resolve('$AUDIO_DIR');
const outputDir = path.resolve('$OUTPUT_DIR');
const concatList = path.join(outputDir, 'concat-list.txt');

let lines = [];
for (const seg of SEGMENTS) {
  const audioFile = path.join(audioDir, seg.id + '.aiff');
  if (!fs.existsSync(audioFile)) {
    console.log('  Missing audio: ' + seg.id);
    continue;
  }
  lines.push(\"file '\" + audioFile + \"'\");

  if (seg.pauseAfter && seg.pauseAfter > 0) {
    const silenceFile = path.join(outputDir, 'silence_' + seg.pauseAfter + 'ms.wav');
    if (!fs.existsSync(silenceFile)) {
      const durSec = (seg.pauseAfter / 1000).toFixed(3);
      execSync('ffmpeg -y -f lavfi -i anullsrc=r=44100:cl=mono -t ' + durSec + ' ' + silenceFile, { stdio: 'pipe' });
    }
    lines.push(\"file '\" + silenceFile + \"'\");
  }
}
fs.writeFileSync(concatList, lines.join('\n'));
console.log('  Generated concat list with ' + SEGMENTS.length + ' segments');
"

  # Concatenate all audio segments
  ffmpeg -y -f concat -safe 0 -i "$OUTPUT_DIR/concat-list.txt" -c:a pcm_s16le "$COMBINED_AUDIO" 2>/dev/null
  AUDIO_DURATION=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$COMBINED_AUDIO")
  echo "  🎵 Combined narration: ${AUDIO_DURATION}s"
  echo ""

  if [ "$COMBINE_ONLY" = true ]; then
    # Skip to step 4
    echo "Skipping video recording (--combine-only)."
    echo ""
  fi
fi

# ── Step 3: Record browser demo ──────────────────────────────────────────
if [ "$COMBINE_ONLY" = false ]; then
  echo "═══════════════════════════════════════════════════════════"
  echo "Step 3/4: Recording browser demo with Playwright"
  echo "═══════════════════════════════════════════════════════════"
  echo "  🎬 Starting browser automation..."
  echo "  📹 Video will be saved to: $OUTPUT_DIR/"
  echo ""

  cd "$SCRIPT_DIR" && node demo.js $NO_AUDIO

  echo ""
fi

# ── Step 4: Combine video + audio ────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════"
echo "Step 4/4: Combining video + narration audio"
echo "═══════════════════════════════════════════════════════════"

# Find the most recent .webm video
VIDEO_FILE=$(ls -t "$OUTPUT_DIR"/*.webm 2>/dev/null | head -1)
COMBINED_AUDIO="$OUTPUT_DIR/narration-combined.wav"

if [ -z "$VIDEO_FILE" ]; then
  echo "❌ No video file found in $OUTPUT_DIR/. Run without --combine-only first."
  exit 1
fi

if [ ! -f "$COMBINED_AUDIO" ]; then
  echo "❌ No combined narration audio found. Run without --video-only first."
  exit 1
fi

echo "  📹 Video: $VIDEO_FILE"
echo "  🎵 Audio: $COMBINED_AUDIO"
echo "  🎬 Output: $FINAL_VIDEO"
echo ""

# Combine: take video from webm, audio from wav, output as mp4
# Use the shorter of the two streams to set duration
ffmpeg -y \
  -i "$VIDEO_FILE" \
  -i "$COMBINED_AUDIO" \
  -c:v libx264 -preset medium -crf 23 \
  -c:a aac -b:a 192k \
  -map 0:v:0 -map 1:a:0 \
  -shortest \
  -movflags +faststart \
  "$FINAL_VIDEO" 2>/dev/null

FINAL_SIZE=$(du -h "$FINAL_VIDEO" | cut -f1)
FINAL_DURATION=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$FINAL_VIDEO")
FINAL_MIN=$(echo "scale=1; $FINAL_DURATION / 60" | bc)

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                    ✅ BUILD COMPLETE                        ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  📹 File:     $FINAL_VIDEO"
echo "║  ⏱️  Duration: ${FINAL_MIN} minutes"
echo "║  📦 Size:     ${FINAL_SIZE}"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "  To play:  open \"$FINAL_VIDEO\""
echo "  To share: upload the .mp4 to Google Drive, Slack, or email."
