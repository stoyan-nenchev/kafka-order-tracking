-- Notification Service Database Schema
CREATE SCHEMA IF NOT EXISTS notification_service;

-- Notifications table
CREATE TABLE IF NOT EXISTS notification_service.notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_id VARCHAR(255) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    order_id BIGINT NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL, -- 'ORDER_CREATED', 'ORDER_CONFIRMED', 'ORDER_SHIPPED', etc.
    channel VARCHAR(50) NOT NULL DEFAULT 'EMAIL', -- 'EMAIL', 'SMS', 'PUSH'
    subject VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    template_name VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'DELIVERED', 'FAILED'
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failure_reason TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    priority INTEGER NOT NULL DEFAULT 5, -- 1 (highest) to 10 (lowest)
    metadata JSONB, -- Additional data like tracking numbers, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Notification templates table
CREATE TABLE IF NOT EXISTS notification_service.notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL UNIQUE,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    subject_template TEXT NOT NULL,
    content_template TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1
);

-- Notification statistics table
CREATE TABLE IF NOT EXISTS notification_service.notification_stats (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL DEFAULT CURRENT_DATE,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    total_sent INTEGER NOT NULL DEFAULT 0,
    total_delivered INTEGER NOT NULL DEFAULT 0,
    total_failed INTEGER NOT NULL DEFAULT 0,
    avg_delivery_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(date, notification_type, channel)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_notifications_order_id ON notification_service.notifications(order_id);
CREATE INDEX IF NOT EXISTS idx_notifications_customer_id ON notification_service.notifications(customer_id);
CREATE INDEX IF NOT EXISTS idx_notifications_correlation_id ON notification_service.notifications(correlation_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notification_service.notifications(notification_type);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notification_service.notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notification_service.notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_priority ON notification_service.notifications(priority);
CREATE INDEX IF NOT EXISTS idx_templates_name ON notification_service.notification_templates(template_name);
CREATE INDEX IF NOT EXISTS idx_templates_type ON notification_service.notification_templates(notification_type);
CREATE INDEX IF NOT EXISTS idx_stats_date_type ON notification_service.notification_stats(date, notification_type);

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION update_notification_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON notification_service.notifications
    FOR EACH ROW EXECUTE FUNCTION update_notification_updated_at();

CREATE TRIGGER update_templates_updated_at BEFORE UPDATE ON notification_service.notification_templates
    FOR EACH ROW EXECUTE FUNCTION update_notification_updated_at();

CREATE TRIGGER update_stats_updated_at BEFORE UPDATE ON notification_service.notification_stats
    FOR EACH ROW EXECUTE FUNCTION update_notification_updated_at();

-- Insert default notification templates
INSERT INTO notification_service.notification_templates (template_name, notification_type, channel, subject_template, content_template) VALUES
('order_created_email', 'ORDER_CREATED', 'EMAIL', 'Order Confirmation - Order #{orderId}', 
 'Dear {customerName},\n\nThank you for your order! Your order #{orderId} has been created successfully.\n\nOrder Details:\n{orderItems}\n\nTotal Amount: ${totalAmount}\n\nWe will send you updates as your order progresses.\n\nBest regards,\nOrder Tracking Team'),
 
('order_confirmed_email', 'ORDER_CONFIRMED', 'EMAIL', 'Order Confirmed - Order #{orderId}', 
 'Dear {customerName},\n\nGreat news! Your order #{orderId} has been confirmed and is being prepared for shipment.\n\nAll items are in stock and will be processed soon.\n\nBest regards,\nOrder Tracking Team'),
 
('order_rejected_email', 'ORDER_REJECTED', 'EMAIL', 'Order Update - Order #{orderId}', 
 'Dear {customerName},\n\nWe regret to inform you that your order #{orderId} could not be processed.\n\nReason: {reason}\n\nWe apologize for any inconvenience. Please contact customer service for assistance.\n\nBest regards,\nOrder Tracking Team'),
 
('order_shipped_email', 'ORDER_SHIPPED', 'EMAIL', 'Order Shipped - Order #{orderId}', 
 'Dear {customerName},\n\nExcellent news! Your order #{orderId} has been shipped.\n\nShipping Details:\nCarrier: {carrier}\nTracking Number: {trackingNumber}\nEstimated Delivery: {estimatedDelivery}\n\nYou can track your shipment using the tracking number above.\n\nBest regards,\nOrder Tracking Team'),
 
('order_delivered_email', 'ORDER_DELIVERED', 'EMAIL', 'Order Delivered - Order #{orderId}', 
 'Dear {customerName},\n\nYour order #{orderId} has been successfully delivered!\n\nDelivery Details:\nDelivered To: {deliveredTo}\nSigned By: {signedBy}\nDelivery Date: {deliveryDate}\n\nThank you for choosing us. We hope you enjoy your purchase!\n\nBest regards,\nOrder Tracking Team')
ON CONFLICT (template_name) DO NOTHING;

GRANT ALL PRIVILEGES ON SCHEMA notification_service TO notificationuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA notification_service TO notificationuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA notification_service TO notificationuser;