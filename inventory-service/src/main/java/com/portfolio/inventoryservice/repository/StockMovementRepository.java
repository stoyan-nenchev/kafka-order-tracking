package com.portfolio.inventoryservice.repository;

import com.portfolio.inventoryservice.entity.StockMovement;
import com.portfolio.inventoryservice.entity.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    
    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    
    List<StockMovement> findByReferenceId(String referenceId);
    
    List<StockMovement> findByProductIdAndMovementType(String productId, StockMovementType movementType);
    
    @Query("SELECT sm FROM StockMovement sm WHERE sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT sm.productId, SUM(sm.quantity) FROM StockMovement sm " +
           "WHERE sm.movementType = :movementType AND sm.createdAt >= :since " +
           "GROUP BY sm.productId")
    List<Object[]> getMovementSummary(@Param("movementType") StockMovementType movementType,
                                     @Param("since") LocalDateTime since);
}