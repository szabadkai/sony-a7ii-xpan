#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -z "${JAVA_HOME:-}" ]; then
  if [ -d .toolchain/jdk-17.0.18+8/Contents/Home ]; then JAVA_HOME="$PWD/.toolchain/jdk-17.0.18+8/Contents/Home";
  else JAVA_HOME="$PWD/.toolchain/jdk-17.0.18+8";fi
fi
mkdir -p build/ui-preview-classes
"$JAVA_HOME/bin/javac" -d build/ui-preview-classes app/src/org/xpan/camera/Geometry.java app/src/org/xpan/camera/UiRenderer.java tools/preview/RenderUi.java
"$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp build/ui-preview-classes RenderUi build/ui-preview
