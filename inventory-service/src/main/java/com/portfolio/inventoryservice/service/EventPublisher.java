package com.portfolio.inventoryservice.service;

import com.portfolio.shared.events.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publish(String topic, BaseEvent event) {
        log.debug("Publishing event {} to topic {} with correlation ID: {}", 
                event.getEventType(), topic, event.getCorrelationId());
        
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, event.getCorrelationId(), event);
            
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.info("Successfully published event {} to topic {} with correlation ID: {}", 
                            event.getEventType(), topic, event.getCorrelationId());
                } else {
                    log.error("Failed to publish event {} to topic {} with correlation ID {}: {}", 
                            event.getEventType(), topic, event.getCorrelationId(), throwable.getMessage(), throwable);
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing event {} to topic {} with correlation ID {}: {}", 
                    event.getEventType(), topic, event.getCorrelationId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}