package com.abhi1kush.mockserver;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockController {
    private final MockRegistry registry;

    public MockController(MockRegistry registry) { this.registry = registry; }

    @RequestMapping("/**")
    public ResponseEntity<Object> handle(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return registry.find(method, path).<ResponseEntity<Object>>map(m -> {
            HttpHeaders headers = new HttpHeaders();
            if (m.headers() != null) m.headers().forEach(headers::add);
            if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) headers.setContentType(MediaType.APPLICATION_JSON);
            JsonNode body = m.body();
            Object out = body == null || body.isNull() ? null : (body.isTextual() ? body.asText() : body);
            return new ResponseEntity<>(out, headers, m.effectiveStatus());
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "No mock found", "method", method, "path", path)));
    }
}
