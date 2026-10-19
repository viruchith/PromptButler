#!/usr/bin/env bash
# Launch the Prompt Butler shadow JAR without keeping the terminal open.
# Usage: ./run.sh [path/to/prompt-butler-all.jar]
# Default JAR path: build/libs/prompt-butler-*-all.jar (most-recently built).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

JAR="${1:-}"
if [[ -z "$JAR" ]]; then
    JAR="$(ls -t "$SCRIPT_DIR"/build/libs/*-all.jar 2>/dev/null | head -1)"
fi

if [[ -z "$JAR" || ! -f "$JAR" ]]; then
    echo "ERROR: shadow JAR not found. Run './gradlew shadowJar' first, or pass the path as an argument." >&2
    exit 1
fi

# Detach from the terminal: stdout/stderr to /dev/null, process in background.
nohup java \
    --add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED \
    --add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED \
    -jar "$JAR" \
    > /dev/null 2>&1 &

echo "Prompt Butler started (PID $!)."
