package com.abhi1kush.mockserver.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

/**
 * Incoming request seen by a {@link DynamicRequestHandler}.
 * {@code json} is null when the body is empty or not valid JSON.
 */
public record DynamicRequest(HttpMethod method, String path, Map<String, String> pathVariables,
                             MultiValueMap<String, String> queryParams, HttpHeaders headers,
                             String rawBody, JsonNode json) {
    public String query(String name) { return queryParams.getFirst(name); }

    public String query(String name, String fallback) {
        String v = query(name);
        return v == null ? fallback : v;
    }

    public String pathVariable(String name) { return pathVariables.get(name); }

    public List<String> queryAll(String name) { return queryParams.getOrDefault(name, List.of()); }
}
