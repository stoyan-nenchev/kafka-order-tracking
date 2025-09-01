package com.portfolio.notificationservice.repository;

import com.portfolio.notificationservice.entity.Notification;
import com.portfolio.notificationservice.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByCorrelationId(String correlationId);
    
    List<Notification> findByOrderId(String orderId);
    
    List<Notification> findByCustomerEmail(String customerEmail);
    
    List<Notification> findByStatus(NotificationStatus status);
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.nextRetryAt <= :now")
    List<Notification> findPendingRetries(@Param("status") NotificationStatus status, 
                                         @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.correlationId = :correlationId AND n.status = 'SENT'")
    long countSentNotificationsByCorrelationId(@Param("correlationId") String correlationId);
}