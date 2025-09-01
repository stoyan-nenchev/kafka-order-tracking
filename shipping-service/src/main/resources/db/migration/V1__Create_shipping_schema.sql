-- Create shipping_service schema
CREATE SCHEMA IF NOT EXISTS shipping_service;

-- Create carriers table
CREATE TABLE shipping_service.carriers (
    id BIGSERIAL PRIMARY KEY,
    carrier_code VARCHAR(50) NOT NULL UNIQUE,
    carrier_name VARCHAR(255) NOT NULL,
    tracking_url_template VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create shipments table
CREATE TABLE shipping_service.shipments (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    tracking_number VARCHAR(255) NOT NULL UNIQUE,
    carrier VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PREPARING',
    shipping_address TEXT,
    weight_kg DECIMAL(8,3),
    shipping_cost DECIMAL(10,2),
    shipped_at TIMESTAMP,
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for better performance
CREATE INDEX idx_shipments_correlation_id ON shipping_service.shipments(correlation_id);
CREATE INDEX idx_shipments_order_id ON shipping_service.shipments(order_id);
CREATE INDEX idx_shipments_tracking_number ON shipping_service.shipments(tracking_number);
CREATE INDEX idx_shipments_status ON shipping_service.shipments(status);
CREATE INDEX idx_shipments_carrier ON shipping_service.shipments(carrier);

-- Insert default carriers
INSERT INTO shipping_service.carriers (carrier_code, carrier_name, tracking_url_template, is_active) VALUES
('FEDEX', 'FedEx', 'https://www.fedex.com/apps/fedextrack/?tracknumbers={trackingNumber}', true),
('UPS', 'UPS', 'https://www.ups.com/track?tracknum={trackingNumber}', true),
('DHL', 'DHL', 'https://www.dhl.com/en/express/tracking.html?AWB={trackingNumber}', true),
('USPS', 'USPS', 'https://tools.usps.com/go/TrackConfirmAction?qtc_tLabels1={trackingNumber}', true);