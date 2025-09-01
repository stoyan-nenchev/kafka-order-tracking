package com.portfolio.notificationservice.repository;

import com.portfolio.notificationservice.entity.NotificationChannel;
import com.portfolio.notificationservice.entity.NotificationTemplate;
import com.portfolio.notificationservice.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    
    Optional<NotificationTemplate> findByTemplateNameAndIsActiveTrue(String templateName);
    
    Optional<NotificationTemplate> findByNotificationTypeAndChannelAndIsActiveTrue(
            NotificationType notificationType, NotificationChannel channel);
    
    List<NotificationTemplate> findByIsActiveTrue();
    
    List<NotificationTemplate> findByNotificationTypeAndIsActiveTrue(NotificationType notificationType);
}