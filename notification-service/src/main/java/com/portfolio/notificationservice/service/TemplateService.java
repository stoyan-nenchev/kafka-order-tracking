package com.portfolio.notificationservice.service;

import com.portfolio.notificationservice.entity.NotificationChannel;
import com.portfolio.notificationservice.entity.NotificationTemplate;
import com.portfolio.notificationservice.entity.NotificationType;
import com.portfolio.notificationservice.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {
    
    private final NotificationTemplateRepository templateRepository;
    
    public String renderSubject(NotificationType type, NotificationChannel channel, Map<String, Object> variables) {
        NotificationTemplate template = getTemplate(type, channel);
        return renderTemplate(template.getSubjectTemplate(), variables);
    }
    
    public String renderContent(NotificationType type, NotificationChannel channel, Map<String, Object> variables) {
        NotificationTemplate template = getTemplate(type, channel);
        return renderTemplate(template.getContentTemplate(), variables);
    }
    
    public NotificationTemplate getTemplate(NotificationType type, NotificationChannel channel) {
        return templateRepository.findByNotificationTypeAndChannelAndIsActiveTrue(type, channel)
                .orElseThrow(() -> new RuntimeException(
                        "No active template found for type: " + type + ", channel: " + channel));
    }
    
    private String renderTemplate(String template, Map<String, Object> variables) {
        String rendered = template;
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            rendered = rendered.replace(placeholder, value);
        }
        
        return rendered;
    }
}