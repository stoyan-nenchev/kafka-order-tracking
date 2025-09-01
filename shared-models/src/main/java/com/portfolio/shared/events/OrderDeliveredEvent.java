package com.portfolio.shared.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderDeliveredEvent extends BaseEvent {
    private UUID shipmentId;
    private String trackingNumber;
    private String deliveredTo;
    private String signedBy;
    
    @Override
    public String getEventType() {
        return "ORDER_DELIVERED";
    }
}