package com.portfolio.shippingservice.repository;

import com.portfolio.shippingservice.entity.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, Long> {
    
    Optional<Carrier> findByCarrierCode(String carrierCode);
    
    List<Carrier> findByIsActiveTrue();
}