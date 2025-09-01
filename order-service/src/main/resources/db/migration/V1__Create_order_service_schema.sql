-- Create schema for order service
CREATE SCHEMA IF NOT EXISTS order_service;

-- Create orders table
CREATE TABLE order_service.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    correlation_id VARCHAR(255) NOT NULL UNIQUE,
    customer_info JSONB NOT NULL,
    order_items JSONB NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_orders_correlation_id ON order_service.orders(correlation_id);
CREATE INDEX idx_orders_customer_id ON order_service.orders USING GIN ((customer_info->>'customerId'));
CREATE INDEX idx_orders_status ON order_service.orders(status);
CREATE INDEX idx_orders_created_at ON order_service.orders(created_at);

-- Add check constraints
ALTER TABLE order_service.orders 
ADD CONSTRAINT chk_orders_status 
CHECK (status IN ('CREATED', 'CONFIRMED', 'REJECTED', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED'));

ALTER TABLE order_service.orders 
ADD CONSTRAINT chk_orders_total_amount_positive 
CHECK (total_amount > 0);

-- Create trigger to update updated_at column
CREATE OR REPLACE FUNCTION order_service.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_orders_updated_at
    BEFORE UPDATE ON order_service.orders
    FOR EACH ROW
    EXECUTE FUNCTION order_service.update_updated_at_column();

-- Insert sample data for testing
INSERT INTO order_service.orders (correlation_id, customer_info, order_items, total_amount, status) VALUES
(
    'test-order-001',
    '{"customerId": "CUST001", "customerName": "John Doe", "email": "john.doe@example.com"}',
    '[{"productId": "PROD001", "productName": "Laptop", "quantity": 1, "unitPrice": 999.99}]',
    999.99,
    'CREATED'
),
(
    'test-order-002',
    '{"customerId": "CUST002", "customerName": "Jane Smith", "email": "jane.smith@example.com"}',
    '[{"productId": "PROD002", "productName": "Mouse", "quantity": 2, "unitPrice": 25.50}, {"productId": "PROD003", "productName": "Keyboard", "quantity": 1, "unitPrice": 75.00}]',
    126.00,
    'CREATED'
);