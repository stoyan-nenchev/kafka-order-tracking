package com.portfolio.shared.events;

public class OrderConfirmedEvent extends BaseEvent {
    private String message;
    
    public OrderConfirmedEvent() {
        super();
    }
    
    public OrderConfirmedEvent(Long orderId, String correlationId, String message) {
        super(orderId, correlationId);
        this.message = message;
    }
    
    @Override
    public String getEventType() {
        return "ORDER_CONFIRMED";
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}