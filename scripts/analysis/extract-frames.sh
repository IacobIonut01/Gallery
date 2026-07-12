#!/usr/bin/env bash
#
# extract-frames.sh — extract evenly-spaced frames from a video for visual analysis.
#
# Usage:
#   extract-frames.sh <video-path> [num-frames] [out-dir]
#
# Args:
#   video-path   Path to the source video (required).
#   num-frames   How many evenly-spaced frames to extract (default: 12).
#   out-dir      Output directory for frames (default: a temp dir under $TMPDIR).
#
# Output:
#   Prints the output directory on the last line. Frames are written as
#   frame_001.jpg, frame_002.jpg, ... and a montage.jpg contact sheet.
#
set -euo pipefail

VIDEO="${1:-}"
NUM_FRAMES="${2:-12}"
OUT_DIR="${3:-}"

if [[ -z "$VIDEO" ]]; then
  echo "ERROR: no video path provided" >&2
  echo "Usage: extract-frames.sh <video-path> [num-frames] [out-dir]" >&2
  exit 1
fi

if [[ ! -f "$VIDEO" ]]; then
  echo "ERROR: file not found: $VIDEO" >&2
  exit 1
fi

command -v ffmpeg  >/dev/null 2>&1 || { echo "ERROR: ffmpeg not installed"  >&2; exit 1; }
command -v ffprobe >/dev/null 2>&1 || { echo "ERROR: ffprobe not installed" >&2; exit 1; }

if [[ -z "$OUT_DIR" ]]; then
  OUT_DIR="$(mktemp -d "${TMPDIR:-/tmp}/video-frames.XXXXXX")"
fi
mkdir -p "$OUT_DIR"

# Duration in seconds (float).
DURATION="$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$VIDEO" || true)"

echo "Video:    $VIDEO" >&2
echo "Duration: ${DURATION:-unknown} s" >&2
echo "Frames:   $NUM_FRAMES" >&2
echo "Out dir:  $OUT_DIR" >&2

if [[ -n "$DURATION" && "$DURATION" != "N/A" ]]; then
  # Evenly spaced across the whole video, avoiding the very first/last frame.
  STEP="$(awk -v d="$DURATION" -v n="$NUM_FRAMES" 'BEGIN { printf "%.6f", d / (n + 1) }')"
  for i in $(seq 1 "$NUM_FRAMES"); do
    TS="$(awk -v s="$STEP" -v i="$i" 'BEGIN { printf "%.6f", s * i }')"
    OUT="$(printf "%s/frame_%03d.jpg" "$OUT_DIR" "$i")"
    ffmpeg -nostdin -loglevel error -ss "$TS" -i "$VIDEO" \
      -frames:v 1 -vf "scale=640:-2" -q:v 3 -y "$OUT"
  done
else
  # Fallback: grab frames by thumbnail filter when duration is unknown.
  ffmpeg -nostdin -loglevel error -i "$VIDEO" \
    -vf "thumbnail,scale=640:-2" -frames:v "$NUM_FRAMES" -q:v 3 -y \
    "$OUT_DIR/frame_%03d.jpg"
fi

# Build an optional contact sheet (best effort; ignore failures).
GRID="$(awk -v n="$NUM_FRAMES" 'BEGIN {
  c = int(sqrt(n)); if (c < 1) c = 1;
  r = int((n + c - 1) / c);
  printf "%dx%d", c, r
}')"
ffmpeg -nostdin -loglevel error -pattern_type glob -i "$OUT_DIR/frame_*.jpg" \
  -vf "tile=${GRID}" -frames:v 1 -y "$OUT_DIR/montage.jpg" 2>/dev/null || true

echo "$OUT_DIR"
