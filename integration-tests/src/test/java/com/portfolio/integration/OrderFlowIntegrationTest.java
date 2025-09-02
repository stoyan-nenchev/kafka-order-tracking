package com.portfolio.integration;

import com.portfolio.shared.dto.CustomerInfo;
import com.portfolio.shared.dto.OrderItem;
import com.portfolio.orderservice.dto.CreateOrderRequest;
import com.portfolio.orderservice.dto.OrderResponse;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderFlowIntegrationTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(
            new File("infrastructure/docker-compose.yml"))
            .withExposedService("kafka-controller", 9092, Wait.forListeningPort())
            .withExposedService("postgres-order", 5432, Wait.forListeningPort())
            .withExposedService("postgres-inventory", 5433, Wait.forListeningPort())
            .withExposedService("postgres-shipping", 5434, Wait.forListeningPort())
            .withExposedService("postgres-notification", 5435, Wait.forListeningPort())
            .withExposedService("postgres-analytics", 5436, Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(5));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> 
            environment.getServiceHost("kafka-controller", 9092) + ":" + 
            environment.getServicePort("kafka-controller", 9092));
        
        registry.add("spring.datasource.url", () -> 
            "jdbc:postgresql://" + environment.getServiceHost("postgres-order", 5432) + 
            ":" + environment.getServicePort("postgres-order", 5432) + "/orderdb");
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @Order(1)
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderRequest request = createValidOrderRequest();
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            baseUrl() + "/api/v1/orders", request, OrderResponse.class);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals("CREATED", response.getBody().getStatus());
        assertEquals(request.getTotalAmount(), response.getBody().getTotalAmount());
    }

    @Test
    @Order(2)
    @DisplayName("Should process complete order lifecycle")
    void shouldProcessCompleteOrderLifecycle() {
        // Given
        CreateOrderRequest request = createValidOrderRequest();
        
        // When - Create Order
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
            baseUrl() + "/api/v1/orders", request, OrderResponse.class);
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        String correlationId = createResponse.getBody().getCorrelationId();
        
        // Then - Wait for inventory confirmation (with timeout)
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(2, TimeUnit.SECONDS)
               .until(() -> {
                   ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                       baseUrl() + "/api/v1/orders/correlation/" + correlationId, OrderResponse.class);
                   return response.getBody() != null && 
                          ("CONFIRMED".equals(response.getBody().getStatus()) || 
                           "REJECTED".equals(response.getBody().getStatus()));
               });

        // Verify final order status
        ResponseEntity<OrderResponse> finalResponse = restTemplate.getForEntity(
            baseUrl() + "/api/v1/orders/correlation/" + correlationId, OrderResponse.class);
        
        assertTrue(finalResponse.getBody().getStatus().equals("CONFIRMED") || 
                  finalResponse.getBody().getStatus().equals("REJECTED"));
    }

    @Test
    @Order(3)
    @DisplayName("Should handle inventory rejection gracefully")
    void shouldHandleInventoryRejectionGracefully() {
        // Given - Order with very high quantity to trigger rejection
        CreateOrderRequest request = createHighQuantityOrderRequest();
        
        // When
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
            baseUrl() + "/api/v1/orders", request, OrderResponse.class);
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        String correlationId = createResponse.getBody().getCorrelationId();
        
        // Then - Wait for rejection
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(2, TimeUnit.SECONDS)
               .until(() -> {
                   ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                       baseUrl() + "/api/v1/orders/correlation/" + correlationId, OrderResponse.class);
                   return response.getBody() != null && "REJECTED".equals(response.getBody().getStatus());
               });
    }

    @Test
    @Order(4)
    @DisplayName("Should validate request data properly")
    void shouldValidateRequestDataProperly() {
        // Given - Invalid request
        CreateOrderRequest request = CreateOrderRequest.builder()
            .customerInfo(null) // Invalid - null customer info
            .orderItems(List.of())  // Invalid - empty items
            .totalAmount(BigDecimal.ZERO) // Invalid - zero amount
            .build();
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/v1/orders", request, String.class);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @Order(5)
    @DisplayName("Should handle concurrent order creation")
    void shouldHandleConcurrentOrderCreation() {
        // Given
        int numberOfOrders = 10;
        CreateOrderRequest[] requests = new CreateOrderRequest[numberOfOrders];
        
        for (int i = 0; i < numberOfOrders; i++) {
            requests[i] = createValidOrderRequest();
        }
        
        // When - Create orders concurrently
        ResponseEntity<OrderResponse>[] responses = new ResponseEntity[numberOfOrders];
        Thread[] threads = new Thread[numberOfOrders];
        
        for (int i = 0; i < numberOfOrders; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                responses[index] = restTemplate.postForEntity(
                    baseUrl() + "/api/v1/orders", requests[index], OrderResponse.class);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Then - All orders should be created successfully
        for (ResponseEntity<OrderResponse> response : responses) {
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody().getCorrelationId());
        }
    }

    private CreateOrderRequest createValidOrderRequest() {
        CustomerInfo customerInfo = CustomerInfo.builder()
            .customerId("CUST-001")
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phone("+1234567890")
            .address("123 Main St")
            .city("Anytown")
            .state("CA")
            .zipCode("12345")
            .build();

        OrderItem orderItem = OrderItem.builder()
            .productId("PROD-001")
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(25.99))
            .build();

        return CreateOrderRequest.builder()
            .customerInfo(customerInfo)
            .orderItems(List.of(orderItem))
            .totalAmount(BigDecimal.valueOf(51.98))
            .build();
    }

    private CreateOrderRequest createHighQuantityOrderRequest() {
        CustomerInfo customerInfo = CustomerInfo.builder()
            .customerId("CUST-002")
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .phone("+1234567891")
            .address("456 Oak St")
            .city("Somewhere")
            .state("NY")
            .zipCode("54321")
            .build();

        OrderItem orderItem = OrderItem.builder()
            .productId("PROD-001")
            .quantity(1000) // Very high quantity to trigger rejection
            .unitPrice(BigDecimal.valueOf(25.99))
            .build();

        return CreateOrderRequest.builder()
            .customerInfo(customerInfo)
            .orderItems(List.of(orderItem))
            .totalAmount(BigDecimal.valueOf(25990.00))
            .build();
    }
}