-- Create notification_service schema
CREATE SCHEMA IF NOT EXISTS notification_service;

-- Create notification_templates table
CREATE TABLE notification_service.notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL UNIQUE,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    subject_template TEXT NOT NULL,
    content_template TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create notifications table
CREATE TABLE notification_service.notifications (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    template_name VARCHAR(100),
    subject VARCHAR(255),
    content TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for better performance
CREATE INDEX idx_notifications_correlation_id ON notification_service.notifications(correlation_id);
CREATE INDEX idx_notifications_order_id ON notification_service.notifications(order_id);
CREATE INDEX idx_notifications_customer_email ON notification_service.notifications(customer_email);
CREATE INDEX idx_notifications_status ON notification_service.notifications(status);
CREATE INDEX idx_notifications_type ON notification_service.notifications(notification_type);
CREATE INDEX idx_notifications_next_retry ON notification_service.notifications(next_retry_at);

CREATE INDEX idx_templates_type_channel ON notification_service.notification_templates(notification_type, channel);
CREATE INDEX idx_templates_name ON notification_service.notification_templates(template_name);

-- Insert default notification templates
INSERT INTO notification_service.notification_templates (template_name, notification_type, channel, subject_template, content_template) VALUES
-- Order Created Templates
('order_created_email', 'ORDER_CREATED', 'EMAIL', 
 'Order Confirmation - Order #{orderId}',
 'Dear {customerName},\n\nThank you for your order!\n\nOrder Details:\n- Order ID: #{orderId}\n- Total Amount: ${totalAmount}\n- Number of Items: {orderItems}\n\nWe will notify you once your order is processed and shipped.\n\nThank you for choosing us!\n\nBest regards,\nYour Order Team'),

-- Order Confirmed Templates  
('order_confirmed_email', 'ORDER_CONFIRMED', 'EMAIL',
 'Order Confirmed - Order #{orderId}',
 'Dear Customer,\n\nGreat news! Your order has been confirmed and is being prepared for shipment.\n\nOrder Details:\n- Order ID: #{orderId}\n- Total Amount: ${totalAmount}\n- Estimated Delivery: {estimatedDeliveryDate}\n\nYou will receive another notification once your order has been shipped with tracking information.\n\nThank you!\n\nBest regards,\nYour Order Team'),

-- Order Shipped Templates
('order_shipped_email', 'ORDER_SHIPPED', 'EMAIL',
 'Order Shipped - Order #{orderId}',
 'Dear Customer,\n\nYour order has been shipped!\n\nShipping Details:\n- Order ID: #{orderId}\n- Tracking Number: {trackingNumber}\n- Carrier: {carrier}\n- Shipping Address: {shippingAddress}\n\nYou can track your package using the tracking number provided above.\n\nThank you!\n\nBest regards,\nYour Order Team'),

-- Order Delivered Templates
('order_delivered_email', 'ORDER_DELIVERED', 'EMAIL',
 'Order Delivered - Order #{orderId}',
 'Dear Customer,\n\nYour order has been successfully delivered!\n\nDelivery Details:\n- Order ID: #{orderId}\n- Tracking Number: {trackingNumber}\n- Delivery Date: {deliveryDate}\n\nWe hope you enjoy your purchase. If you have any questions or concerns, please don''t hesitate to contact us.\n\nThank you for choosing us!\n\nBest regards,\nYour Order Team'),

-- Order Rejected Templates
('order_rejected_email', 'ORDER_REJECTED', 'EMAIL',
 'Order Update - Order #{orderId}',
 'Dear Customer,\n\nWe regret to inform you that your order could not be processed.\n\nOrder Details:\n- Order ID: #{orderId}\n- Reason: {rejectionReason}\n\nIf this was due to insufficient inventory, the item may become available again soon. Please check back later or contact our customer service team.\n\nWe apologize for any inconvenience.\n\nBest regards,\nYour Order Team');