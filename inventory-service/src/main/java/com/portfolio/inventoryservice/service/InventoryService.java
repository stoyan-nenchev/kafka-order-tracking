package com.portfolio.inventoryservice.service;

import com.portfolio.inventoryservice.entity.Product;
import com.portfolio.inventoryservice.entity.StockMovement;
import com.portfolio.inventoryservice.entity.StockMovementType;
import com.portfolio.inventoryservice.exception.InsufficientStockException;
import com.portfolio.inventoryservice.exception.ProductNotFoundException;
import com.portfolio.inventoryservice.repository.ProductRepository;
import com.portfolio.inventoryservice.repository.StockMovementRepository;
import com.portfolio.shared.constants.KafkaTopics;
import com.portfolio.shared.dto.OrderItem;
import com.portfolio.shared.events.OrderConfirmedEvent;
import com.portfolio.shared.events.OrderCreatedEvent;
import com.portfolio.shared.events.OrderRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final EventPublisher eventPublisher;
    
    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Processing order created event for correlation ID: {}", event.getCorrelationId());
        
        try {
            // Check stock availability for all items
            List<OrderItem> orderItems = event.getOrderItems();
            List<String> unavailableProducts = new ArrayList<>();
            
            // First, check if all products exist and have sufficient stock
            for (OrderItem item : orderItems) {
                Product product = productRepository.findByProductId(item.getProductId())
                        .orElse(null);
                
                if (product == null) {
                    unavailableProducts.add(item.getProductId() + " (not found)");
                    continue;
                }
                
                if (!product.canReserve(item.getQuantity())) {
                    int available = product.getStockQuantity() - product.getReservedQuantity();
                    unavailableProducts.add(item.getProductId() + " (requested: " + item.getQuantity() + ", available: " + available + ")");
                }
            }
            
            if (!unavailableProducts.isEmpty()) {
                // Reject the order
                rejectOrder(event, "Insufficient stock for products: " + String.join(", ", unavailableProducts));
                return;
            }
            
            // If all products are available, reserve stock
            for (OrderItem item : orderItems) {
                Product product = productRepository.findByProductIdWithLock(item.getProductId())
                        .orElseThrow(() -> ProductNotFoundException.byProductId(item.getProductId()));
                
                // Double-check availability (race condition protection)
                if (!product.canReserve(item.getQuantity())) {
                    // Release any already reserved stock and reject the order
                    releaseReservedStock(event.getCorrelationId());
                    int available = product.getStockQuantity() - product.getReservedQuantity();
                    rejectOrder(event, "Insufficient stock for product " + item.getProductId() + 
                              ". Requested: " + item.getQuantity() + ", available: " + available);
                    return;
                }
                
                // Reserve the stock
                product.reserveStock(item.getQuantity());
                productRepository.save(product);
                
                // Record stock movement
                StockMovement movement = StockMovement.builder()
                        .productId(item.getProductId())
                        .movementType(StockMovementType.RESERVED)
                        .quantity(item.getQuantity())
                        .referenceId(event.getCorrelationId())
                        .notes("Reserved for order " + event.getOrderId())
                        .build();
                stockMovementRepository.save(movement);
                
                log.info("Reserved {} units of product {} for order correlation ID: {}", 
                        item.getQuantity(), item.getProductId(), event.getCorrelationId());
            }
            
            // Confirm the order
            confirmOrder(event);
            
        } catch (Exception e) {
            log.error("Error processing order created event for correlation ID {}: {}", 
                     event.getCorrelationId(), e.getMessage(), e);
            rejectOrder(event, "Internal error processing order: " + e.getMessage());
        }
    }
    
    private void confirmOrder(OrderCreatedEvent originalEvent) {
        log.info("Confirming order for correlation ID: {}", originalEvent.getCorrelationId());
        
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(originalEvent.getCorrelationId())
                .orderId(originalEvent.getOrderId())
                .timestamp(LocalDateTime.now())
                .customerInfo(originalEvent.getCustomerInfo())
                .orderItems(originalEvent.getOrderItems())
                .totalAmount(originalEvent.getTotalAmount())
                .build();
        
        eventPublisher.publish(KafkaTopics.INVENTORY_EVENTS, event);
        log.info("Order confirmed for correlation ID: {}", originalEvent.getCorrelationId());
    }
    
    private void rejectOrder(OrderCreatedEvent originalEvent, String reason) {
        log.warn("Rejecting order for correlation ID {}: {}", originalEvent.getCorrelationId(), reason);
        
        OrderRejectedEvent event = OrderRejectedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(originalEvent.getCorrelationId())
                .orderId(originalEvent.getOrderId())
                .timestamp(LocalDateTime.now())
                .reason(reason)
                .build();
        
        eventPublisher.publish(KafkaTopics.INVENTORY_EVENTS, event);
        log.info("Order rejected for correlation ID: {}", originalEvent.getCorrelationId());
    }
    
    private void releaseReservedStock(String correlationId) {
        log.info("Releasing reserved stock for correlation ID: {}", correlationId);
        
        List<StockMovement> reservedMovements = stockMovementRepository.findByReferenceId(correlationId);
        
        for (StockMovement movement : reservedMovements) {
            if (movement.getMovementType() == StockMovementType.RESERVED) {
                Product product = productRepository.findByProductIdWithLock(movement.getProductId())
                        .orElse(null);
                
                if (product != null) {
                    product.releaseReservation(movement.getQuantity());
                    productRepository.save(product);
                    
                    // Record the release movement
                    StockMovement releaseMovement = StockMovement.builder()
                            .productId(movement.getProductId())
                            .movementType(StockMovementType.RELEASED)
                            .quantity(movement.getQuantity())
                            .referenceId(correlationId)
                            .notes("Released reservation due to order rejection")
                            .build();
                    stockMovementRepository.save(releaseMovement);
                    
                    log.info("Released {} units of product {} for correlation ID: {}", 
                            movement.getQuantity(), movement.getProductId(), correlationId);
                }
            }
        }
    }
    
    public void confirmStockReservation(String correlationId) {
        log.info("Confirming stock reservation for correlation ID: {}", correlationId);
        
        List<StockMovement> reservedMovements = stockMovementRepository.findByReferenceId(correlationId);
        
        for (StockMovement movement : reservedMovements) {
            if (movement.getMovementType() == StockMovementType.RESERVED) {
                Product product = productRepository.findByProductIdWithLock(movement.getProductId())
                        .orElse(null);
                
                if (product != null) {
                    product.confirmReservation(movement.getQuantity());
                    productRepository.save(product);
                    
                    // Record the confirmation movement
                    StockMovement confirmMovement = StockMovement.builder()
                            .productId(movement.getProductId())
                            .movementType(StockMovementType.CONFIRMED)
                            .quantity(movement.getQuantity())
                            .referenceId(correlationId)
                            .notes("Confirmed stock for shipped order")
                            .build();
                    stockMovementRepository.save(confirmMovement);
                    
                    log.info("Confirmed {} units of product {} for correlation ID: {}", 
                            movement.getQuantity(), movement.getProductId(), correlationId);
                }
            }
        }
    }
}