package com.abhi1kush.mockserver.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * POST /mock/dynamic/orders with {"items":[{"sku":"A","price":9.99,"quantity":2}],"taxRate":0.1}
 * returns line totals, subtotal, tax and total. Invalid input yields 400 with a list of errors.
 */
@Component
@Order(30)
public class OrderHandler implements DynamicRequestHandler {
    public record Line(String sku, BigDecimal price, int quantity, BigDecimal lineTotal) { }

    public record OrderSummary(String orderId, List<Line> items, int itemCount, BigDecimal subtotal,
                               BigDecimal taxRate, BigDecimal tax, BigDecimal total) { }

    @Override public String pathPattern() { return "/orders"; }

    @Override public Set<HttpMethod> methods() { return Set.of(HttpMethod.POST); }

    @Override
    public ResponseEntity<?> handle(DynamicRequest req) {
        JsonNode json = req.json();
        List<String> errors = new ArrayList<>();
        if (json == null || !json.isObject()) {
            return badRequest(List.of("Body must be a JSON object"));
        }
        JsonNode items = json.get("items");
        if (items == null || !items.isArray() || items.isEmpty()) errors.add("items must be a non-empty array");
        BigDecimal taxRate = BigDecimal.ZERO;
        JsonNode tr = json.get("taxRate");
        if (tr != null && !tr.isNull()) {
            if (!tr.isNumber() || tr.decimalValue().signum() < 0 || tr.decimalValue().compareTo(BigDecimal.ONE) > 0) {
                errors.add("taxRate must be a number between 0 and 1");
            } else taxRate = tr.decimalValue();
        }
        List<Line> lines = new ArrayList<>();
        if (items != null && items.isArray()) {
            for (int i = 0; i < items.size(); i++) {
                JsonNode it = items.get(i);
                String p = "items[" + i + "]";
                JsonNode sku = it.get("sku"), price = it.get("price"), qty = it.get("quantity");
                if (sku == null || !sku.isTextual() || sku.asText().isBlank()) errors.add(p + ".sku is required");
                if (price == null || !price.isNumber() || price.decimalValue().signum() < 0) errors.add(p + ".price must be a non-negative number");
                if (qty == null || !qty.isIntegralNumber() || qty.asInt() < 1) errors.add(p + ".quantity must be a positive integer");
                if (errors.isEmpty()) {
                    BigDecimal pr = price.decimalValue();
                    lines.add(new Line(sku.asText(), pr, qty.asInt(), money(pr.multiply(BigDecimal.valueOf(qty.asInt())))));
                }
            }
        }
        if (!errors.isEmpty()) return badRequest(errors);
        BigDecimal subtotal = money(lines.stream().map(Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal tax = money(subtotal.multiply(taxRate));
        int count = lines.stream().mapToInt(Line::quantity).sum();
        String orderId = "ORD-" + Integer.toHexString(Math.abs(req.rawBody().hashCode())).toUpperCase();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderSummary(orderId, lines, count, subtotal, taxRate, tax, money(subtotal.add(tax))));
    }

    private static BigDecimal money(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }

    private static ResponseEntity<?> badRequest(List<String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Invalid order");
        body.put("details", errors);
        return ResponseEntity.badRequest().body(body);
    }
}
