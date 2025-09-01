package com.portfolio.orderservice.service;

import com.portfolio.orderservice.dto.CreateOrderRequest;
import com.portfolio.orderservice.dto.OrderResponse;
import com.portfolio.orderservice.dto.UpdateOrderStatusRequest;
import com.portfolio.orderservice.entity.Order;
import com.portfolio.orderservice.entity.OrderStatus;
import com.portfolio.orderservice.exception.InvalidOrderStatusException;
import com.portfolio.orderservice.exception.OrderNotFoundException;
import com.portfolio.orderservice.repository.OrderRepository;
import com.portfolio.shared.constants.KafkaTopics;
import com.portfolio.shared.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    
    public OrderResponse createOrder(CreateOrderRequest request, String correlationId) {
        log.debug("Creating order with correlation ID: {}", correlationId);
        
        // Generate correlation ID if not provided
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Validate business rules
        validateOrderRequest(request);
        
        // Create order entity
        Order order = Order.builder()
                .correlationId(correlationId)
                .customerInfo(request.getCustomerInfo())
                .orderItems(request.getOrderItems())
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.CREATED)
                .notes(request.getNotes())
                .build();
        
        // Save to database
        order = orderRepository.save(order);
        log.info("Order created successfully with ID: {} and correlation ID: {}", order.getId(), correlationId);
        
        // Publish event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .orderId(order.getId())
                .timestamp(LocalDateTime.now())
                .customerInfo(request.getCustomerInfo())
                .orderItems(request.getOrderItems())
                .totalAmount(request.getTotalAmount())
                .status("CREATED")
                .build();
        
        eventPublisher.publish(KafkaTopics.ORDERS_EVENTS, event);
        
        return OrderResponse.from(order);
    }
    
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> OrderNotFoundException.byId(orderId));
        return OrderResponse.from(order);
    }
    
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCorrelationId(String correlationId) {
        Order order = orderRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> OrderNotFoundException.byCorrelationId(correlationId));
        return OrderResponse.from(order);
    }
    
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomerId(String customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(OrderResponse::from);
    }
    
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        log.debug("Updating order {} status to: {}", orderId, request.getStatus());
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> OrderNotFoundException.byId(orderId));
        
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderStatusException("Invalid order status: " + request.getStatus());
        }
        
        // Validate status transition
        validateStatusTransition(order.getStatus(), newStatus);
        
        order.setStatus(newStatus);
        if (request.getReason() != null) {
            String currentNotes = order.getNotes() != null ? order.getNotes() : "";
            order.setNotes(currentNotes + "\nStatus updated to " + newStatus + ": " + request.getReason());
        }
        
        order = orderRepository.save(order);
        log.info("Order {} status updated to: {}", orderId, newStatus);
        
        return OrderResponse.from(order);
    }
    
    private void validateOrderRequest(CreateOrderRequest request) {
        // Validate total amount matches order items
        BigDecimal calculatedTotal = request.getOrderItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (calculatedTotal.compareTo(request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Total amount does not match sum of order items");
        }
        
        // Validate all items have positive quantities and prices
        request.getOrderItems().forEach(item -> {
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Order item quantity must be positive");
            }
            if (item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Order item unit price must be positive");
            }
        });
    }
    
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Define allowed transitions
        switch (currentStatus) {
            case CREATED:
                if (newStatus != OrderStatus.CONFIRMED && newStatus != OrderStatus.REJECTED && 
                    newStatus != OrderStatus.CANCELLED) {
                    throw InvalidOrderStatusException.forTransition(currentStatus.name(), newStatus.name());
                }
                break;
            case CONFIRMED:
                if (newStatus != OrderStatus.SHIPPED && newStatus != OrderStatus.CANCELLED) {
                    throw InvalidOrderStatusException.forTransition(currentStatus.name(), newStatus.name());
                }
                break;
            case SHIPPED:
                if (newStatus != OrderStatus.IN_TRANSIT && newStatus != OrderStatus.DELIVERED) {
                    throw InvalidOrderStatusException.forTransition(currentStatus.name(), newStatus.name());
                }
                break;
            case IN_TRANSIT:
                if (newStatus != OrderStatus.DELIVERED) {
                    throw InvalidOrderStatusException.forTransition(currentStatus.name(), newStatus.name());
                }
                break;
            case REJECTED:
            case DELIVERED:
            case CANCELLED:
                throw InvalidOrderStatusException.forTransition(currentStatus.name(), newStatus.name());
        }
    }
}