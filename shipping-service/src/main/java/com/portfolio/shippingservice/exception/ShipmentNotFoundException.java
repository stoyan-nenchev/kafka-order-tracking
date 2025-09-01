package com.portfolio.shippingservice.exception;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String message) {
        super(message);
    }
    
    public static ShipmentNotFoundException byCorrelationId(String correlationId) {
        return new ShipmentNotFoundException("Shipment not found with correlation ID: " + correlationId);
    }
    
    public static ShipmentNotFoundException byTrackingNumber(String trackingNumber) {
        return new ShipmentNotFoundException("Shipment not found with tracking number: " + trackingNumber);
    }
    
    public static ShipmentNotFoundException byOrderId(Long orderId) {
        return new ShipmentNotFoundException("Shipment not found for order ID: " + orderId);
    }
}