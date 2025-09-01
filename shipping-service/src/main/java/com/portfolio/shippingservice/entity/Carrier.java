package com.portfolio.shippingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carriers", schema = "shipping_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Carrier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "carrier_code", nullable = false, unique = true)
    private String carrierCode;
    
    @Column(name = "carrier_name", nullable = false)
    private String carrierName;
    
    @Column(name = "tracking_url_template", length = 500)
    private String trackingUrlTemplate;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}