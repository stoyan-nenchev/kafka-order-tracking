package com.portfolio.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    
    public boolean sendEmail(String to, String subject, String content) {
        // Simulate email sending - in real implementation this would integrate with
        // email service providers like SendGrid, Amazon SES, etc.
        
        log.info("=== EMAIL SIMULATION ===");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Content: {}", content);
        log.info("========================");
        
        // Simulate 95% success rate
        boolean success = Math.random() > 0.05;
        
        if (success) {
            log.info("Email sent successfully to: {}", to);
        } else {
            log.warn("Failed to send email to: {}", to);
        }
        
        return success;
    }
}