package com.abhi1kush.mockserver.dynamic;

import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;

/**
 * A code-driven mock under {@code /mock/dynamic/}. Implement this and annotate with
 * {@code @Component} (optionally {@code @Order}) to register it.
 */
public interface DynamicRequestHandler {
    AntPathMatcher MATCHER = new AntPathMatcher();

    /** Ant-style pattern relative to /mock/dynamic, e.g. {@code /users/{id}}. */
    String pathPattern();

    /** Supported methods; empty means all methods. */
    Set<HttpMethod> methods();

    default boolean supports(String path, HttpMethod method) {
        return (methods().isEmpty() || methods().contains(method)) && MATCHER.match(pathPattern(), path);
    }

    ResponseEntity<?> handle(DynamicRequest req);

    default String name() { return getClass().getSimpleName(); }
}
