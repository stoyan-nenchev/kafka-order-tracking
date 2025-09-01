package com.portfolio.analyticsservice.repository;

import com.portfolio.analyticsservice.entity.OrderMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderMetricRepository extends JpaRepository<OrderMetric, Long> {
    
    Optional<OrderMetric> findByCorrelationId(String correlationId);
    
    Optional<OrderMetric> findByOrderId(String orderId);
    
    List<OrderMetric> findByCustomerId(String customerId);
    
    List<OrderMetric> findByOrderStatus(String orderStatus);
    
    @Query("SELECT om FROM OrderMetric om WHERE om.orderCreatedAt >= :startDate AND om.orderCreatedAt <= :endDate")
    List<OrderMetric> findByOrderCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT om FROM OrderMetric om WHERE DATE(om.orderCreatedAt) = :date")
    List<OrderMetric> findByOrderCreatedDate(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(om) FROM OrderMetric om WHERE om.orderStatus = :status AND DATE(om.orderCreatedAt) = :date")
    long countByStatusAndDate(@Param("status") String status, @Param("date") LocalDate date);
    
    @Query("SELECT AVG(om.processingTimeMinutes) FROM OrderMetric om WHERE om.processingTimeMinutes IS NOT NULL AND DATE(om.orderCreatedAt) = :date")
    Double findAvgProcessingTimeByDate(@Param("date") LocalDate date);
    
    @Query("SELECT AVG(om.shippingTimeMinutes) FROM OrderMetric om WHERE om.shippingTimeMinutes IS NOT NULL AND DATE(om.orderCreatedAt) = :date")
    Double findAvgShippingTimeByDate(@Param("date") LocalDate date);
    
    @Query("SELECT AVG(om.deliveryTimeMinutes) FROM OrderMetric om WHERE om.deliveryTimeMinutes IS NOT NULL AND DATE(om.orderCreatedAt) = :date")
    Double findAvgDeliveryTimeByDate(@Param("date") LocalDate date);
}