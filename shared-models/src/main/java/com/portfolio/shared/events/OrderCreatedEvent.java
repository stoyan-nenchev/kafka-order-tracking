package com.portfolio.shared.events;

import com.portfolio.shared.dto.CustomerInfo;
import com.portfolio.shared.dto.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends BaseEvent {
    private CustomerInfo customerInfo;
    private List<OrderItem> orderItems;
    private BigDecimal totalAmount;
    private String status;
    
    @Override
    public String getEventType() {
        return "ORDER_CREATED";
    }
}