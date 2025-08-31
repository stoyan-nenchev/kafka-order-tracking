package com.portfolio.shared.events;

public class OrderInTransitEvent extends BaseEvent {
    private Long shipmentId;
    private String trackingNumber;
    private String currentLocation;
    private String estimatedDelivery;
    
    public OrderInTransitEvent() {
        super();
    }
    
    public OrderInTransitEvent(Long orderId, String correlationId, Long shipmentId, 
                             String trackingNumber, String currentLocation, String estimatedDelivery) {
        super(orderId, correlationId);
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.currentLocation = currentLocation;
        this.estimatedDelivery = estimatedDelivery;
    }
    
    @Override
    public String getEventType() {
        return "ORDER_IN_TRANSIT";
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
    
    public String getCurrentLocation() {
        return currentLocation;
    }
    
    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }
    
    public String getEstimatedDelivery() {
        return estimatedDelivery;
    }
    
    public void setEstimatedDelivery(String estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }
}