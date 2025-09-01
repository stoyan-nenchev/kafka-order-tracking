package com.portfolio.notificationservice.controller;

import com.portfolio.notificationservice.entity.Notification;
import com.portfolio.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationRepository notificationRepository;
    
    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<Notification>> getNotificationsByCorrelation(@PathVariable String correlationId) {
        log.info("Getting notifications for correlation ID: {}", correlationId);
        List<Notification> notifications = notificationRepository.findByCorrelationId(correlationId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Notification>> getNotificationsByOrder(@PathVariable String orderId) {
        log.info("Getting notifications for order ID: {}", orderId);
        List<Notification> notifications = notificationRepository.findByOrderId(orderId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/customer/{email}")
    public ResponseEntity<List<Notification>> getNotificationsByCustomer(@PathVariable String email) {
        log.info("Getting notifications for customer email: {}", email);
        List<Notification> notifications = notificationRepository.findByCustomerEmail(email);
        return ResponseEntity.ok(notifications);
    }
}