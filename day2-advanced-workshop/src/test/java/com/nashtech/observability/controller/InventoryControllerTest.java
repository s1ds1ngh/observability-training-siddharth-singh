package com.nashtech.observability.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Integration tests for InventoryController.
 * Verifies HTTP responses and custom metric increments for both success and
 * error scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    private double getCounterValue(String status) {
        Counter counter = meterRegistry.find("inventory_requests_total")
                .tag("status", status)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }

    @Test
    void shouldReturnInventoryItem() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/inventory/42"))
                .andReturn();

        int statusCode = result.getResponse().getStatus();
        assertThat(statusCode).isIn(200, 500); // Either success or simulated error
    }

    @Test
    void shouldContainItemIdInResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/inventory/99"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("99");
    }

    @Test
    void shouldContainRequestIdInResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/inventory/7"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("requestId");
    }

    @Test
    void shouldIncrementMetricsOnRequests() throws Exception {
        double initialSuccess = getCounterValue("success");
        double initialError = getCounterValue("error");

        // Fire multiple requests to ensure at least some succeed and some fail
        int totalRequests = 50;
        for (int i = 0; i < totalRequests; i++) {
            mockMvc.perform(get("/api/inventory/" + i));
        }

        double finalSuccess = getCounterValue("success");
        double finalError = getCounterValue("error");

        double totalIncrement = (finalSuccess - initialSuccess) + (finalError - initialError);

        // All requests should be counted (either success or error)
        assertThat(totalIncrement).isEqualTo(totalRequests);

        // With 20% error rate and 50 requests, we expect both counters to have
        // incremented
        assertThat(finalSuccess).isGreaterThan(initialSuccess);
        // Note: With randomness there's a small chance all 50 succeed,
        // but with 20% error rate it's extremely unlikely
    }

    @Test
    void successCounterShouldIncrementOnSuccessfulRequest() throws Exception {
        // Run enough requests to statistically guarantee at least one success
        double initialSuccess = getCounterValue("success");

        for (int i = 100; i < 130; i++) {
            mockMvc.perform(get("/api/inventory/" + i));
        }

        double finalSuccess = getCounterValue("success");
        assertThat(finalSuccess).isGreaterThan(initialSuccess);
    }

    @Test
    void errorCounterShouldIncrementOnErrorRequest() throws Exception {
        // Run enough requests to statistically guarantee at least one error
        double initialError = getCounterValue("error");

        for (int i = 200; i < 250; i++) {
            mockMvc.perform(get("/api/inventory/" + i));
        }

        double finalError = getCounterValue("error");
        assertThat(finalError).isGreaterThan(initialError);
    }

    @Test
    void metricsShouldHaveCorrectTags() throws Exception {
        // Make a request to ensure metrics are registered
        mockMvc.perform(get("/api/inventory/tag-test"));

        // Verify the counter exists with expected tags
        Counter successCounter = meterRegistry.find("inventory_requests_total")
                .tag("status", "success")
                .counter();
        Counter errorCounter = meterRegistry.find("inventory_requests_total")
                .tag("status", "error")
                .counter();

        assertThat(successCounter).isNotNull();
        assertThat(errorCounter).isNotNull();
        assertThat(successCounter.getId().getTag("status")).isEqualTo("success");
        assertThat(errorCounter.getId().getTag("status")).isEqualTo("error");
    }

    @Test
    void metricRegistryShouldContainJvmMetrics() {
        // Verify JVM metrics are registered (indicating Micrometer/Actuator is properly
        // configured)
        assertThat(meterRegistry.find("jvm.memory.used").gauge()).isNotNull();
        assertThat(meterRegistry.find("jvm.threads.live").gauge()).isNotNull();
    }

    @Test
    void metricRegistryShouldContainCustomInventoryMetric() throws Exception {
        // Trigger at least one request to register the counters
        mockMvc.perform(get("/api/inventory/metric-check"));

        // Verify our custom counter is registered with both tag variants
        assertThat(meterRegistry.find("inventory_requests_total").counters())
                .isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(2); // success + error counters
    }
}
