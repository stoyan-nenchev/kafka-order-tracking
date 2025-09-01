package com.portfolio.inventoryservice.controller;

import com.portfolio.inventoryservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    
    private final ProductRepository productRepository;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        
        try {
            // Test database connectivity
            long productCount = productRepository.count();
            
            healthInfo.put("status", "UP");
            healthInfo.put("service", "inventory-service");
            healthInfo.put("timestamp", LocalDateTime.now());
            healthInfo.put("database", "Connected");
            healthInfo.put("productCount", productCount);
            
            return ResponseEntity.ok(healthInfo);
            
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            
            healthInfo.put("status", "DOWN");
            healthInfo.put("service", "inventory-service");
            healthInfo.put("timestamp", LocalDateTime.now());
            healthInfo.put("database", "Disconnected");
            healthInfo.put("error", e.getMessage());
            
            return ResponseEntity.status(503).body(healthInfo);
        }
    }
    
    @GetMapping("/ready")
    public ResponseEntity<String> ready() {
        return ResponseEntity.ok("Inventory Service is ready");
    }
}