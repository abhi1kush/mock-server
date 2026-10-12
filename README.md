# mock-server

A collection of simple HTTP mock servers in several languages, all driven by the same JSON mock definitions.

## Layout

| Folder | Status | Description |
|---|---|---|
| `mocks/` | ✅ | Shared mock definitions (`*.json`) every server can load via `MOCKS_DIR` |
| `springboot/` | ✅ | Java 17+ / Spring Boot 3 implementation |
| `golang/` | planned | Go implementation |
| `rust/` | planned | Rust implementation |
| `python/` | planned | Python implementation |

## Mock definition format

```json
{
  "method": "GET",
  "path": "/api/hello",
  "status": 200,
  "headers": { "X-Mock": "true" },
  "body": { "message": "Hello from mock-server!" }
}
```

Every server exposes `GET /health` → `{"status":"UP"}` and returns a 404 JSON error for unmatched requests.

## Quick start (Spring Boot)

```bash
cd springboot
./start.sh            # PORT=8080, MOCKS_DIR=../mocks by default
curl localhost:8080/api/hello
```

See [`springboot/README.md`](springboot/README.md) for details.
