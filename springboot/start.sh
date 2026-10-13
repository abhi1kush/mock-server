#!/usr/bin/env bash
# Build (if needed) and start the mock server.
# Port precedence: CLI arg (--server.port=N) > PORT env > config file (server.port) > default 8080
# Env: PORT (optional), MOCKS_DIR (default ../mocks), CONFIG_FILE (default config/mock-server.properties)
# Extra args are passed to the app, e.g. ./start.sh --server.port=9090
set -euo pipefail
cd "$(dirname "$0")"
JAR=target/mock-server.jar
export MOCKS_DIR="${MOCKS_DIR:-../mocks}"
CONFIG_FILE="${CONFIG_FILE:-config/mock-server.properties}"
ARGS=()
if [[ -f "$CONFIG_FILE" ]]; then
  ARGS+=("--spring.config.additional-location=file:$CONFIG_FILE")
else
  echo "Config file $CONFIG_FILE not found; using defaults" >&2
fi
# PORT env, when set, is passed as an arg so it beats the file; an explicit --server.port arg beats PORT.
CLI_PORT=""
for a in "$@"; do [[ "$a" == --server.port=* ]] && CLI_PORT=${a#--server.port=}; done
[[ -n "${PORT:-}" && -z "$CLI_PORT" ]] && ARGS+=("--server.port=$PORT")
ARGS+=("$@")

# Work out the effective port only for the startup message.
EFFECTIVE_PORT=8080
if [[ -f "$CONFIG_FILE" ]]; then
  FILE_PORT=$(sed -n 's/^[[:space:]]*server\.port[[:space:]]*[=:][[:space:]]*\([0-9][0-9]*\).*/\1/p' "$CONFIG_FILE" | tail -1)
  [[ -n "$FILE_PORT" ]] && EFFECTIVE_PORT=$FILE_PORT
fi
[[ -n "${PORT:-}" ]] && EFFECTIVE_PORT=$PORT
[[ -n "$CLI_PORT" ]] && EFFECTIVE_PORT=$CLI_PORT

if [[ ! -f "$JAR" ]] || [[ -n "$(find src pom.xml -newer "$JAR" -print -quit)" ]]; then
  echo "Building $JAR ..."
  ./mvnw -q -DskipTests package
fi
echo "Starting mock-server on port $EFFECTIVE_PORT with mocks from $MOCKS_DIR (config: $CONFIG_FILE)"
exec java -jar "$JAR" "${ARGS[@]}"
