package com.portfolio.notificationservice.service;

import com.portfolio.shared.events.*;
import com.portfolio.notificationservice.entity.*;
import com.portfolio.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final TemplateService templateService;
    private final EmailService emailService;
    
    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Processing order created notification for correlation ID: {}", event.getCorrelationId());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", event.getCustomerInfo().getFirstName() + " " + event.getCustomerInfo().getLastName());
        variables.put("orderId", event.getOrderId());
        variables.put("totalAmount", event.getTotalAmount());
        variables.put("orderItems", event.getOrderItems().size());
        
        createAndSendNotification(
                event.getCorrelationId(),
                event.getOrderId().toString(),
                event.getCustomerInfo().getEmail(),
                NotificationType.ORDER_CREATED,
                variables
        );
    }
    
    public void processOrderConfirmedEvent(OrderConfirmedEvent event) {
        log.info("Processing order confirmed notification for correlation ID: {}", event.getCorrelationId());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("totalAmount", event.getTotalAmount());
        variables.put("estimatedDeliveryDate", "5-7 business days");
        
        createAndSendNotification(
                event.getCorrelationId(),
                event.getOrderId().toString(),
                event.getCustomerInfo().getEmail(),
                NotificationType.ORDER_CONFIRMED,
                variables
        );
    }
    
    public void processOrderShippedEvent(OrderShippedEvent event) {
        log.info("Processing order shipped notification for correlation ID: {}", event.getCorrelationId());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("trackingNumber", event.getTrackingNumber());
        variables.put("carrier", event.getCarrier());
        variables.put("shippingAddress", event.getShippingAddress());
        
        // For shipped events, we need to get customer email from a different source
        // In a real system, this might come from the order service or be included in the event
        String customerEmail = getCustomerEmailForOrder(event.getOrderId().toString());
        
        createAndSendNotification(
                event.getCorrelationId(),
                event.getOrderId().toString(),
                customerEmail,
                NotificationType.ORDER_SHIPPED,
                variables
        );
    }
    
    public void processOrderDeliveredEvent(OrderDeliveredEvent event) {
        log.info("Processing order delivered notification for correlation ID: {}", event.getCorrelationId());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("deliveryDate", java.time.LocalDate.now());
        variables.put("trackingNumber", event.getTrackingNumber());
        
        String customerEmail = getCustomerEmailForOrder(event.getOrderId().toString());
        
        createAndSendNotification(
                event.getCorrelationId(),
                event.getOrderId().toString(),
                customerEmail,
                NotificationType.ORDER_DELIVERED,
                variables
        );
    }
    
    public void processOrderRejectedEvent(OrderRejectedEvent event) {
        log.info("Processing order rejected notification for correlation ID: {}", event.getCorrelationId());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("rejectionReason", event.getReason());
        
        createAndSendNotification(
                event.getCorrelationId(),
                event.getOrderId().toString(),
                event.getCustomerEmail(),
                NotificationType.ORDER_REJECTED,
                variables
        );
    }
    
    private void createAndSendNotification(String correlationId, String orderId, String customerEmail,
                                         NotificationType type, Map<String, Object> variables) {
        
        try {
            // Render templates
            String subject = templateService.renderSubject(type, NotificationChannel.EMAIL, variables);
            String content = templateService.renderContent(type, NotificationChannel.EMAIL, variables);
            
            // Create notification record
            Notification notification = Notification.builder()
                    .correlationId(correlationId)
                    .orderId(orderId)
                    .customerEmail(customerEmail)
                    .notificationType(type)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .subject(subject)
                    .content(content)
                    .retryCount(0)
                    .maxRetries(3)
                    .build();
            
            notification = notificationRepository.save(notification);
            
            // Send notification
            sendNotification(notification);
            
        } catch (Exception e) {
            log.error("Failed to create/send notification for correlation ID {}: {}", 
                     correlationId, e.getMessage(), e);
        }
    }
    
    public void sendNotification(Notification notification) {
        log.info("Sending notification ID: {} to: {}", notification.getId(), notification.getCustomerEmail());
        
        try {
            boolean success = false;
            
            switch (notification.getChannel()) {
                case EMAIL:
                    success = emailService.sendEmail(
                            notification.getCustomerEmail(),
                            notification.getSubject(),
                            notification.getContent()
                    );
                    break;
                default:
                    log.warn("Unsupported notification channel: {}", notification.getChannel());
                    return;
            }
            
            if (success) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                log.info("Notification sent successfully: {}", notification.getId());
            } else {
                handleNotificationFailure(notification, "Failed to send via " + notification.getChannel());
            }
            
        } catch (Exception e) {
            handleNotificationFailure(notification, e.getMessage());
        }
        
        notificationRepository.save(notification);
    }
    
    private void handleNotificationFailure(Notification notification, String errorMessage) {
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setErrorMessage(errorMessage);
        
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            notification.setStatus(NotificationStatus.FAILED);
            log.error("Notification failed permanently after {} retries: {}", 
                     notification.getMaxRetries(), notification.getId());
        } else {
            notification.setStatus(NotificationStatus.RETRY_SCHEDULED);
            notification.setNextRetryAt(LocalDateTime.now().plusMinutes(5 * notification.getRetryCount()));
            log.warn("Notification failed, scheduled for retry {}/{}: {}", 
                    notification.getRetryCount(), notification.getMaxRetries(), notification.getId());
        }
    }
    
    private String getCustomerEmailForOrder(String orderId) {
        // In a real system, this would fetch from order service or a shared database
        // For demo purposes, we'll simulate this
        return "customer-" + orderId + "@example.com";
    }
}