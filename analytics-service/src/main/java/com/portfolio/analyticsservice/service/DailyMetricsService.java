package com.portfolio.analyticsservice.service;

import com.portfolio.analyticsservice.entity.DailyMetric;
import com.portfolio.analyticsservice.entity.OrderMetric;
import com.portfolio.analyticsservice.repository.DailyMetricRepository;
import com.portfolio.analyticsservice.repository.OrderMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyMetricsService {
    
    private final DailyMetricRepository dailyMetricRepository;
    private final OrderMetricRepository orderMetricRepository;
    
    @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM every day
    @Transactional
    public void generateDailyMetrics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        generateDailyMetricsForDate(yesterday);
    }
    
    public void generateDailyMetricsForDate(LocalDate date) {
        log.info("Generating daily metrics for date: {}", date);
        
        List<OrderMetric> orders = orderMetricRepository.findByOrderCreatedDate(date);
        
        if (orders.isEmpty()) {
            log.info("No orders found for date: {}", date);
            return;
        }
        
        long totalOrders = orders.size();
        long confirmedOrders = orders.stream().mapToLong(o -> "CONFIRMED".equals(o.getOrderStatus()) || 
                                                              "SHIPPED".equals(o.getOrderStatus()) || 
                                                              "DELIVERED".equals(o.getOrderStatus()) ? 1 : 0).sum();
        long shippedOrders = orders.stream().mapToLong(o -> "SHIPPED".equals(o.getOrderStatus()) || 
                                                            "DELIVERED".equals(o.getOrderStatus()) ? 1 : 0).sum();
        long deliveredOrders = orders.stream().mapToLong(o -> "DELIVERED".equals(o.getOrderStatus()) ? 1 : 0).sum();
        long rejectedOrders = orders.stream().mapToLong(o -> "REJECTED".equals(o.getOrderStatus()) ? 1 : 0).sum();
        
        BigDecimal totalRevenue = orders.stream()
                .map(OrderMetric::getOrderTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgOrderValue = totalOrders > 0 ? 
                totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        Double avgProcessingTime = orderMetricRepository.findAvgProcessingTimeByDate(date);
        Double avgShippingTime = orderMetricRepository.findAvgShippingTimeByDate(date);
        Double avgDeliveryTime = orderMetricRepository.findAvgDeliveryTimeByDate(date);
        
        DailyMetric dailyMetric = DailyMetric.builder()
                .metricDate(date)
                .totalOrders(totalOrders)
                .confirmedOrders(confirmedOrders)
                .shippedOrders(shippedOrders)
                .deliveredOrders(deliveredOrders)
                .rejectedOrders(rejectedOrders)
                .totalRevenue(totalRevenue)
                .avgOrderValue(avgOrderValue)
                .avgProcessingTimeMinutes(avgProcessingTime != null ? BigDecimal.valueOf(avgProcessingTime) : null)
                .avgShippingTimeMinutes(avgShippingTime != null ? BigDecimal.valueOf(avgShippingTime) : null)
                .avgDeliveryTimeMinutes(avgDeliveryTime != null ? BigDecimal.valueOf(avgDeliveryTime) : null)
                .build();
        
        // Update existing or create new
        dailyMetricRepository.findByMetricDate(date)
                .ifPresentOrElse(
                        existing -> {
                            existing.setTotalOrders(totalOrders);
                            existing.setConfirmedOrders(confirmedOrders);
                            existing.setShippedOrders(shippedOrders);
                            existing.setDeliveredOrders(deliveredOrders);
                            existing.setRejectedOrders(rejectedOrders);
                            existing.setTotalRevenue(totalRevenue);
                            existing.setAvgOrderValue(avgOrderValue);
                            existing.setAvgProcessingTimeMinutes(dailyMetric.getAvgProcessingTimeMinutes());
                            existing.setAvgShippingTimeMinutes(dailyMetric.getAvgShippingTimeMinutes());
                            existing.setAvgDeliveryTimeMinutes(dailyMetric.getAvgDeliveryTimeMinutes());
                            dailyMetricRepository.save(existing);
                            log.info("Updated daily metrics for date: {}", date);
                        },
                        () -> {
                            dailyMetricRepository.save(dailyMetric);
                            log.info("Created daily metrics for date: {}", date);
                        }
                );
    }
    
    public List<DailyMetric> getRecentMetrics(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return dailyMetricRepository.findRecentMetrics(startDate);
    }
    
    public List<DailyMetric> getMetricsBetween(LocalDate startDate, LocalDate endDate) {
        return dailyMetricRepository.findByMetricDateBetween(startDate, endDate);
    }
}