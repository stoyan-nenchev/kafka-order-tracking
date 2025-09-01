package com.portfolio.shippingservice.dto;

import com.portfolio.shippingservice.entity.Shipment;
import com.portfolio.shippingservice.entity.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    private Long id;
    private String correlationId;
    private String orderId;
    private String trackingNumber;
    private String carrier;
    private ShipmentStatus status;
    private String shippingAddress;
    private BigDecimal weightKg;
    private BigDecimal shippingCost;
    private LocalDateTime shippedAt;
    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static ShipmentResponse from(Shipment shipment) {
        return ShipmentResponse.builder()
                .id(shipment.getId())
                .correlationId(shipment.getCorrelationId())
                .orderId(shipment.getOrderId())
                .trackingNumber(shipment.getTrackingNumber())
                .carrier(shipment.getCarrier())
                .status(shipment.getStatus())
                .shippingAddress(shipment.getShippingAddress())
                .weightKg(shipment.getWeightKg())
                .shippingCost(shipment.getShippingCost())
                .shippedAt(shipment.getShippedAt())
                .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                .actualDeliveryDate(shipment.getActualDeliveryDate())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}