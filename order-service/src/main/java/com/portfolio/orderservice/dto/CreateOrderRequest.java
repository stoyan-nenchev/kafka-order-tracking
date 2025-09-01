package com.portfolio.orderservice.dto;

import com.portfolio.shared.dto.CustomerInfo;
import com.portfolio.shared.dto.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "Customer information is required")
    @Valid
    private CustomerInfo customerInfo;
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItem> orderItems;
    
    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;
    
    private String notes;
}