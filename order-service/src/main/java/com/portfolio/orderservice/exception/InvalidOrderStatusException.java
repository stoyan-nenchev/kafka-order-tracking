package com.portfolio.orderservice.exception;

public class InvalidOrderStatusException extends RuntimeException {
    
    public InvalidOrderStatusException(String message) {
        super(message);
    }
    
    public InvalidOrderStatusException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static InvalidOrderStatusException forTransition(String currentStatus, String newStatus) {
        return new InvalidOrderStatusException(
            String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
    }
}