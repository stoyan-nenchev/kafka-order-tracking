package com.portfolio.shippingservice.consumer;

import com.portfolio.shared.constants.KafkaTopics;
import com.portfolio.shared.events.OrderConfirmedEvent;
import com.portfolio.shippingservice.exception.InvalidShipmentStateException;
import com.portfolio.shippingservice.service.ShippingService;
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
    
    private final ShippingService shippingService;
    
    @KafkaListener(topics = KafkaTopics.INVENTORY_EVENTS, groupId = "shipping-service-group")
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        exclude = {InvalidShipmentStateException.class}
    )
    public void handleOrderConfirmedEvent(
            OrderConfirmedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header("kafka_receivedPartitionId") int partition) {
        
        log.info("Processing order confirmed event: {} from topic: {}, partition: {}", 
                event.getCorrelationId(), topic, partition);
        
        try {
            if ("ORDER_CONFIRMED".equals(event.getEventType())) {
                shippingService.processOrderConfirmed(event);
                log.info("Successfully processed order confirmed event: {}", event.getCorrelationId());
            } else {
                log.debug("Ignoring event type: {} for correlation ID: {}", 
                         event.getEventType(), event.getCorrelationId());
            }
        } catch (InvalidShipmentStateException e) {
            log.warn("Business error processing order confirmed event {}: {}", 
                    event.getCorrelationId(), e.getMessage());
            // Don't retry business errors
        } catch (Exception e) {
            log.error("Unexpected error processing order confirmed event {}: {}", 
                     event.getCorrelationId(), e.getMessage(), e);
            throw e; // Will be retried
        }
    }
}