# mock-server — Spring Boot

Minimal Spring Boot 3 mock server (only `spring-boot-starter-web`). Requires only a JDK 17+; Maven is provided by the wrapper.

## Run

```bash
./start.sh                                  # builds target/mock-server.jar if missing/outdated, then starts
PORT=9090 MOCKS_DIR=/path/to/mocks ./start.sh
```

| Env var | Default | Meaning |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `MOCKS_DIR` | `../mocks` | Directory of `*.json` mock definitions (shared top-level folder) |

## Endpoints

- `GET /health` → `{"status":"UP"}`
- Any method/path matching a mock file → its `status`, `headers` and `body`
  (`method` defaults to GET, `status` to 200, Content-Type to application/json)
- Anything else → `404 {"error":"No mock found","method":...,"path":...}`

Mocks are loaded at startup; restart to pick up changes.

## Test

```bash
./mvnw test
```
