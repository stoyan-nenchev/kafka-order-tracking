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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_metrics", schema = "analytics_service", 
       uniqueConstraints = @UniqueConstraint(columnNames = "metric_date"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DailyMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "metric_date", nullable = false, unique = true)
    private LocalDate metricDate;
    
    @Column(name = "total_orders", nullable = false)
    private Long totalOrders;
    
    @Column(name = "confirmed_orders", nullable = false)
    private Long confirmedOrders;
    
    @Column(name = "shipped_orders", nullable = false)
    private Long shippedOrders;
    
    @Column(name = "delivered_orders", nullable = false)
    private Long deliveredOrders;
    
    @Column(name = "rejected_orders", nullable = false)
    private Long rejectedOrders;
    
    @Column(name = "total_revenue", nullable = false)
    private BigDecimal totalRevenue;
    
    @Column(name = "avg_order_value")
    private BigDecimal avgOrderValue;
    
    @Column(name = "avg_processing_time_minutes")
    private BigDecimal avgProcessingTimeMinutes;
    
    @Column(name = "avg_shipping_time_minutes")
    private BigDecimal avgShippingTimeMinutes;
    
    @Column(name = "avg_delivery_time_minutes")
    private BigDecimal avgDeliveryTimeMinutes;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}