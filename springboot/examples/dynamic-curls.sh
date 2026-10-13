#!/usr/bin/env bash
# Runs the dynamic-mock curl examples from README.md against a running server.
# Usage: ./start.sh &  then  ./examples/dynamic-curls.sh   (BASE_URL defaults to http://localhost:8080)
set -uo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
run() { echo "\$ $*"; eval "$@"; echo; echo; }

run "curl -s '$BASE_URL/mock/dynamic/users/42?greeting=Hi'"
run "curl -s -X POST $BASE_URL/mock/dynamic/orders -H 'Content-Type: application/json' -d '{\"items\":[{\"sku\":\"A\",\"price\":9.99,\"quantity\":2},{\"sku\":\"B\",\"price\":5,\"quantity\":1}],\"taxRate\":0.1}'"
run "curl -s -w '\nHTTP %{http_code}\n' -X POST $BASE_URL/mock/dynamic/orders -H 'Content-Type: application/json' -d '{\"items\":[{\"sku\":\"\",\"price\":-1,\"quantity\":0}]}'"
run "curl -s -X PUT '$BASE_URL/mock/dynamic/echo/items/7?x=1&x=2' -H 'Content-Type: application/json' -H 'X-Request-Id: abc' -d '{\"name\":\"widget\"}'"
run "curl -s -X PATCH $BASE_URL/mock/dynamic/echo/items/7 -H 'Content-Type: application/json' -d '{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"gadget\"}'"
run "curl -s -X DELETE $BASE_URL/mock/dynamic/echo/items/7"
run "curl -sI $BASE_URL/mock/dynamic/users/42"
run "curl -s -X OPTIONS $BASE_URL/mock/dynamic/echo/items"
run "curl -s -w '\nHTTP %{http_code}\n' -X DELETE $BASE_URL/mock/dynamic/unknown"
