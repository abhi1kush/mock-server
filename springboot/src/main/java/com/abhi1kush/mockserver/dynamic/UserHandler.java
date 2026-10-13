package com.abhi1kush.mockserver.dynamic;

import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** GET /mock/dynamic/users/{id}?greeting=Hi returns a user deterministically derived from the id. */
@Component
@Order(20)
public class UserHandler implements DynamicRequestHandler {
    private static final String[] NAMES = {"Alice", "Bob", "Carol", "Dave", "Eve", "Frank", "Grace", "Heidi"};
    private static final String[] ROLES = {"admin", "editor", "viewer"};

    public record User(String id, String name, String email, String role, int age, String greeting) { }

    @Override public String pathPattern() { return "/users/{id}"; }

    @Override public Set<HttpMethod> methods() { return Set.of(HttpMethod.GET); }

    @Override
    public ResponseEntity<?> handle(DynamicRequest req) {
        String id = req.pathVariable("id");
        if (!id.matches("[A-Za-z0-9_-]{1,64}")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid user id", "id", id));
        }
        int h = Math.abs(id.hashCode() % 1_000_003);
        String name = NAMES[h % NAMES.length];
        String greeting = req.query("greeting", "Hello") + ", " + name + "!";
        return ResponseEntity.ok(new User(id, name, name.toLowerCase() + "." + id + "@example.com",
                ROLES[h % ROLES.length], 18 + h % 50, greeting));
    }
}
