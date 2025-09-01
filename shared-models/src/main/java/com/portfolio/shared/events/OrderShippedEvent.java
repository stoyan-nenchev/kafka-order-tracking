package com.portfolio.shared.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderShippedEvent extends BaseEvent {
    private Long shipmentId;
    private String trackingNumber;
    private String carrier;
    private String shippingAddress;
    
    @Override
    public String getEventType() {
        return "ORDER_SHIPPED";
    }
}