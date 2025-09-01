package com.portfolio.shippingservice.service;

import com.portfolio.shared.events.OrderConfirmedEvent;
import com.portfolio.shared.events.OrderDeliveredEvent;
import com.portfolio.shared.events.OrderInTransitEvent;
import com.portfolio.shared.events.OrderShippedEvent;
import com.portfolio.shippingservice.dto.ShipmentResponse;
import com.portfolio.shippingservice.entity.Carrier;
import com.portfolio.shippingservice.entity.Shipment;
import com.portfolio.shippingservice.entity.ShipmentStatus;
import com.portfolio.shippingservice.exception.InvalidShipmentStateException;
import com.portfolio.shippingservice.exception.ShipmentNotFoundException;
import com.portfolio.shippingservice.repository.CarrierRepository;
import com.portfolio.shippingservice.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ShippingService {
    
    private final ShipmentRepository shipmentRepository;
    private final CarrierRepository carrierRepository;
    private final EventPublisher eventPublisher;
    
    public ShipmentResponse processOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Processing order confirmed event for correlation ID: {}", event.getCorrelationId());
        
        Shipment shipment = Shipment.builder()
                .correlationId(event.getCorrelationId())
                .orderId(event.getOrderId().toString())
                .trackingNumber(generateTrackingNumber())
                .carrier(selectCarrier())
                .status(ShipmentStatus.PREPARING)
                .shippingAddress(event.getCustomerInfo().getFullAddress())
                .weightKg(calculateWeight(event))
                .shippingCost(calculateShippingCost(event))
                .estimatedDeliveryDate(calculateEstimatedDelivery())
                .build();
        
        shipment = shipmentRepository.save(shipment);
        log.info("Shipment created with ID: {} for order: {}", shipment.getId(), event.getOrderId());
        
        return ShipmentResponse.from(shipment);
    }
    
    public ShipmentResponse shipOrder(String correlationId) {
        log.info("Shipping order with correlation ID: {}", correlationId);
        
        Shipment shipment = shipmentRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> ShipmentNotFoundException.byCorrelationId(correlationId));
        
        if (shipment.getStatus() != ShipmentStatus.PREPARING) {
            throw new InvalidShipmentStateException(
                    "Cannot ship order in status: " + shipment.getStatus());
        }
        
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setShippedAt(LocalDateTime.now());
        shipment = shipmentRepository.save(shipment);
        
        // Publish OrderShippedEvent
        OrderShippedEvent shippedEvent = OrderShippedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .orderId(UUID.fromString(shipment.getOrderId()))
                .timestamp(LocalDateTime.now())
                .shipmentId(UUID.fromString(shipment.getId().toString()))
                .trackingNumber(shipment.getTrackingNumber())
                .carrier(shipment.getCarrier())
                .shippingAddress(shipment.getShippingAddress())
                .build();
        
        eventPublisher.publishShippingEvent(shippedEvent);
        log.info("Order shipped successfully: {}", correlationId);
        
        return ShipmentResponse.from(shipment);
    }
    
    public ShipmentResponse updateToInTransit(String trackingNumber) {
        log.info("Updating shipment to in-transit: {}", trackingNumber);
        
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> ShipmentNotFoundException.byTrackingNumber(trackingNumber));
        
        if (shipment.getStatus() != ShipmentStatus.SHIPPED) {
            throw new InvalidShipmentStateException(
                    "Cannot update to in-transit from status: " + shipment.getStatus());
        }
        
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment = shipmentRepository.save(shipment);
        
        // Publish OrderInTransitEvent
        OrderInTransitEvent inTransitEvent = OrderInTransitEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(shipment.getCorrelationId())
                .orderId(UUID.fromString(shipment.getOrderId()))
                .timestamp(LocalDateTime.now())
                .trackingNumber(trackingNumber)
                .currentLocation("Distribution Center")
                .estimatedDelivery(shipment.getEstimatedDeliveryDate().toString())
                .build();
        
        eventPublisher.publishShippingEvent(inTransitEvent);
        log.info("Shipment updated to in-transit: {}", trackingNumber);
        
        return ShipmentResponse.from(shipment);
    }
    
    public ShipmentResponse markAsDelivered(String trackingNumber) {
        log.info("Marking shipment as delivered: {}", trackingNumber);
        
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> ShipmentNotFoundException.byTrackingNumber(trackingNumber));
        
        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT && 
            shipment.getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new InvalidShipmentStateException(
                    "Cannot deliver from status: " + shipment.getStatus());
        }
        
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setActualDeliveryDate(LocalDate.now());
        shipment = shipmentRepository.save(shipment);
        
        // Publish OrderDeliveredEvent
        OrderDeliveredEvent deliveredEvent = OrderDeliveredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(shipment.getCorrelationId())
                .orderId(UUID.fromString(shipment.getOrderId()))
                .timestamp(LocalDateTime.now())
                .trackingNumber(trackingNumber)
                .actualDeliveryDate(LocalDate.now())
                .build();
        
        eventPublisher.publishShippingEvent(deliveredEvent);
        log.info("Shipment delivered successfully: {}", trackingNumber);
        
        return ShipmentResponse.from(shipment);
    }
    
    public ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> ShipmentNotFoundException.byTrackingNumber(trackingNumber));
        return ShipmentResponse.from(shipment);
    }
    
    public ShipmentResponse getShipmentByCorrelationId(String correlationId) {
        Shipment shipment = shipmentRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> ShipmentNotFoundException.byCorrelationId(correlationId));
        return ShipmentResponse.from(shipment);
    }
    
    private String generateTrackingNumber() {
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String selectCarrier() {
        // Simple carrier selection logic - could be enhanced with business rules
        return carrierRepository.findByIsActiveTrue()
                .stream()
                .findFirst()
                .map(Carrier::getCarrierCode)
                .orElse("FEDEX");
    }
    
    private BigDecimal calculateWeight(OrderConfirmedEvent event) {
        // Simple weight calculation - could be enhanced with actual product weights
        return BigDecimal.valueOf(event.getTotalAmount().doubleValue() * 0.1); // 0.1 kg per dollar
    }
    
    private BigDecimal calculateShippingCost(OrderConfirmedEvent event) {
        // Simple shipping cost calculation
        BigDecimal weight = calculateWeight(event);
        return weight.multiply(BigDecimal.valueOf(5.0)); // $5 per kg
    }
    
    private LocalDate calculateEstimatedDelivery() {
        // Standard delivery time of 5 business days
        return LocalDate.now().plusDays(5);
    }
}