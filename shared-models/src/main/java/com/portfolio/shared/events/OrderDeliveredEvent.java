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
public class OrderDeliveredEvent extends BaseEvent {
    private Long shipmentId;
    private String trackingNumber;
    private String deliveredTo;
    private String signedBy;
    
    @Override
    public String getEventType() {
        return "ORDER_DELIVERED";
    }
}