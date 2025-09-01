package com.portfolio.orderservice.service;

import com.portfolio.orderservice.dto.CreateOrderRequest;
import com.portfolio.orderservice.dto.OrderResponse;
import com.portfolio.orderservice.entity.Order;
import com.portfolio.orderservice.entity.OrderStatus;
import com.portfolio.orderservice.exception.OrderNotFoundException;
import com.portfolio.orderservice.repository.OrderRepository;
import com.portfolio.shared.dto.CustomerInfo;
import com.portfolio.shared.dto.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private OrderService orderService;
    
    private CreateOrderRequest validOrderRequest;
    private Order savedOrder;
    
    @BeforeEach
    void setUp() {
        CustomerInfo customerInfo = CustomerInfo.builder()
                .customerId("CUST001")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("555-1234")
                .address("123 Main St")
                .build();
        
        OrderItem orderItem = OrderItem.builder()
                .productId("PROD001")
                .productName("Test Product")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(50.00))
                .build();
        
        validOrderRequest = CreateOrderRequest.builder()
                .customerInfo(customerInfo)
                .orderItems(List.of(orderItem))
                .totalAmount(BigDecimal.valueOf(100.00))
                .notes("Test order")
                .build();
        
        savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .correlationId("test-correlation-id")
                .customerInfo(customerInfo)
                .orderItems(List.of(orderItem))
                .totalAmount(BigDecimal.valueOf(100.00))
                .status(OrderStatus.CREATED)
                .notes("Test order")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void createOrder_ValidRequest_ShouldReturnOrderResponse() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        
        // When
        OrderResponse response = orderService.createOrder(validOrderRequest, "test-correlation-id");
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("test-correlation-id", response.getCorrelationId());
        assertEquals(BigDecimal.valueOf(100.00), response.getTotalAmount());
        assertEquals("CREATED", response.getStatus());
        
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publish(anyString(), any());
    }
    
    @Test
    void createOrder_InvalidTotalAmount_ShouldThrowException() {
        // Given
        validOrderRequest.setTotalAmount(BigDecimal.valueOf(50.00)); // Wrong total
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> orderService.createOrder(validOrderRequest, "test-correlation-id"));
        
        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publish(anyString(), any());
    }
    
    @Test
    void getOrder_ExistingOrder_ShouldReturnOrderResponse() {
        // Given
        UUID orderId = savedOrder.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(savedOrder));
        
        // When
        OrderResponse response = orderService.getOrder(orderId);
        
        // Then
        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals("test-correlation-id", response.getCorrelationId());
    }
    
    @Test
    void getOrder_NonExistingOrder_ShouldThrowOrderNotFoundException() {
        // Given
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(OrderNotFoundException.class, 
                () -> orderService.getOrder(orderId));
    }
    
    @Test
    void getOrderByCorrelationId_ExistingOrder_ShouldReturnOrderResponse() {
        // Given
        when(orderRepository.findByCorrelationId("test-correlation-id"))
                .thenReturn(Optional.of(savedOrder));
        
        // When
        OrderResponse response = orderService.getOrderByCorrelationId("test-correlation-id");
        
        // Then
        assertNotNull(response);
        assertEquals("test-correlation-id", response.getCorrelationId());
    }
    
    @Test
    void createOrder_ZeroQuantity_ShouldThrowException() {
        // Given
        validOrderRequest.getOrderItems().getFirst().setQuantity(0);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> orderService.createOrder(validOrderRequest, "test-correlation-id"));
    }
    
    @Test
    void createOrder_NegativePrice_ShouldThrowException() {
        // Given
        validOrderRequest.getOrderItems().getFirst().setUnitPrice(BigDecimal.valueOf(-10.00));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> orderService.createOrder(validOrderRequest, "test-correlation-id"));
    }
}