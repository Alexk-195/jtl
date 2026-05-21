#!/bin/sh
# Compiles and runs the JTL test suite.
# Layout:
#   build/JTL.jar     — production classes (must be built first via build.sh)
#   test/*.java       — test sources
#   build/test-out/   — compiled test classes (created here)
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_SRC_DIR="$ROOT_DIR/test"
TEST_OUT_DIR="$SCRIPT_DIR/test-out"
JAR="$SCRIPT_DIR/JTL.jar"

if [ ! -f "$JAR" ]; then
    echo "JTL.jar not found at $JAR — run build.sh first." >&2
    exit 1
fi

rm -rf "$TEST_OUT_DIR"
mkdir -p "$TEST_OUT_DIR"

javac --release 8 -Xlint:-options -g -cp "$JAR" -d "$TEST_OUT_DIR" "$TEST_SRC_DIR"/*.java

java -cp "$TEST_OUT_DIR:$JAR" TestHarness
