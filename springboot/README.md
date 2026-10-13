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

- Any method under `/mock/dynamic/**` → code-driven handlers (see below)

Mocks are loaded at startup; restart to pick up changes.

## Test

```bash
./mvnw test
```

## Dynamic mocks (`/mock/dynamic/**`)

For responses computed from the request, `DynamicMockController` dispatches every method under
`/mock/dynamic/**` to the first `DynamicRequestHandler` bean (sorted by `@Order`) whose `pathPattern()`
(Ant-style, relative to `/mock/dynamic`) and `methods()` match. If none match it returns
`404 {"error":"No dynamic handler found", ..., "availableHandlers":[...]}`.

Handlers receive a `DynamicRequest` record: `method`, `path`, `pathVariables`, `queryParams`, `headers`,
`rawBody`, and `json` (Jackson `JsonNode`, null if the body is empty or not JSON).

### Built-in examples

| Handler | Route | Behaviour |
|---|---|---|
| `EchoHandler` | any method `/mock/dynamic/echo/**` | echoes method, path, query, selected headers and body |
| `UserHandler` | `GET /mock/dynamic/users/{id}?greeting=Hi` | deterministic user generated from the id |
| `OrderHandler` | `POST /mock/dynamic/orders` | validates items and returns subtotal/tax/total (`201`, or `400` with details) |

```bash
curl -X PUT 'localhost:8080/mock/dynamic/echo/a/b?x=1&x=2' -H 'Content-Type: application/json' -d '{"k":"v"}'
# {"method":"PUT","path":"/echo/a/b","query":{"x":["1","2"]},"headers":{...},"body":{"k":"v"}}

curl 'localhost:8080/mock/dynamic/users/42?greeting=Hi'
# {"id":"42","name":"Grace","email":"grace.42@example.com","role":"admin","age":30,"greeting":"Hi, Grace!"}

curl -X POST localhost:8080/mock/dynamic/orders -H 'Content-Type: application/json' \
  -d '{"items":[{"sku":"A","price":9.99,"quantity":2},{"sku":"B","price":5,"quantity":1}],"taxRate":0.1}'
# 201 {"orderId":"ORD-...","items":[...],"itemCount":3,"subtotal":24.98,"taxRate":0.1,"tax":2.50,"total":27.48}

curl -X DELETE localhost:8080/mock/dynamic/unknown
# 404 {"error":"No dynamic handler found",...,"availableHandlers":[...]}
```

### Adding a handler

Implement the interface in `com.abhi1kush.mockserver.dynamic` and annotate it with `@Component`
(add `@Order` to control precedence when patterns overlap):

```java
@Component
@Order(40)
public class TimeHandler implements DynamicRequestHandler {
    @Override public String pathPattern() { return "/time/{zone}"; }
    @Override public Set<HttpMethod> methods() { return Set.of(HttpMethod.GET); } // empty = all methods

    @Override
    public ResponseEntity<?> handle(DynamicRequest req) {
        ZoneId zone = ZoneId.of(req.pathVariable("zone").replace('_', '/'));
        return ResponseEntity.ok(Map.of("zone", zone.getId(), "now", ZonedDateTime.now(zone).toString()));
    }
}
```

Restart the server; it is picked up automatically. Override `supports(path, method)` for custom matching.
