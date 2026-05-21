#!/bin/sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$(cd "$SCRIPT_DIR/../src" && pwd)"

cd "$SCRIPT_DIR"

rm -f *.class
rm -f *.jar
javac --release 8 -Xlint:-options -g -d "$SCRIPT_DIR" "$SRC_DIR"/*.java
jar cvfm JTL.jar MANIFEST.MF *.class
