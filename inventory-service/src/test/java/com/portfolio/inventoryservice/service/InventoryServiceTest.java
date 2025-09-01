package com.portfolio.inventoryservice.service;

import com.portfolio.inventoryservice.entity.Product;
import com.portfolio.inventoryservice.entity.StockMovement;
import com.portfolio.inventoryservice.entity.StockMovementType;
import com.portfolio.inventoryservice.repository.ProductRepository;
import com.portfolio.inventoryservice.repository.StockMovementRepository;
import com.portfolio.shared.dto.CustomerInfo;
import com.portfolio.shared.dto.OrderItem;
import com.portfolio.shared.events.OrderCreatedEvent;
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
class InventoryServiceTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private StockMovementRepository stockMovementRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private InventoryService inventoryService;
    
    private OrderCreatedEvent orderCreatedEvent;
    private Product product;
    private OrderItem orderItem;
    
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
        
        orderItem = OrderItem.builder()
                .productId("PROD001")
                .productName("Test Product")
                .quantity(5)
                .unitPrice(BigDecimal.valueOf(100.00))
                .build();
        
        orderCreatedEvent = OrderCreatedEvent.builder()
                .eventId("event-001")
                .correlationId("test-correlation-id")
                .orderId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .customerInfo(customerInfo)
                .orderItems(List.of(orderItem))
                .totalAmount(BigDecimal.valueOf(500.00))
                .status("CREATED")
                .build();
        
        product = Product.builder()
                .id(UUID.randomUUID())
                .productId("PROD001")
                .productName("Test Product")
                .stockQuantity(20)
                .reservedQuantity(0)
                .reorderLevel(5)
                .unitPrice(BigDecimal.valueOf(100.00))
                .build();
    }
    
    @Test
    void processOrderCreatedEvent_SufficientStock_ShouldConfirmOrder() {
        // Given
        when(productRepository.findByProductId("PROD001"))
                .thenReturn(Optional.of(product));
        when(productRepository.findByProductIdWithLock("PROD001"))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenReturn(product);
        
        // When
        inventoryService.processOrderCreatedEvent(orderCreatedEvent);
        
        // Then
        verify(productRepository).save(any(Product.class));
        verify(stockMovementRepository).save(any(StockMovement.class));
        verify(eventPublisher).publish(anyString(), any());
        
        assertEquals(5, product.getReservedQuantity());
    }
    
    @Test
    void processOrderCreatedEvent_InsufficientStock_ShouldRejectOrder() {
        // Given
        product.setStockQuantity(3); // Less than required 5
        when(productRepository.findByProductId("PROD001"))
                .thenReturn(Optional.of(product));
        
        // When
        inventoryService.processOrderCreatedEvent(orderCreatedEvent);
        
        // Then
        verify(eventPublisher).publish(anyString(), any()); // Should publish reject event
        verify(productRepository, never()).findByProductIdWithLock(anyString());
        assertEquals(0, product.getReservedQuantity()); // Should not reserve anything
    }
    
    @Test
    void processOrderCreatedEvent_ProductNotFound_ShouldRejectOrder() {
        // Given
        when(productRepository.findByProductId("PROD001"))
                .thenReturn(Optional.empty());
        
        // When
        inventoryService.processOrderCreatedEvent(orderCreatedEvent);
        
        // Then
        verify(eventPublisher).publish(anyString(), any()); // Should publish reject event
        verify(productRepository, never()).findByProductIdWithLock(anyString());
    }
    
    @Test
    void confirmStockReservation_ShouldConfirmReservedStock() {
        // Given
        String correlationId = "test-correlation-id";
        StockMovement reservedMovement = StockMovement.builder()
                .id(UUID.randomUUID())
                .productId("PROD001")
                .movementType(StockMovementType.RESERVED)
                .quantity(5)
                .referenceId(correlationId)
                .build();
        
        when(stockMovementRepository.findByReferenceId(correlationId))
                .thenReturn(List.of(reservedMovement));
        when(productRepository.findByProductIdWithLock("PROD001"))
                .thenReturn(Optional.of(product));
        
        // Set up initial state
        product.setReservedQuantity(5);
        
        // When
        inventoryService.confirmStockReservation(correlationId);
        
        // Then
        verify(productRepository).save(any(Product.class));
        verify(stockMovementRepository).save(any(StockMovement.class));
        
        assertEquals(15, product.getStockQuantity()); // 20 - 5
        assertEquals(0, product.getReservedQuantity()); // 5 - 5
    }
    
    @Test
    void canReserve_SufficientStock_ShouldReturnTrue() {
        // Given
        product.setStockQuantity(20);
        product.setReservedQuantity(5);
        
        // When
        boolean canReserve = product.canReserve(10);
        
        // Then
        assertTrue(canReserve);
    }
    
    @Test
    void canReserve_InsufficientStock_ShouldReturnFalse() {
        // Given
        product.setStockQuantity(20);
        product.setReservedQuantity(18);
        
        // When
        boolean canReserve = product.canReserve(5);
        
        // Then
        assertFalse(canReserve);
    }
    
    @Test
    void reserveStock_ValidQuantity_ShouldIncreaseReserved() {
        // Given
        product.setStockQuantity(20);
        product.setReservedQuantity(5);
        
        // When
        product.reserveStock(3);
        
        // Then
        assertEquals(8, product.getReservedQuantity());
    }
    
    @Test
    void reserveStock_InsufficientStock_ShouldThrowException() {
        // Given
        product.setStockQuantity(20);
        product.setReservedQuantity(18);
        
        // When & Then
        assertThrows(IllegalStateException.class, 
                () -> product.reserveStock(5));
    }
}