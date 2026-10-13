package com.abhi1kush.mockserver.dynamic;

import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Any method on /mock/dynamic/echo/** returns what it received. */
@Component
@Order(10)
public class EchoHandler implements DynamicRequestHandler {
    private static final List<String> ECHOED_HEADERS = List.of(
            HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.USER_AGENT, "X-Request-Id");

    @Override public String pathPattern() { return "/echo/**"; }

    @Override public Set<HttpMethod> methods() { return Set.of(); }

    @Override
    public ResponseEntity<?> handle(DynamicRequest req) {
        Map<String, String> headers = new LinkedHashMap<>();
        ECHOED_HEADERS.forEach(h -> { String v = req.headers().getFirst(h); if (v != null) headers.put(h, v); });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("method", req.method().name());
        out.put("path", req.path());
        out.put("query", req.queryParams());
        out.put("headers", headers);
        out.put("body", req.json() != null ? req.json() : req.rawBody());
        return ResponseEntity.ok(out);
    }
}
