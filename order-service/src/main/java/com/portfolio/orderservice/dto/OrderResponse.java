package com.portfolio.orderservice.dto;

import com.portfolio.orderservice.entity.Order;
import com.portfolio.shared.dto.CustomerInfo;
import com.portfolio.shared.dto.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private Long id;
    private String correlationId;
    private CustomerInfo customerInfo;
    private List<OrderItem> orderItems;
    private BigDecimal totalAmount;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .correlationId(order.getCorrelationId())
                .customerInfo(order.getCustomerInfo())
                .orderItems(order.getOrderItems())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}