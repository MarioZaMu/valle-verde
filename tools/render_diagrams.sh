#!/usr/bin/env bash
# Renders PlantUML files in docs/diagramas to PNG (UTF-8), one file at a time (-nbthread 1 avoids Graphviz clashes).
# Usage: tools/render_diagrams.sh [file.puml ...]   (no arguments = every .puml in docs/diagramas)
set -e
export PLANTUML_LIMIT_SIZE=16384
cd "$(dirname "$0")/.."
export GRAPHVIZ_DOT="${GRAPHVIZ_DOT:-C:/Program Files/Graphviz/bin/dot.exe}"
if [ $# -eq 0 ]; then set -- docs/diagramas/*.puml; fi
for f in "$@"; do
  echo "render $f"
  java -jar tools/plantuml.jar -charset UTF-8 -nbthread 1 -tpng "$f"
done
