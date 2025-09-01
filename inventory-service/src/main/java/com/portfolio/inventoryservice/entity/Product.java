package com.portfolio.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "inventory_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;
    
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;
    
    @Column(name = "available_quantity", insertable = false, updatable = false)
    private Integer availableQuantity;
    
    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel;
    
    @Column(name = "unit_price", precision = 10, scale = 2)
    private java.math.BigDecimal unitPrice;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    public boolean canReserve(Integer quantity) {
        Integer available = stockQuantity - reservedQuantity;
        return available >= quantity;
    }
    
    public void reserveStock(Integer quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Cannot reserve " + quantity + " items. Available: " + (stockQuantity - reservedQuantity));
        }
        this.reservedQuantity += quantity;
    }
    
    public void releaseReservation(Integer quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release " + quantity + " items. Reserved: " + this.reservedQuantity);
        }
        this.reservedQuantity -= quantity;
    }
    
    public void confirmReservation(Integer quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm " + quantity + " items. Reserved: " + this.reservedQuantity);
        }
        this.stockQuantity -= quantity;
        this.reservedQuantity -= quantity;
    }
}