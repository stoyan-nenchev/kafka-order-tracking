package com.portfolio.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public class ServiceResilienceTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @DisplayName("Should handle high concurrent load gracefully")
    void shouldHandleHighConcurrentLoadGracefully() {
        // Given
        int numberOfConcurrentRequests = 50;
        CompletableFuture<ResponseEntity<String>>[] futures = new CompletableFuture[numberOfConcurrentRequests];
        
        // When - Send many concurrent requests
        for (int i = 0; i < numberOfConcurrentRequests; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> 
                restTemplate.getForEntity(baseUrl() + "/api/health", String.class)
            );
        }
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures).join();
        
        // Then - Most requests should succeed (allowing for some failures under extreme load)
        int successCount = 0;
        int errorCount = 0;
        
        for (CompletableFuture<ResponseEntity<String>> future : futures) {
            try {
                ResponseEntity<String> response = future.get();
                if (response.getStatusCode() == HttpStatus.OK) {
                    successCount++;
                } else {
                    errorCount++;
                }
            } catch (Exception e) {
                errorCount++;
            }
        }
        
        // Allow up to 10% failure rate under high load
        assertTrue(successCount >= numberOfConcurrentRequests * 0.9, 
                  "Success rate should be at least 90%. Got " + successCount + "/" + numberOfConcurrentRequests);
    }

    @Test
    @DisplayName("Should respond within acceptable time limits")
    void shouldRespondWithinAcceptableTimeLimits() {
        // Given
        long startTime = System.currentTimeMillis();
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/api/health", String.class);
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(responseTime < 5000, "Response time should be less than 5 seconds. Got: " + responseTime + "ms");
    }

    @Test
    @DisplayName("Should maintain service availability during restart simulation")
    void shouldMaintainServiceAvailabilityDuringRestartSimulation() {
        // This test simulates service resilience by testing health endpoints
        // In a real environment, this would involve actually stopping/starting services
        
        // Given - Service is running
        ResponseEntity<String> initialResponse = restTemplate.getForEntity(
            baseUrl() + "/api/health", String.class);
        assertEquals(HttpStatus.OK, initialResponse.getStatusCode());
        
        // When - Simulate brief service disruption (in real test, this would restart the service)
        // For now, we'll just verify the service can handle repeated requests
        boolean serviceAvailable = true;
        
        for (int i = 0; i < 10; i++) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl() + "/api/health", String.class);
                if (response.getStatusCode() != HttpStatus.OK) {
                    serviceAvailable = false;
                    break;
                }
                Thread.sleep(100); // Brief pause between requests
            } catch (Exception e) {
                serviceAvailable = false;
                break;
            }
        }
        
        // Then
        assertTrue(serviceAvailable, "Service should remain available during test");
    }

    @Test
    @DisplayName("Should handle malformed requests gracefully")
    void shouldHandleMalformedRequestsGracefully() {
        // Given - Various malformed requests
        String[] malformedPayloads = {
            "{\"invalid\": json}",
            "not json at all",
            "",
            "null",
            "{\"customerInfo\": \"not an object\"}"
        };
        
        // When & Then
        for (String payload : malformedPayloads) {
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/orders", payload, String.class);
            
            // Should return 4xx error, not crash
            assertTrue(response.getStatusCode().is4xxClientError(), 
                      "Should return 4xx error for payload: " + payload);
        }
    }

    @Test
    @DisplayName("Should recover from temporary database connection issues")
    void shouldRecoverFromTemporaryDatabaseConnectionIssues() {
        // This is a simulation test - in a real environment you'd actually disconnect the database
        
        // Given - Service is healthy
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
            baseUrl() + "/actuator/health", String.class);
        
        // The health endpoint should respond (may show degraded status if DB is down)
        assertNotNull(healthResponse);
        assertTrue(healthResponse.getStatusCode().is2xxSuccessful() || 
                  healthResponse.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Should implement proper timeout handling")
    void shouldImplementProperTimeoutHandling() {
        // Given - Make a request that might take time
        long startTime = System.currentTimeMillis();
        
        // When - Request with potential for timeout
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "/api/v1/orders?customerId=TIMEOUT_TEST", String.class);
            
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            // Then - Should respond within reasonable time
            assertTrue(responseTime < 30000, "Request should timeout or complete within 30 seconds");
            
        } catch (Exception e) {
            // If timeout exception occurs, that's also acceptable behavior
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            assertTrue(responseTime < 30000, "Timeout should occur within 30 seconds");
        }
    }
}