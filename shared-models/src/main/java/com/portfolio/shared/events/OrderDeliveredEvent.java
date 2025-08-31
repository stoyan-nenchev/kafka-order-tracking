package com.portfolio.shared.events;

public class OrderDeliveredEvent extends BaseEvent {
    private Long shipmentId;
    private String trackingNumber;
    private String deliveredTo;
    private String signedBy;
    
    public OrderDeliveredEvent() {
        super();
    }
    
    public OrderDeliveredEvent(Long orderId, String correlationId, Long shipmentId, 
                             String trackingNumber, String deliveredTo, String signedBy) {
        super(orderId, correlationId);
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.deliveredTo = deliveredTo;
        this.signedBy = signedBy;
    }
    
    @Override
    public String getEventType() {
        return "ORDER_DELIVERED";
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
    
    public String getDeliveredTo() {
        return deliveredTo;
    }
    
    public void setDeliveredTo(String deliveredTo) {
        this.deliveredTo = deliveredTo;
    }
    
    public String getSignedBy() {
        return signedBy;
    }
    
    public void setSignedBy(String signedBy) {
        this.signedBy = signedBy;
    }
}