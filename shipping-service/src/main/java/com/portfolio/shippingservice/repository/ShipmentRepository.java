package com.portfolio.shippingservice.repository;

import com.portfolio.shippingservice.entity.Shipment;
import com.portfolio.shippingservice.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    
    Optional<Shipment> findByCorrelationId(String correlationId);
    
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    
    Optional<Shipment> findByOrderId(String orderId);
    
    @Modifying
    @Query("UPDATE Shipment s SET s.status = :status WHERE s.correlationId = :correlationId")
    int updateStatusByCorrelationId(@Param("correlationId") String correlationId, 
                                   @Param("status") ShipmentStatus status);
}