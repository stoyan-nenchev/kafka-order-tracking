package com.portfolio.shippingservice.controller;

import com.portfolio.shippingservice.dto.ShipmentResponse;
import com.portfolio.shippingservice.service.ShippingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
@Slf4j
public class ShippingController {
    
    private final ShippingService shippingService;
    
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> getShipmentByTracking(@PathVariable String trackingNumber) {
        log.info("Getting shipment by tracking number: {}", trackingNumber);
        ShipmentResponse response = shippingService.getShipmentByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<ShipmentResponse> getShipmentByCorrelation(@PathVariable String correlationId) {
        log.info("Getting shipment by correlation ID: {}", correlationId);
        ShipmentResponse response = shippingService.getShipmentByCorrelationId(correlationId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/ship/{correlationId}")
    public ResponseEntity<ShipmentResponse> shipOrder(
            @PathVariable String correlationId,
            HttpServletRequest httpRequest) {
        
        String requestCorrelationId = httpRequest.getHeader("X-Correlation-ID");
        if (requestCorrelationId == null) {
            requestCorrelationId = UUID.randomUUID().toString();
        }
        
        log.info("Shipping order with correlation ID: {} (request correlation: {})", 
                correlationId, requestCorrelationId);
        
        ShipmentResponse response = shippingService.shipOrder(correlationId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/in-transit/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> updateToInTransit(@PathVariable String trackingNumber) {
        log.info("Updating shipment to in-transit: {}", trackingNumber);
        ShipmentResponse response = shippingService.updateToInTransit(trackingNumber);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/delivered/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> markAsDelivered(@PathVariable String trackingNumber) {
        log.info("Marking shipment as delivered: {}", trackingNumber);
        ShipmentResponse response = shippingService.markAsDelivered(trackingNumber);
        return ResponseEntity.ok(response);
    }
}