package com.portfolio.shared.constants;

public final class KafkaTopics {
    public static final String ORDERS_EVENTS = "orders.events";
    public static final String INVENTORY_EVENTS = "inventory.events";
    public static final String SHIPPING_EVENTS = "shipping.events";
    public static final String NOTIFICATION_EVENTS = "notification.events";
    
    private KafkaTopics() {
        throw new IllegalStateException("Utility class");
    }
}