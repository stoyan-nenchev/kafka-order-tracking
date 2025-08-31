package com.portfolio.shared.events;

public class OrderRejectedEvent extends BaseEvent {
    private String reason;
    
    public OrderRejectedEvent() {
        super();
    }
    
    public OrderRejectedEvent(Long orderId, String correlationId, String reason) {
        super(orderId, correlationId);
        this.reason = reason;
    }
    
    @Override
    public String getEventType() {
        return "ORDER_REJECTED";
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}