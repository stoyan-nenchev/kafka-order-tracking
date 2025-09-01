package com.portfolio.orderservice.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(String message) {
        super(message);
    }
    
    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static OrderNotFoundException byId(UUID orderId) {
        return new OrderNotFoundException("Order not found with ID: " + orderId);
    }
    
    public static OrderNotFoundException byCorrelationId(String correlationId) {
        return new OrderNotFoundException("Order not found with correlation ID: " + correlationId);
    }
}