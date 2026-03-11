package com.nashtech.observability.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Inventory REST Controller that simulates realistic traffic patterns
 * including random processing delays and random error responses.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final Random random = new Random();

    private final Counter successCounter;
    private final Counter errorCounter;

    public InventoryController(MeterRegistry meterRegistry) {
        this.successCounter = Counter.builder("inventory_requests_total")
                .description("Total number of inventory requests")
                .tag("status", "success")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("inventory_requests_total")
                .description("Total number of inventory requests")
                .tag("status", "error")
                .register(meterRegistry);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Map<String, Object>> getItem(@PathVariable String itemId) {

        // Set MDC values for structured logging correlation
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("itemId", itemId);
        MDC.put("endpoint", "/api/inventory/" + itemId);

        try {
            log.info("Received request for inventory item lookup");

            // Simulate random processing delay (~30% of requests)
            if (random.nextInt(100) < 30) {
                int delay = random.nextInt(500) + 100;
                log.debug("Simulating processing delay of {} ms for itemId={}", delay, itemId);
                Thread.sleep(delay);
            }

            // Simulate random error (~20% of requests)
            if (random.nextInt(100) < 20) {
                throw new RuntimeException("Simulated internal server error for itemId=" + itemId);
            }

            // Successful response
            log.debug("Successfully retrieved item. itemId={}, requestId={}", itemId, requestId);

            Map<String, Object> response = Map.of(
                    "itemId", itemId,
                    "name", "Widget-" + itemId,
                    "quantity", random.nextInt(100) + 1,
                    "price", Math.round(random.nextDouble() * 100.0 * 100.0) / 100.0,
                    "requestId", requestId);

            successCounter.increment();
            log.info("Request completed successfully for itemId={}", itemId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            errorCounter.increment();
            log.error("Error processing request for itemId={}: {}", itemId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage(),
                    "itemId", itemId,
                    "requestId", requestId));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorCounter.increment();
            log.error("Request interrupted for itemId={}", itemId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Request Interrupted",
                    "itemId", itemId,
                    "requestId", requestId));
        } finally {
            MDC.clear();
        }
    }
}
