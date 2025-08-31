-- Inventory Service Database Schema
CREATE SCHEMA IF NOT EXISTS inventory_service;

-- Products table
CREATE TABLE IF NOT EXISTS inventory_service.products (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255) NOT NULL,
    sku VARCHAR(255) UNIQUE,
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    available_quantity INTEGER GENERATED ALWAYS AS (stock_quantity - reserved_quantity) STORED,
    reorder_level INTEGER NOT NULL DEFAULT 10,
    max_stock_level INTEGER NOT NULL DEFAULT 1000,
    supplier VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Stock movements table for audit trail
CREATE TABLE IF NOT EXISTS inventory_service.stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL REFERENCES inventory_service.products(product_id),
    movement_type VARCHAR(50) NOT NULL, -- 'STOCK_IN', 'STOCK_OUT', 'RESERVED', 'RELEASED'
    quantity INTEGER NOT NULL,
    reference_id VARCHAR(255), -- Order ID or other reference
    reference_type VARCHAR(50), -- 'ORDER', 'ADJUSTMENT', 'SUPPLIER'
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) DEFAULT 'SYSTEM'
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_products_product_id ON inventory_service.products(product_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON inventory_service.products(category);
CREATE INDEX IF NOT EXISTS idx_products_sku ON inventory_service.products(sku);
CREATE INDEX IF NOT EXISTS idx_products_stock_quantity ON inventory_service.products(stock_quantity);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON inventory_service.products(is_active);
CREATE INDEX IF NOT EXISTS idx_stock_movements_product_id ON inventory_service.stock_movements(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_reference_id ON inventory_service.stock_movements(reference_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON inventory_service.stock_movements(created_at);

-- Update trigger for updated_at and version
CREATE OR REPLACE FUNCTION update_product_meta()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_products_meta BEFORE UPDATE ON inventory_service.products
    FOR EACH ROW EXECUTE FUNCTION update_product_meta();

-- Sample data insertion
INSERT INTO inventory_service.products (product_id, product_name, description, category, sku, unit_price, stock_quantity, reorder_level, supplier) VALUES
('LAPTOP001', 'MacBook Pro 16"', 'Apple MacBook Pro 16-inch with M3 chip', 'Electronics', 'MBP-16-M3-001', 2499.99, 50, 5, 'Apple Inc.'),
('PHONE001', 'iPhone 15 Pro', 'Latest iPhone 15 Pro with 256GB storage', 'Electronics', 'IPH-15P-256-001', 1199.99, 100, 10, 'Apple Inc.'),
('BOOK001', 'Spring Boot in Action', 'Comprehensive guide to Spring Boot development', 'Books', 'SBA-2024-001', 49.99, 200, 20, 'Tech Books Publishing'),
('MOUSE001', 'Magic Mouse', 'Apple Magic Mouse with wireless connectivity', 'Accessories', 'MM-WIRELESS-001', 79.99, 150, 15, 'Apple Inc.'),
('KEYBOARD001', 'Magic Keyboard', 'Apple Magic Keyboard with numeric keypad', 'Accessories', 'MK-NUMERIC-001', 129.99, 120, 12, 'Apple Inc.'),
('MONITOR001', 'Studio Display 27"', 'Apple Studio Display 27-inch 5K monitor', 'Electronics', 'SD-27-5K-001', 1599.99, 25, 3, 'Apple Inc.'),
('HEADPHONES001', 'AirPods Pro', 'Apple AirPods Pro with noise cancellation', 'Audio', 'APP-NC-001', 249.99, 300, 30, 'Apple Inc.'),
('TABLET001', 'iPad Air', 'iPad Air with 256GB storage and Wi-Fi', 'Electronics', 'IPA-256-WIFI-001', 749.99, 75, 8, 'Apple Inc.'),
('WATCH001', 'Apple Watch Series 9', 'Apple Watch Series 9 with GPS', 'Wearables', 'AWS9-GPS-001', 399.99, 80, 10, 'Apple Inc.'),
('CHARGER001', 'USB-C Power Adapter', '67W USB-C Power Adapter', 'Accessories', 'USBC-67W-001', 59.99, 200, 20, 'Apple Inc.')
ON CONFLICT (product_id) DO NOTHING;

GRANT ALL PRIVILEGES ON SCHEMA inventory_service TO inventoryuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA inventory_service TO inventoryuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA inventory_service TO inventoryuser;