package com.portfolio.shared.events;

public class OrderShippedEvent extends BaseEvent {
    private Long shipmentId;
    private String trackingNumber;
    private String carrier;
    private String shippingAddress;
    
    public OrderShippedEvent() {
        super();
    }
    
    public OrderShippedEvent(Long orderId, String correlationId, Long shipmentId, 
                           String trackingNumber, String carrier, String shippingAddress) {
        super(orderId, correlationId);
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.shippingAddress = shippingAddress;
    }
    
    @Override
    public String getEventType() {
        return "ORDER_SHIPPED";
    }
    
    public Long getShipmentId() {
        return shipmentId;
    }
    
    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public String getCarrier() {
        return carrier;
    }
    
    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}