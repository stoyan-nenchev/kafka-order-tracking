package com.portfolio.analyticsservice.controller;

import com.portfolio.analyticsservice.entity.DailyMetric;
import com.portfolio.analyticsservice.entity.OrderMetric;
import com.portfolio.analyticsservice.repository.OrderMetricRepository;
import com.portfolio.analyticsservice.service.DailyMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {
    
    private final OrderMetricRepository orderMetricRepository;
    private final DailyMetricsService dailyMetricsService;
    
    @GetMapping("/orders/correlation/{correlationId}")
    public ResponseEntity<OrderMetric> getOrderMetricByCorrelation(@PathVariable String correlationId) {
        log.info("Getting order metrics for correlation ID: {}", correlationId);
        return orderMetricRepository.findByCorrelationId(correlationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderMetric> getOrderMetricByOrderId(@PathVariable String orderId) {
        log.info("Getting order metrics for order ID: {}", orderId);
        return orderMetricRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/orders/customer/{customerId}")
    public ResponseEntity<List<OrderMetric>> getOrderMetricsByCustomer(@PathVariable String customerId) {
        log.info("Getting order metrics for customer ID: {}", customerId);
        List<OrderMetric> metrics = orderMetricRepository.findByCustomerId(customerId);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/orders/status/{status}")
    public ResponseEntity<List<OrderMetric>> getOrderMetricsByStatus(@PathVariable String status) {
        log.info("Getting order metrics for status: {}", status);
        List<OrderMetric> metrics = orderMetricRepository.findByOrderStatus(status.toUpperCase());
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/daily/recent")
    public ResponseEntity<List<DailyMetric>> getRecentDailyMetrics(
            @RequestParam(defaultValue = "30") int days) {
        log.info("Getting recent daily metrics for {} days", days);
        List<DailyMetric> metrics = dailyMetricsService.getRecentMetrics(days);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/daily/range")
    public ResponseEntity<List<DailyMetric>> getDailyMetricsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Getting daily metrics between {} and {}", startDate, endDate);
        List<DailyMetric> metrics = dailyMetricsService.getMetricsBetween(startDate, endDate);
        return ResponseEntity.ok(metrics);
    }
    
    @PostMapping("/daily/generate/{date}")
    public ResponseEntity<String> generateDailyMetricsForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Manually generating daily metrics for date: {}", date);
        dailyMetricsService.generateDailyMetricsForDate(date);
        return ResponseEntity.ok("Daily metrics generated successfully for date: " + date);
    }
}