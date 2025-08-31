-- Shipping Service Database Schema
CREATE SCHEMA IF NOT EXISTS shipping_service;

-- Shipments table
CREATE TABLE IF NOT EXISTS shipping_service.shipments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    tracking_number VARCHAR(255) NOT NULL UNIQUE,
    carrier VARCHAR(255) NOT NULL DEFAULT 'FedEx',
    shipping_method VARCHAR(100) NOT NULL DEFAULT 'STANDARD',
    status VARCHAR(50) NOT NULL DEFAULT 'PREPARING',
    shipping_address TEXT NOT NULL,
    estimated_delivery_date DATE,
    actual_delivery_date TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_to VARCHAR(255),
    signed_by VARCHAR(255),
    current_location VARCHAR(255),
    weight_kg DECIMAL(8,2),
    dimensions_cm VARCHAR(50), -- "L x W x H"
    shipping_cost DECIMAL(8,2) DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Shipping events table for tracking history
CREATE TABLE IF NOT EXISTS shipping_service.shipping_events (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipping_service.shipments(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL, -- 'CREATED', 'SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'EXCEPTION'
    event_description TEXT NOT NULL,
    location VARCHAR(255),
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Carriers table for carrier information
CREATE TABLE IF NOT EXISTS shipping_service.carriers (
    id BIGSERIAL PRIMARY KEY,
    carrier_code VARCHAR(50) NOT NULL UNIQUE,
    carrier_name VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(20),
    contact_email VARCHAR(255),
    website VARCHAR(255),
    tracking_url_template VARCHAR(500), -- Template with {trackingNumber} placeholder
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_shipments_order_id ON shipping_service.shipments(order_id);
CREATE INDEX IF NOT EXISTS idx_shipments_tracking_number ON shipping_service.shipments(tracking_number);
CREATE INDEX IF NOT EXISTS idx_shipments_status ON shipping_service.shipments(status);
CREATE INDEX IF NOT EXISTS idx_shipments_carrier ON shipping_service.shipments(carrier);
CREATE INDEX IF NOT EXISTS idx_shipments_created_at ON shipping_service.shipments(created_at);
CREATE INDEX IF NOT EXISTS idx_shipping_events_shipment_id ON shipping_service.shipping_events(shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipping_events_event_type ON shipping_service.shipping_events(event_type);
CREATE INDEX IF NOT EXISTS idx_shipping_events_timestamp ON shipping_service.shipping_events(event_timestamp);

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION update_shipment_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_shipments_updated_at BEFORE UPDATE ON shipping_service.shipments
    FOR EACH ROW EXECUTE FUNCTION update_shipment_updated_at();

-- Insert sample carriers
INSERT INTO shipping_service.carriers (carrier_code, carrier_name, contact_phone, contact_email, website, tracking_url_template) VALUES
('FEDEX', 'FedEx Corporation', '1-800-463-3339', 'customer.service@fedex.com', 'https://www.fedex.com', 'https://www.fedex.com/fedextrack/?trknbr={trackingNumber}'),
('UPS', 'United Parcel Service', '1-800-742-5877', 'customer.service@ups.com', 'https://www.ups.com', 'https://www.ups.com/track?tracknum={trackingNumber}'),
('USPS', 'United States Postal Service', '1-800-275-8777', 'customer.service@usps.gov', 'https://www.usps.com', 'https://tools.usps.com/go/TrackConfirmAction?qtc_tLabels1={trackingNumber}'),
('DHL', 'DHL Express', '1-800-225-5345', 'customer.service@dhl.com', 'https://www.dhl.com', 'https://www.dhl.com/en/express/tracking.html?AWB={trackingNumber}'),
('AMAZON', 'Amazon Logistics', '1-888-280-4331', 'customer-service@amazon.com', 'https://amazon.com', 'https://track.amazon.com/tracking/{trackingNumber}')
ON CONFLICT (carrier_code) DO NOTHING;

GRANT ALL PRIVILEGES ON SCHEMA shipping_service TO shippinguser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA shipping_service TO shippinguser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA shipping_service TO shippinguser;