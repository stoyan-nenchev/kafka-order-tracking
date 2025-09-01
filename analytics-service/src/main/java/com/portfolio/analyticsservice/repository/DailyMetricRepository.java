package com.portfolio.analyticsservice.repository;

import com.portfolio.analyticsservice.entity.DailyMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyMetricRepository extends JpaRepository<DailyMetric, Long> {
    
    Optional<DailyMetric> findByMetricDate(LocalDate metricDate);
    
    List<DailyMetric> findByMetricDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT dm FROM DailyMetric dm ORDER BY dm.metricDate DESC")
    List<DailyMetric> findAllOrderByDateDesc();
    
    @Query("SELECT dm FROM DailyMetric dm WHERE dm.metricDate >= :startDate ORDER BY dm.metricDate DESC")
    List<DailyMetric> findRecentMetrics(@Param("startDate") LocalDate startDate);
}