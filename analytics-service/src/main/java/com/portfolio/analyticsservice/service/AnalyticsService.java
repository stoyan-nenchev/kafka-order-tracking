package com.portfolio.analyticsservice.service;

import com.portfolio.shared.events.*;
import com.portfolio.analyticsservice.entity.OrderMetric;
import com.portfolio.analyticsservice.repository.OrderMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    
    private final OrderMetricRepository orderMetricRepository;
    
    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Processing order created analytics for correlation ID: {}", event.getCorrelationId());
        
        OrderMetric metric = OrderMetric.builder()
                .correlationId(event.getCorrelationId())
                .orderId(event.getOrderId().toString())
                .customerId(event.getCustomerInfo().getCustomerId())
                .orderTotal(event.getTotalAmount())
                .orderStatus("CREATED")
                .orderCreatedAt(event.getTimestamp())
                .build();
        
        orderMetricRepository.save(metric);
        log.info("Order analytics created for correlation ID: {}", event.getCorrelationId());
    }
    
    public void processOrderConfirmedEvent(OrderConfirmedEvent event) {
        log.info("Processing order confirmed analytics for correlation ID: {}", event.getCorrelationId());
        
        OrderMetric metric = orderMetricRepository.findByCorrelationId(event.getCorrelationId())
                .orElseGet(() -> {
                    log.warn("Order metric not found for correlation ID: {}, creating new one", event.getCorrelationId());
                    return OrderMetric.builder()
                            .correlationId(event.getCorrelationId())
                            .orderId(event.getOrderId().toString())
                            .customerId("unknown")
                            .orderTotal(BigDecimal.ZERO)
                            .build();
                });
        
        metric.setOrderStatus("CONFIRMED");
        metric.setOrderConfirmedAt(event.getTimestamp());
        
        if (metric.getOrderCreatedAt() != null) {
            long processingTime = Duration.between(metric.getOrderCreatedAt(), event.getTimestamp()).toMinutes();
            metric.setProcessingTimeMinutes(processingTime);
        }
        
        orderMetricRepository.save(metric);
        log.info("Order analytics updated for confirmation: {}", event.getCorrelationId());
    }
    
    public void processOrderRejectedEvent(OrderRejectedEvent event) {
        log.info("Processing order rejected analytics for correlation ID: {}", event.getCorrelationId());
        
        OrderMetric metric = orderMetricRepository.findByCorrelationId(event.getCorrelationId())
                .orElseGet(() -> {
                    log.warn("Order metric not found for correlation ID: {}, creating new one", event.getCorrelationId());
                    return OrderMetric.builder()
                            .correlationId(event.getCorrelationId())
                            .orderId(event.getOrderId().toString())
                            .customerId("unknown")
                            .orderTotal(BigDecimal.ZERO)
                            .build();
                });
        
        metric.setOrderStatus("REJECTED");
        
        if (metric.getOrderCreatedAt() != null) {
            long processingTime = Duration.between(metric.getOrderCreatedAt(), event.getTimestamp()).toMinutes();
            metric.setProcessingTimeMinutes(processingTime);
        }
        
        orderMetricRepository.save(metric);
        log.info("Order analytics updated for rejection: {}", event.getCorrelationId());
    }
    
    public void processOrderShippedEvent(OrderShippedEvent event) {
        log.info("Processing order shipped analytics for correlation ID: {}", event.getCorrelationId());
        
        OrderMetric metric = orderMetricRepository.findByCorrelationId(event.getCorrelationId())
                .orElseGet(() -> {
                    log.warn("Order metric not found for correlation ID: {}, creating new one", event.getCorrelationId());
                    return OrderMetric.builder()
                            .correlationId(event.getCorrelationId())
                            .orderId(event.getOrderId().toString())
                            .customerId("unknown")
                            .build();
                });
        
        metric.setOrderStatus("SHIPPED");
        metric.setOrderShippedAt(event.getTimestamp());
        
        if (metric.getOrderConfirmedAt() != null) {
            long shippingTime = Duration.between(metric.getOrderConfirmedAt(), event.getTimestamp()).toMinutes();
            metric.setShippingTimeMinutes(shippingTime);
        }
        
        orderMetricRepository.save(metric);
        log.info("Order analytics updated for shipping: {}", event.getCorrelationId());
    }
    
    public void processOrderDeliveredEvent(OrderDeliveredEvent event) {
        log.info("Processing order delivered analytics for correlation ID: {}", event.getCorrelationId());
        
        OrderMetric metric = orderMetricRepository.findByCorrelationId(event.getCorrelationId())
                .orElseGet(() -> {
                    log.warn("Order metric not found for correlation ID: {}, creating new one", event.getCorrelationId());
                    return OrderMetric.builder()
                            .correlationId(event.getCorrelationId())
                            .orderId(event.getOrderId().toString())
                            .customerId("unknown")
                            .build();
                });
        
        metric.setOrderStatus("DELIVERED");
        metric.setOrderDeliveredAt(event.getTimestamp());
        
        if (metric.getOrderShippedAt() != null) {
            long deliveryTime = Duration.between(metric.getOrderShippedAt(), event.getTimestamp()).toMinutes();
            metric.setDeliveryTimeMinutes(deliveryTime);
        }
        
        if (metric.getOrderCreatedAt() != null) {
            long totalFulfillmentTime = Duration.between(metric.getOrderCreatedAt(), event.getTimestamp()).toMinutes();
            metric.setTotalFulfillmentTimeMinutes(totalFulfillmentTime);
        }
        
        orderMetricRepository.save(metric);
        log.info("Order analytics updated for delivery: {}", event.getCorrelationId());
    }
}