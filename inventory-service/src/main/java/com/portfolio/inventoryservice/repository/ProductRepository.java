package com.portfolio.inventoryservice.repository;

import com.portfolio.inventoryservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    Optional<Product> findByProductId(String productId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :productId")
    Optional<Product> findByProductIdWithLock(@Param("productId") String productId);
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity - p.reservedQuantity <= p.reorderLevel")
    List<Product> findLowStockProducts();
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity - p.reservedQuantity >= :minQuantity")
    List<Product> findAvailableProducts(@Param("minQuantity") Integer minQuantity);
    
    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds")
    List<Product> findByProductIds(@Param("productIds") List<String> productIds);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds")
    List<Product> findByProductIdsWithLock(@Param("productIds") List<String> productIds);
}