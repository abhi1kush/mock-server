#!/usr/bin/env bash
# Build (if needed) and start the mock server.
# Env: PORT (default 8080), MOCKS_DIR (default ../mocks, the shared top-level folder)
set -euo pipefail
cd "$(dirname "$0")"
JAR=target/mock-server.jar
export PORT="${PORT:-8080}"
export MOCKS_DIR="${MOCKS_DIR:-../mocks}"
if [[ ! -f "$JAR" ]] || [[ -n "$(find src pom.xml -newer "$JAR" -print -quit)" ]]; then
  echo "Building $JAR ..."
  ./mvnw -q -DskipTests package
fi
echo "Starting mock-server on port $PORT with mocks from $MOCKS_DIR"
exec java -jar "$JAR"
