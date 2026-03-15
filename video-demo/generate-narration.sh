#!/usr/bin/env bash
#
# generate-narration.sh — Generate narration audio from segments using macOS `say`
#
# Usage: bash generate-narration.sh [--voice NAME]
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AUDIO_DIR="$SCRIPT_DIR/audio"
VOICE="${1:-Samantha}"

# Override voice if --voice flag is passed
for arg in "$@"; do
  case "$arg" in
    --voice=*) VOICE="${arg#*=}" ;;
  esac
done

mkdir -p "$AUDIO_DIR"

echo "🎙️  Generating narration audio with voice: $VOICE"
echo "   Output directory: $AUDIO_DIR"
echo ""

# Extract segment IDs and text from narration.js
# Using node to parse the JS module
node -e "
const { SEGMENTS } = require('./narration');
SEGMENTS.forEach(s => {
  console.log(s.id + '|||' + s.text);
});
" | while IFS='|||' read -r id text; do
  if [ -z "$id" ] || [ -z "$text" ]; then
    continue
  fi

  outfile="$AUDIO_DIR/${id}.aiff"

  if [ -f "$outfile" ]; then
    echo "   ⏭️  Skip (exists): $id"
    continue
  fi

  echo "   🔊 Generating: $id"
  say -v "$VOICE" -o "$outfile" "$text"
done

echo ""
echo "✅ Audio generation complete!"
echo ""

# Show total duration
TOTAL_SECS=0
for f in "$AUDIO_DIR"/*.aiff; do
  if [ -f "$f" ]; then
    dur=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$f" 2>/dev/null || echo "0")
    TOTAL_SECS=$(echo "$TOTAL_SECS + $dur" | bc)
  fi
done
TOTAL_MIN=$(echo "scale=1; $TOTAL_SECS / 60" | bc)
echo "📊 Total narration audio: ${TOTAL_MIN} minutes (${TOTAL_SECS}s)"
echo "   With pauses and navigation, expect ~${TOTAL_MIN}+ minutes total video"
