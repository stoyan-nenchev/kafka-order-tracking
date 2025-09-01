package com.portfolio.inventoryservice.exception;

public class ProductNotFoundException extends RuntimeException {
    
    public ProductNotFoundException(String message) {
        super(message);
    }
    
    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static ProductNotFoundException byProductId(String productId) {
        return new ProductNotFoundException("Product not found with ID: " + productId);
    }
}