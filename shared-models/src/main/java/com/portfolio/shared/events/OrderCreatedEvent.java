package com.portfolio.shared.events;

import com.portfolio.shared.dto.CustomerInfo;
import com.portfolio.shared.dto.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public class OrderCreatedEvent extends BaseEvent {
    private CustomerInfo customerInfo;
    private List<OrderItem> orderItems;
    private BigDecimal totalAmount;
    private String status;
    
    public OrderCreatedEvent() {
        super();
    }
    
    public OrderCreatedEvent(Long orderId, String correlationId, CustomerInfo customerInfo, 
                           List<OrderItem> orderItems, BigDecimal totalAmount) {
        super(orderId, correlationId);
        this.customerInfo = customerInfo;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.status = "CREATED";
    }
    
    @Override
    public String getEventType() {
        return "ORDER_CREATED";
    }
    
    public CustomerInfo getCustomerInfo() {
        return customerInfo;
    }
    
    public void setCustomerInfo(CustomerInfo customerInfo) {
        this.customerInfo = customerInfo;
    }
    
    public List<OrderItem> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}