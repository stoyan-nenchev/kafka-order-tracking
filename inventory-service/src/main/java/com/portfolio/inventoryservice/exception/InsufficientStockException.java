package com.portfolio.inventoryservice.exception;

public class InsufficientStockException extends RuntimeException {
    
    public InsufficientStockException(String message) {
        super(message);
    }
    
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static InsufficientStockException forProduct(String productId, Integer requested, Integer available) {
        return new InsufficientStockException(
            String.format("Insufficient stock for product %s. Requested: %d, Available: %d", 
                         productId, requested, available));
    }
}