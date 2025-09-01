package com.portfolio.orderservice.repository;

import com.portfolio.orderservice.entity.Order;
import com.portfolio.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    Optional<Order> findByCorrelationId(String correlationId);
    
    @Query("SELECT o FROM Order o WHERE o.customerInfo.customerId = :customerId")
    Page<Order> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);
    
    List<Order> findByStatus(OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :cutoffTime")
    List<Order> findStaleOrders(@Param("status") OrderStatus status, 
                               @Param("cutoffTime") LocalDateTime cutoffTime);
}