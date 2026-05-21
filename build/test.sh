#!/bin/sh
# Compiles and runs the JTL test suite.
# Layout:
#   build/JTL.jar     — production classes (must be built first via build.sh)
#   test/*.java       — test sources
#   build/test-out/   — compiled test classes (created here)
#
# Options:
#   --update    Regenerate golden files in test/expected/ instead of comparing
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_SRC_DIR="$ROOT_DIR/test"
TEST_OUT_DIR="$SCRIPT_DIR/test-out"
JAR="$SCRIPT_DIR/JTL.jar"

# Parse flags
JAVA_FLAGS="-Djtl.root=$ROOT_DIR"
for arg in "$@"; do
    if [ "$arg" = "--update" ]; then
        JAVA_FLAGS="$JAVA_FLAGS -Dupdate=true"
    fi
done

if [ ! -f "$JAR" ]; then
    echo "JTL.jar not found at $JAR — run build.sh first." >&2
    exit 1
fi

rm -rf "$TEST_OUT_DIR"
mkdir -p "$TEST_OUT_DIR"

javac --release 8 -Xlint:-options -g -cp "$JAR" -d "$TEST_OUT_DIR" "$TEST_SRC_DIR"/*.java

java $JAVA_FLAGS -cp "$TEST_OUT_DIR:$JAR" TestHarness
