-- Create analytics_service schema
CREATE SCHEMA IF NOT EXISTS analytics_service;

-- Create order_metrics table
CREATE TABLE analytics_service.order_metrics (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    order_total DECIMAL(10,2) NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    order_created_at TIMESTAMP,
    order_confirmed_at TIMESTAMP,
    order_shipped_at TIMESTAMP,
    order_delivered_at TIMESTAMP,
    processing_time_minutes BIGINT,
    shipping_time_minutes BIGINT,
    delivery_time_minutes BIGINT,
    total_fulfillment_time_minutes BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create daily_metrics table
CREATE TABLE analytics_service.daily_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_date DATE NOT NULL UNIQUE,
    total_orders BIGINT NOT NULL DEFAULT 0,
    confirmed_orders BIGINT NOT NULL DEFAULT 0,
    shipped_orders BIGINT NOT NULL DEFAULT 0,
    delivered_orders BIGINT NOT NULL DEFAULT 0,
    rejected_orders BIGINT NOT NULL DEFAULT 0,
    total_revenue DECIMAL(12,2) NOT NULL DEFAULT 0,
    avg_order_value DECIMAL(10,2),
    avg_processing_time_minutes DECIMAL(8,2),
    avg_shipping_time_minutes DECIMAL(8,2),
    avg_delivery_time_minutes DECIMAL(8,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_order_metrics_correlation_id ON analytics_service.order_metrics(correlation_id);
CREATE INDEX idx_order_metrics_order_id ON analytics_service.order_metrics(order_id);
CREATE INDEX idx_order_metrics_customer_id ON analytics_service.order_metrics(customer_id);
CREATE INDEX idx_order_metrics_status ON analytics_service.order_metrics(order_status);
CREATE INDEX idx_order_metrics_created_at ON analytics_service.order_metrics(order_created_at);
CREATE INDEX idx_order_metrics_created_date ON analytics_service.order_metrics(DATE(order_created_at));

CREATE INDEX idx_daily_metrics_date ON analytics_service.daily_metrics(metric_date);
CREATE INDEX idx_daily_metrics_date_desc ON analytics_service.daily_metrics(metric_date DESC);