# mock-server — Spring Boot

Minimal Spring Boot 3 mock server (only `spring-boot-starter-web`). Requires only a JDK 17+; Maven is provided by the wrapper.

## Run

```bash
./start.sh                                  # builds target/mock-server.jar if missing/outdated, then starts
PORT=9090 MOCKS_DIR=/path/to/mocks ./start.sh
./start.sh --server.port=9090              # extra args are passed to the app
```

## Configuring the port

Edit [`config/mock-server.properties`](config/mock-server.properties) and restart (no rebuild needed):

```properties
server.port=8080
```

`start.sh` loads it via `--spring.config.additional-location`. Precedence, highest first:

1. CLI arg: `./start.sh --server.port=9090`
2. `PORT` env var: `PORT=9090 ./start.sh`
3. `server.port` in `config/mock-server.properties` (or the file named by `CONFIG_FILE`)
4. Built-in default `8080`

The file is only read by `start.sh`. Running `java -jar target/mock-server.jar` directly uses `PORT` or 8080.

| Env var | Default | Meaning |
|---|---|---|
| `PORT` | _(unset)_ | HTTP port; overrides the config file |
| `CONFIG_FILE` | `config/mock-server.properties` | Properties file loaded by `start.sh` (port etc.) |
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

### curl example per HTTP method

Responses below were pasted from a real run (`HEAD` `Date` header removed). `examples/dynamic-curls.sh` runs all of them against a running server.

**GET: user generated from the path variable and query**

```bash
curl -s 'localhost:8080/mock/dynamic/users/42?greeting=Hi'
```

```
{"id":"42","name":"Grace","email":"grace.42@example.com","role":"admin","age":30,"greeting":"Hi, Grace!"}
```

**POST: order totals (201)**

```bash
curl -s -X POST localhost:8080/mock/dynamic/orders -H 'Content-Type: application/json' -d '{"items":[{"sku":"A","price":9.99,"quantity":2},{"sku":"B","price":5,"quantity":1}],"taxRate":0.1}'
```

```
{"orderId":"ORD-7CA65AA4","items":[{"sku":"A","price":9.99,"quantity":2,"lineTotal":19.98},{"sku":"B","price":5,"quantity":1,"lineTotal":5.00}],"itemCount":3,"subtotal":24.98,"taxRate":0.1,"tax":2.50,"total":27.48}
```

**POST: validation error (400)**

```bash
curl -s -w '\nHTTP %{http_code}\n' -X POST localhost:8080/mock/dynamic/orders -H 'Content-Type: application/json' -d '{"items":[{"sku":"","price":-1,"quantity":0}]}'
```

```
{"error":"Invalid order","details":["items[0].sku is required","items[0].price must be a non-negative number","items[0].quantity must be a positive integer"]}
HTTP 400
```

**PUT: echo**

```bash
curl -s -X PUT 'localhost:8080/mock/dynamic/echo/items/7?x=1&x=2' -H 'Content-Type: application/json' -H 'X-Request-Id: abc' -d '{"name":"widget"}'
```

```
{"method":"PUT","path":"/echo/items/7","query":{"x":["1","2"]},"headers":{"Content-Type":"application/json","Accept":"*/*","User-Agent":"curl/8.14.1","X-Request-Id":"abc"},"body":{"name":"widget"}}
```

**PATCH: echo**

```bash
curl -s -X PATCH localhost:8080/mock/dynamic/echo/items/7 -H 'Content-Type: application/json' -d '{"op":"replace","path":"/name","value":"gadget"}'
```

```
{"method":"PATCH","path":"/echo/items/7","query":{},"headers":{"Content-Type":"application/json","Accept":"*/*","User-Agent":"curl/8.14.1"},"body":{"op":"replace","path":"/name","value":"gadget"}}
```

**DELETE: echo**

```bash
curl -s -X DELETE localhost:8080/mock/dynamic/echo/items/7
```

```
{"method":"DELETE","path":"/echo/items/7","query":{},"headers":{"Accept":"*/*","User-Agent":"curl/8.14.1"},"body":""}
```

**HEAD: GET handler, headers only (200, no body)**

```bash
curl -sI localhost:8080/mock/dynamic/users/42
```

```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
```

**OPTIONS: echo (routed to the handler, not answered by Spring)**

```bash
curl -s -X OPTIONS localhost:8080/mock/dynamic/echo/items
```

```
{"method":"OPTIONS","path":"/echo/items","query":{},"headers":{"Accept":"*/*","User-Agent":"curl/8.14.1"},"body":""}
```

**No matching handler (404)**

```bash
curl -s -w '\nHTTP %{http_code}\n' -X DELETE localhost:8080/mock/dynamic/unknown
```

```
{"error":"No dynamic handler found","method":"DELETE","path":"/mock/dynamic/unknown","availableHandlers":[{"name":"EchoHandler","path":"/mock/dynamic/echo/**","methods":["*"]},{"name":"UserHandler","path":"/mock/dynamic/users/{id}","methods":["GET"]},{"name":"OrderHandler","path":"/mock/dynamic/orders","methods":["POST"]}]}
HTTP 404
```

Notes:
- `HEAD` is routed to the matching handler for that method, or to its `GET` handler if it has none. The body is dropped, so `curl -I /mock/dynamic/users/42` returns 200.
- `OPTIONS` is passed to the handlers like any other method. Before this change Spring MVC answered it itself (`200` with an `Allow` header). A handler that does not list `OPTIONS` returns the usual 404 (for example `OPTIONS /mock/dynamic/orders`).

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
