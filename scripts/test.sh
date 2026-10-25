#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -z "${JAVA_HOME:-}" ]; then
  if [ -d .toolchain/jdk-17.0.18+8/Contents/Home ]; then JAVA_HOME="$PWD/.toolchain/jdk-17.0.18+8/Contents/Home";
  else JAVA_HOME="$PWD/.toolchain/jdk-17.0.18+8";fi
fi
mkdir -p build/tests
"$JAVA_HOME/bin/javac" -d build/tests app/src/org/xpan/camera/Geometry.java app/src/org/xpan/camera/CaptureGate.java tests/CoreTest.java
"$JAVA_HOME/bin/java" -cp build/tests CoreTest
PY="${XPAN_PYTHON:-python3}"
"$PY" -m unittest discover -s tests -p 'test_*.py' -v
