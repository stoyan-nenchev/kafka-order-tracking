-- Order Service Database Schema
CREATE SCHEMA IF NOT EXISTS order_service;

-- Orders table
CREATE TABLE IF NOT EXISTS order_service.orders (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL UNIQUE,
    customer_id VARCHAR(255) NOT NULL,
    customer_first_name VARCHAR(255) NOT NULL,
    customer_last_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(255) NOT NULL,
    customer_address TEXT NOT NULL,
    customer_city VARCHAR(255),
    customer_state VARCHAR(255),
    customer_zip_code VARCHAR(20),
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order items table
CREATE TABLE IF NOT EXISTS order_service.order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES order_service.orders(id) ON DELETE CASCADE,
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price >= 0),
    description TEXT,
    category VARCHAR(255),
    sku VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON order_service.orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON order_service.orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON order_service.orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_correlation_id ON order_service.orders(correlation_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_service.order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_service.order_items(product_id);

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON order_service.orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

GRANT ALL PRIVILEGES ON SCHEMA order_service TO orderuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA order_service TO orderuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA order_service TO orderuser;