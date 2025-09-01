-- Create schema for inventory service
CREATE SCHEMA IF NOT EXISTS inventory_service;

-- Create products table
CREATE TABLE inventory_service.products (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    available_quantity INTEGER GENERATED ALWAYS AS (stock_quantity - reserved_quantity) STORED,
    reorder_level INTEGER NOT NULL DEFAULT 10,
    unit_price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create stock movements audit table
CREATE TABLE inventory_service.stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    reference_id VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_products_product_id ON inventory_service.products(product_id);
CREATE INDEX idx_products_available_quantity ON inventory_service.products(available_quantity);
CREATE INDEX idx_products_reorder_level ON inventory_service.products(reorder_level) WHERE available_quantity <= reorder_level;

CREATE INDEX idx_stock_movements_product_id ON inventory_service.stock_movements(product_id);
CREATE INDEX idx_stock_movements_reference_id ON inventory_service.stock_movements(reference_id);
CREATE INDEX idx_stock_movements_type ON inventory_service.stock_movements(movement_type);
CREATE INDEX idx_stock_movements_created_at ON inventory_service.stock_movements(created_at);

-- Add check constraints
ALTER TABLE inventory_service.products 
ADD CONSTRAINT chk_products_stock_quantity_non_negative 
CHECK (stock_quantity >= 0);

ALTER TABLE inventory_service.products 
ADD CONSTRAINT chk_products_reserved_quantity_non_negative 
CHECK (reserved_quantity >= 0);

ALTER TABLE inventory_service.products 
ADD CONSTRAINT chk_products_reserved_not_exceed_stock 
CHECK (reserved_quantity <= stock_quantity);

ALTER TABLE inventory_service.stock_movements 
ADD CONSTRAINT chk_stock_movements_type 
CHECK (movement_type IN ('RESERVED', 'RELEASED', 'CONFIRMED', 'STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT'));

ALTER TABLE inventory_service.stock_movements 
ADD CONSTRAINT chk_stock_movements_quantity_positive 
CHECK (quantity > 0);

-- Create trigger to update updated_at column
CREATE OR REPLACE FUNCTION inventory_service.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_products_updated_at
    BEFORE UPDATE ON inventory_service.products
    FOR EACH ROW
    EXECUTE FUNCTION inventory_service.update_updated_at_column();

-- Create foreign key constraint (optional, for data integrity)
ALTER TABLE inventory_service.stock_movements 
ADD CONSTRAINT fk_stock_movements_product_id 
FOREIGN KEY (product_id) REFERENCES inventory_service.products(product_id);

-- Insert sample products for testing
INSERT INTO inventory_service.products (product_id, product_name, stock_quantity, reserved_quantity, reorder_level, unit_price) VALUES
('PROD001', 'Laptop', 50, 0, 5, 999.99),
('PROD002', 'Mouse', 200, 0, 20, 25.50),
('PROD003', 'Keyboard', 100, 0, 10, 75.00),
('PROD004', 'Monitor', 30, 0, 5, 299.99),
('PROD005', 'Headphones', 75, 0, 10, 149.99);