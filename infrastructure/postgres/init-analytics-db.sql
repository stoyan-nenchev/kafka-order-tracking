-- Analytics Service Database Schema
CREATE SCHEMA IF NOT EXISTS analytics_service;

-- Order metrics table
CREATE TABLE IF NOT EXISTS analytics_service.order_metrics (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    order_total DECIMAL(10,2) NOT NULL,
    order_item_count INTEGER NOT NULL,
    order_created_at TIMESTAMP NOT NULL,
    order_confirmed_at TIMESTAMP,
    order_shipped_at TIMESTAMP,
    order_delivered_at TIMESTAMP,
    order_rejected_at TIMESTAMP,
    rejection_reason TEXT,
    processing_time_minutes INTEGER, -- Time from created to confirmed
    shipping_time_minutes INTEGER,   -- Time from confirmed to shipped
    delivery_time_minutes INTEGER,   -- Time from shipped to delivered
    total_fulfillment_time_minutes INTEGER, -- Time from created to delivered
    carrier VARCHAR(255),
    shipping_method VARCHAR(100),
    customer_city VARCHAR(255),
    customer_state VARCHAR(255),
    customer_country VARCHAR(255) DEFAULT 'USA',
    order_status VARCHAR(50) NOT NULL,
    is_completed BOOLEAN GENERATED ALWAYS AS (order_status = 'DELIVERED') STORED,
    is_stuck BOOLEAN NOT NULL DEFAULT false, -- Orders taking too long
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Daily aggregated metrics
CREATE TABLE IF NOT EXISTS analytics_service.daily_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_date DATE NOT NULL,
    total_orders INTEGER NOT NULL DEFAULT 0,
    total_revenue DECIMAL(12,2) NOT NULL DEFAULT 0,
    orders_created INTEGER NOT NULL DEFAULT 0,
    orders_confirmed INTEGER NOT NULL DEFAULT 0,
    orders_shipped INTEGER NOT NULL DEFAULT 0,
    orders_delivered INTEGER NOT NULL DEFAULT 0,
    orders_rejected INTEGER NOT NULL DEFAULT 0,
    avg_processing_time_minutes DECIMAL(8,2),
    avg_shipping_time_minutes DECIMAL(8,2),
    avg_delivery_time_minutes DECIMAL(8,2),
    avg_fulfillment_time_minutes DECIMAL(8,2),
    stuck_orders_count INTEGER NOT NULL DEFAULT 0,
    completion_rate DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE 
            WHEN total_orders > 0 THEN (orders_delivered::DECIMAL / total_orders * 100)
            ELSE 0 
        END
    ) STORED,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(metric_date)
);

-- Carrier performance metrics
CREATE TABLE IF NOT EXISTS analytics_service.carrier_performance (
    id BIGSERIAL PRIMARY KEY,
    carrier VARCHAR(255) NOT NULL,
    metric_date DATE NOT NULL,
    total_shipments INTEGER NOT NULL DEFAULT 0,
    delivered_shipments INTEGER NOT NULL DEFAULT 0,
    avg_shipping_time_minutes DECIMAL(8,2),
    avg_delivery_time_minutes DECIMAL(8,2),
    on_time_delivery_count INTEGER NOT NULL DEFAULT 0,
    late_delivery_count INTEGER NOT NULL DEFAULT 0,
    delivery_success_rate DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE 
            WHEN total_shipments > 0 THEN (delivered_shipments::DECIMAL / total_shipments * 100)
            ELSE 0 
        END
    ) STORED,
    on_time_rate DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE 
            WHEN delivered_shipments > 0 THEN (on_time_delivery_count::DECIMAL / delivered_shipments * 100)
            ELSE 0 
        END
    ) STORED,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(carrier, metric_date)
);

-- Product performance metrics
CREATE TABLE IF NOT EXISTS analytics_service.product_metrics (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    metric_date DATE NOT NULL,
    total_quantity_sold INTEGER NOT NULL DEFAULT 0,
    total_revenue DECIMAL(10,2) NOT NULL DEFAULT 0,
    avg_unit_price DECIMAL(8,2),
    orders_containing_product INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, metric_date)
);

-- Customer analytics
CREATE TABLE IF NOT EXISTS analytics_service.customer_metrics (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_city VARCHAR(255),
    customer_state VARCHAR(255),
    total_orders INTEGER NOT NULL DEFAULT 0,
    total_spent DECIMAL(12,2) NOT NULL DEFAULT 0,
    avg_order_value DECIMAL(8,2),
    first_order_date TIMESTAMP,
    last_order_date TIMESTAMP,
    avg_days_between_orders DECIMAL(8,2),
    is_repeat_customer BOOLEAN GENERATED ALWAYS AS (total_orders > 1) STORED,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(customer_id)
);

-- System alerts table
CREATE TABLE IF NOT EXISTS analytics_service.system_alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL, -- 'STUCK_ORDER', 'HIGH_REJECTION_RATE', 'CARRIER_DELAY', etc.
    severity VARCHAR(20) NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    related_entity_type VARCHAR(50), -- 'ORDER', 'CARRIER', 'PRODUCT', etc.
    related_entity_id VARCHAR(255),
    is_resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(255),
    resolution_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_order_metrics_order_id ON analytics_service.order_metrics(order_id);
CREATE INDEX IF NOT EXISTS idx_order_metrics_customer_id ON analytics_service.order_metrics(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_metrics_correlation_id ON analytics_service.order_metrics(correlation_id);
CREATE INDEX IF NOT EXISTS idx_order_metrics_status ON analytics_service.order_metrics(order_status);
CREATE INDEX IF NOT EXISTS idx_order_metrics_created_at ON analytics_service.order_metrics(order_created_at);
CREATE INDEX IF NOT EXISTS idx_order_metrics_is_stuck ON analytics_service.order_metrics(is_stuck);
CREATE INDEX IF NOT EXISTS idx_daily_metrics_date ON analytics_service.daily_metrics(metric_date);
CREATE INDEX IF NOT EXISTS idx_carrier_performance_carrier ON analytics_service.carrier_performance(carrier);
CREATE INDEX IF NOT EXISTS idx_carrier_performance_date ON analytics_service.carrier_performance(metric_date);
CREATE INDEX IF NOT EXISTS idx_product_metrics_product_id ON analytics_service.product_metrics(product_id);
CREATE INDEX IF NOT EXISTS idx_product_metrics_date ON analytics_service.product_metrics(metric_date);
CREATE INDEX IF NOT EXISTS idx_customer_metrics_customer_id ON analytics_service.customer_metrics(customer_id);
CREATE INDEX IF NOT EXISTS idx_system_alerts_type ON analytics_service.system_alerts(alert_type);
CREATE INDEX IF NOT EXISTS idx_system_alerts_severity ON analytics_service.system_alerts(severity);
CREATE INDEX IF NOT EXISTS idx_system_alerts_resolved ON analytics_service.system_alerts(is_resolved);

-- Update triggers
CREATE OR REPLACE FUNCTION update_analytics_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_order_metrics_updated_at BEFORE UPDATE ON analytics_service.order_metrics
    FOR EACH ROW EXECUTE FUNCTION update_analytics_updated_at();

CREATE TRIGGER update_daily_metrics_updated_at BEFORE UPDATE ON analytics_service.daily_metrics
    FOR EACH ROW EXECUTE FUNCTION update_analytics_updated_at();

CREATE TRIGGER update_carrier_performance_updated_at BEFORE UPDATE ON analytics_service.carrier_performance
    FOR EACH ROW EXECUTE FUNCTION update_analytics_updated_at();

CREATE TRIGGER update_product_metrics_updated_at BEFORE UPDATE ON analytics_service.product_metrics
    FOR EACH ROW EXECUTE FUNCTION update_analytics_updated_at();

CREATE TRIGGER update_customer_metrics_updated_at BEFORE UPDATE ON analytics_service.customer_metrics
    FOR EACH ROW EXECUTE FUNCTION update_analytics_updated_at();

CREATE TRIGGER update_system_alerts_updated_at BEFORE UPDATE ON analytics_service.system_alerts
    FOR EACH ROW EXECUTE FUNCTION update_analytics_updated_at();

GRANT ALL PRIVILEGES ON SCHEMA analytics_service TO analyticsuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA analytics_service TO analyticsuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA analytics_service TO analyticsuser;