package com.portfolio.inventoryservice.consumer;

import com.portfolio.inventoryservice.service.InventoryService;
import com.portfolio.shared.events.OrderCreatedEvent;
import com.portfolio.shared.events.OrderShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    
    private final InventoryService inventoryService;
    
    @KafkaListener(topics = "orders.events", groupId = "inventory-service-group")
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleOrderCreatedEvent(
            OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        
        log.info("Processing order created event: {} from topic: {}, partition: {}", 
                event.getCorrelationId(), topic, partition);
        
        try {
            inventoryService.processOrderCreatedEvent(event);
            log.info("Successfully processed order created event: {}", event.getCorrelationId());
        } catch (Exception e) {
            log.error("Error processing order created event {}: {}", event.getCorrelationId(), e.getMessage(), e);
            throw e; // Will trigger retry
        }
    }
    
    @KafkaListener(topics = "shipping.events", groupId = "inventory-service-group")
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleOrderShippedEvent(
            OrderShippedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        
        log.info("Processing order shipped event: {} from topic: {}, partition: {}", 
                event.getCorrelationId(), topic, partition);
        
        try {
            // When an order is shipped, confirm the stock reservation (convert reserved to sold)
            inventoryService.confirmStockReservation(event.getCorrelationId());
            log.info("Successfully processed order shipped event: {}", event.getCorrelationId());
        } catch (Exception e) {
            log.error("Error processing order shipped event {}: {}", event.getCorrelationId(), e.getMessage(), e);
            throw e; // Will trigger retry
        }
    }
    
    @Recover
    public void recoverOrderEvent(Exception ex, Object event) {
        String correlationId = "unknown";
        if (event instanceof OrderCreatedEvent) {
            correlationId = ((OrderCreatedEvent) event).getCorrelationId();
        } else if (event instanceof OrderShippedEvent) {
            correlationId = ((OrderShippedEvent) event).getCorrelationId();
        }
        
        log.error("Failed to process event after retries: {} - {}", correlationId, ex.getMessage(), ex);
        
        // Here you might want to send to a dead letter queue or alert system
        // For now, we'll just log the failure
    }
}