package com.portfolio.analyticsservice.consumer;

import com.portfolio.shared.constants.KafkaTopics;
import com.portfolio.shared.events.*;
import com.portfolio.analyticsservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    
    private final AnalyticsService analyticsService;
    
    @KafkaListener(topics = KafkaTopics.ORDERS_EVENTS, groupId = "analytics-service-group")
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void handleOrderEvents(
            BaseEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header("kafka_receivedPartitionId") int partition) {
        
        log.info("Processing order event: {} from topic: {}, partition: {}", 
                event.getCorrelationId(), topic, partition);
        
        try {
            switch (event.getEventType()) {
                case "ORDER_CREATED":
                    analyticsService.processOrderCreatedEvent((OrderCreatedEvent) event);
                    break;
                default:
                    log.debug("Ignoring event type: {} for correlation ID: {}", 
                             event.getEventType(), event.getCorrelationId());
            }
            
            log.info("Successfully processed order event: {}", event.getCorrelationId());
        } catch (Exception e) {
            log.error("Error processing order event {}: {}", 
                     event.getCorrelationId(), e.getMessage(), e);
            throw e; // Will be retried
        }
    }
    
    @KafkaListener(topics = KafkaTopics.INVENTORY_EVENTS, groupId = "analytics-service-group")
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void handleInventoryEvents(
            BaseEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header("kafka_receivedPartitionId") int partition) {
        
        log.info("Processing inventory event: {} from topic: {}, partition: {}", 
                event.getCorrelationId(), topic, partition);
        
        try {
            switch (event.getEventType()) {
                case "ORDER_CONFIRMED":
                    analyticsService.processOrderConfirmedEvent((OrderConfirmedEvent) event);
                    break;
                case "ORDER_REJECTED":
                    analyticsService.processOrderRejectedEvent((OrderRejectedEvent) event);
                    break;
                default:
                    log.debug("Ignoring event type: {} for correlation ID: {}", 
                             event.getEventType(), event.getCorrelationId());
            }
            
            log.info("Successfully processed inventory event: {}", event.getCorrelationId());
        } catch (Exception e) {
            log.error("Error processing inventory event {}: {}", 
                     event.getCorrelationId(), e.getMessage(), e);
            throw e; // Will be retried
        }
    }
    
    @KafkaListener(topics = KafkaTopics.SHIPPING_EVENTS, groupId = "analytics-service-group")
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void handleShippingEvents(
            BaseEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header("kafka_receivedPartitionId") int partition) {
        
        log.info("Processing shipping event: {} from topic: {}, partition: {}", 
                event.getCorrelationId(), topic, partition);
        
        try {
            switch (event.getEventType()) {
                case "ORDER_SHIPPED":
                    analyticsService.processOrderShippedEvent((OrderShippedEvent) event);
                    break;
                case "ORDER_DELIVERED":
                    analyticsService.processOrderDeliveredEvent((OrderDeliveredEvent) event);
                    break;
                default:
                    log.debug("Ignoring event type: {} for correlation ID: {}", 
                             event.getEventType(), event.getCorrelationId());
            }
            
            log.info("Successfully processed shipping event: {}", event.getCorrelationId());
        } catch (Exception e) {
            log.error("Error processing shipping event {}: {}", 
                     event.getCorrelationId(), e.getMessage(), e);
            throw e; // Will be retried
        }
    }
}