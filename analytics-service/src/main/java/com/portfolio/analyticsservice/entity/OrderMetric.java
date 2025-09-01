package com.portfolio.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_metrics", schema = "analytics_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OrderMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "correlation_id", nullable = false, unique = true)
    private String correlationId;
    
    @Column(name = "order_id", nullable = false)
    private String orderId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "order_total", nullable = false)
    private BigDecimal orderTotal;
    
    @Column(name = "order_status", nullable = false)
    private String orderStatus;
    
    @Column(name = "order_created_at")
    private LocalDateTime orderCreatedAt;
    
    @Column(name = "order_confirmed_at")
    private LocalDateTime orderConfirmedAt;
    
    @Column(name = "order_shipped_at")
    private LocalDateTime orderShippedAt;
    
    @Column(name = "order_delivered_at")
    private LocalDateTime orderDeliveredAt;
    
    @Column(name = "processing_time_minutes")
    private Long processingTimeMinutes;
    
    @Column(name = "shipping_time_minutes")
    private Long shippingTimeMinutes;
    
    @Column(name = "delivery_time_minutes")
    private Long deliveryTimeMinutes;
    
    @Column(name = "total_fulfillment_time_minutes")
    private Long totalFulfillmentTimeMinutes;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
}