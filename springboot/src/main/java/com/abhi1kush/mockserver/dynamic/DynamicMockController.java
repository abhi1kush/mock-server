package com.abhi1kush.mockserver.dynamic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class DynamicMockController {
    public static final String BASE = "/mock/dynamic";
    private final List<DynamicRequestHandler> handlers;
    private final ObjectMapper mapper;

    /** Spring injects handlers sorted by {@code @Order}. */
    public DynamicMockController(List<DynamicRequestHandler> handlers, ObjectMapper mapper) {
        this.handlers = handlers;
        this.mapper = mapper;
    }

    @RequestMapping({BASE, BASE + "/**"})
    public ResponseEntity<?> handle(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        String path = uri.length() > BASE.length() ? uri.substring(BASE.length()) : "/";
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        for (DynamicRequestHandler h : handlers) {
            if (h.supports(path, method)) return h.handle(toRequest(request, method, path, h));
        }
        List<Map<String, Object>> available = handlers.stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", h.name());
            m.put("path", BASE + h.pathPattern());
            m.put("methods", h.methods().isEmpty() ? List.of("*") : h.methods().stream().map(HttpMethod::name).sorted().toList());
            return m;
        }).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "No dynamic handler found");
        body.put("method", method.name());
        body.put("path", uri);
        body.put("availableHandlers", available);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private DynamicRequest toRequest(HttpServletRequest request, HttpMethod method, String path,
                                     DynamicRequestHandler h) throws IOException {
        Map<String, String> vars = DynamicRequestHandler.MATCHER.extractUriTemplateVariables(h.pathPattern(), path);
        MultiValueMap<String, String> query = request.getQueryString() == null ? new LinkedMultiValueMap<>()
                : UriComponentsBuilder.newInstance().query(request.getQueryString()).build().getQueryParams();
        MultiValueMap<String, String> decoded = new LinkedMultiValueMap<>();
        query.forEach((k, vs) -> vs.forEach(v -> decoded.add(decode(k), v == null ? "" : decode(v))));
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames()).forEach(n -> Collections.list(request.getHeaders(n)).forEach(v -> headers.add(n, v)));
        String raw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        JsonNode json = null;
        if (!raw.isBlank()) {
            try { json = mapper.readTree(raw); } catch (JsonProcessingException ignored) { }
        }
        return new DynamicRequest(method, path, Map.copyOf(vars), decoded, headers, raw, json);
    }

    private static String decode(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
