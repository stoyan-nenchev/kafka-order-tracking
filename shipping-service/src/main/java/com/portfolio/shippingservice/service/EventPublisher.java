package com.portfolio.shippingservice.service;

import com.portfolio.shared.constants.KafkaTopics;
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
    
    public void publish(String topic, Object event) {
        log.debug("Publishing event to topic {}: {}", topic, event);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, event);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic {}: {}", topic, ex.getMessage(), ex);
            } else {
                log.debug("Successfully published event to topic {} with offset: {}", 
                         topic, result.getRecordMetadata().offset());
            }
        });
    }
    
    public void publishShippingEvent(Object event) {
        publish(KafkaTopics.SHIPPING_EVENTS, event);
    }
}