package com.abhi1kush.mockserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MockDefinition(String method, String path, Integer status,
                             Map<String, String> headers, JsonNode body) {
    public String normalizedMethod() { return method == null ? "GET" : method.toUpperCase(); }
    public int effectiveStatus() { return status == null ? 200 : status; }
}
