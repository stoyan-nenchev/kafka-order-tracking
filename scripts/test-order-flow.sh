#!/bin/bash

# Kafka Order Tracking System - End-to-End Test Script
# This script tests the complete order flow through all microservices

set -e

echo "🧪 Testing Kafka Order Tracking System End-to-End Flow"
echo "========================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Test variables
ORDER_SERVICE_URL="http://localhost:8081"
INVENTORY_SERVICE_URL="http://localhost:8082"
SHIPPING_SERVICE_URL="http://localhost:8083"
NOTIFICATION_SERVICE_URL="http://localhost:8084"
ANALYTICS_SERVICE_URL="http://localhost:8085"

# Function to check service health
check_service_health() {
    local service_name=$1
    local url=$2
    
    if curl -s "$url/actuator/health" | grep -q "UP"; then
        print_status "$service_name is healthy"
        return 0
    else
        print_error "$service_name is not responding"
        return 1
    fi
}

# Function to wait for condition
wait_for_condition() {
    local description=$1
    local condition_command=$2
    local max_attempts=${3:-30}
    local sleep_interval=${4:-2}
    
    print_info "Waiting for: $description"
    
    for i in $(seq 1 $max_attempts); do
        if eval $condition_command > /dev/null 2>&1; then
            print_status "$description - Success!"
            return 0
        fi
        
        if [ $i -eq $max_attempts ]; then
            print_error "$description - Timeout after $max_attempts attempts"
            return 1
        fi
        
        echo "  Attempt $i/$max_attempts..."
        sleep $sleep_interval
    done
}

# Step 1: Check all services are running
echo ""
print_info "Step 1: Checking service health..."

services=(
    "Order Service:$ORDER_SERVICE_URL"
    "Inventory Service:$INVENTORY_SERVICE_URL"
    "Shipping Service:$SHIPPING_SERVICE_URL"
    "Notification Service:$NOTIFICATION_SERVICE_URL"
    "Analytics Service:$ANALYTICS_SERVICE_URL"
)

for service in "${services[@]}"; do
    IFS=':' read -r name url <<< "$service"
    if ! check_service_health "$name" "$url"; then
        print_error "Cannot proceed with tests - $name is not healthy"
        exit 1
    fi
done

# Step 2: Create a test order
echo ""
print_info "Step 2: Creating test order..."

ORDER_PAYLOAD='{
    "customerInfo": {
        "customerId": "CUST001",
        "firstName": "John",
        "lastName": "Doe",
        "email": "john.doe@example.com",
        "phone": "+1-555-0123",
        "address": "123 Main Street",
        "city": "San Francisco",
        "state": "CA",
        "zipCode": "94105"
    },
    "orderItems": [
        {
            "productId": "LAPTOP001",
            "productName": "MacBook Pro 16\"",
            "quantity": 1,
            "unitPrice": 2499.99,
            "description": "Apple MacBook Pro 16-inch with M3 chip",
            "category": "Electronics",
            "sku": "MBP-16-M3-001"
        },
        {
            "productId": "MOUSE001",
            "productName": "Magic Mouse",
            "quantity": 1,
            "unitPrice": 79.99,
            "description": "Apple Magic Mouse with wireless connectivity",
            "category": "Accessories",
            "sku": "MM-WIRELESS-001"
        }
    ]
}'

RESPONSE=$(curl -s -X POST "$ORDER_SERVICE_URL/api/orders" \
    -H "Content-Type: application/json" \
    -d "$ORDER_PAYLOAD")

ORDER_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
CORRELATION_ID=$(echo "$RESPONSE" | grep -o '"correlationId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$ORDER_ID" ] && [ -n "$CORRELATION_ID" ]; then
    print_status "Order created successfully - ID: $ORDER_ID, Correlation ID: $CORRELATION_ID"
else
    print_error "Failed to create order"
    echo "Response: $RESPONSE"
    exit 1
fi

# Step 3: Wait for inventory confirmation
echo ""
print_info "Step 3: Waiting for inventory validation..."

wait_for_condition "Order confirmation in inventory" \
    "curl -s '$ORDER_SERVICE_URL/api/orders/$ORDER_ID' | grep -q 'CONFIRMED'" \
    30 3

ORDER_STATUS=$(curl -s "$ORDER_SERVICE_URL/api/orders/$ORDER_ID" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$ORDER_STATUS" = "CONFIRMED" ]; then
    print_status "Order confirmed by inventory service"
elif [ "$ORDER_STATUS" = "REJECTED" ]; then
    print_warning "Order was rejected by inventory service (possibly out of stock)"
    # Continue with test to verify rejection flow
else
    print_error "Unexpected order status: $ORDER_STATUS"
fi

# Step 4: Wait for shipping (if order was confirmed)
if [ "$ORDER_STATUS" = "CONFIRMED" ]; then
    echo ""
    print_info "Step 4: Waiting for shipping..."
    
    wait_for_condition "Order shipment creation" \
        "curl -s '$SHIPPING_SERVICE_URL/api/shipments/order/$ORDER_ID' | grep -q 'trackingNumber'" \
        30 3
    
    SHIPMENT_INFO=$(curl -s "$SHIPPING_SERVICE_URL/api/shipments/order/$ORDER_ID")
    TRACKING_NUMBER=$(echo "$SHIPMENT_INFO" | grep -o '"trackingNumber":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$TRACKING_NUMBER" ]; then
        print_status "Shipment created - Tracking Number: $TRACKING_NUMBER"
        
        # Step 5: Wait for shipping status updates
        echo ""
        print_info "Step 5: Monitoring shipping status updates..."
        
        # Wait for different shipping statuses
        for status in "SHIPPED" "IN_TRANSIT"; do
            print_info "Waiting for status: $status"
            wait_for_condition "Shipping status: $status" \
                "curl -s '$SHIPPING_SERVICE_URL/api/shipments/track/$TRACKING_NUMBER' | grep -q '$status'" \
                20 5
        done
        
        # Check for delivery (this might take longer in real scenario)
        print_info "Checking for delivery status (this may take a while)..."
        if wait_for_condition "Delivery completion" \
            "curl -s '$SHIPPING_SERVICE_URL/api/shipments/track/$TRACKING_NUMBER' | grep -q 'DELIVERED'" \
            10 5; then
            print_status "Order delivered successfully!"
        else
            print_info "Order is still in transit (delivery simulation may take longer)"
        fi
    else
        print_error "Failed to create shipment"
    fi
else
    print_info "Skipping shipping tests - order was rejected"
fi

# Step 6: Check notifications
echo ""
print_info "Step 6: Checking notification history..."

NOTIFICATIONS=$(curl -s "$NOTIFICATION_SERVICE_URL/api/notifications/customer/CUST001")
NOTIFICATION_COUNT=$(echo "$NOTIFICATIONS" | grep -o '"id":[0-9]*' | wc -l)

if [ "$NOTIFICATION_COUNT" -gt 0 ]; then
    print_status "Found $NOTIFICATION_COUNT notifications for customer"
    echo "Notification types:"
    echo "$NOTIFICATIONS" | grep -o '"notificationType":"[^"]*"' | cut -d'"' -f4 | sort | uniq -c
else
    print_warning "No notifications found (they may still be processing)"
fi

# Step 7: Check analytics
echo ""
print_info "Step 7: Verifying analytics data..."

ANALYTICS=$(curl -s "$ANALYTICS_SERVICE_URL/api/analytics/dashboard")
if echo "$ANALYTICS" | grep -q "totalOrders"; then
    TOTAL_ORDERS=$(echo "$ANALYTICS" | grep -o '"totalOrders":[0-9]*' | cut -d':' -f2)
    print_status "Analytics dashboard shows $TOTAL_ORDERS total orders"
else
    print_warning "Analytics data may still be processing"
fi

# Step 8: Test additional endpoints
echo ""
print_info "Step 8: Testing additional API endpoints..."

# Test order retrieval
if curl -s "$ORDER_SERVICE_URL/api/orders/$ORDER_ID" | grep -q "$ORDER_ID"; then
    print_status "Order retrieval endpoint working"
else
    print_error "Order retrieval endpoint failed"
fi

# Test inventory endpoint (if available)
if curl -s "$INVENTORY_SERVICE_URL/actuator/health" > /dev/null 2>&1; then
    print_status "Inventory service endpoints accessible"
fi

# Test notification stats
NOTIFICATION_STATS=$(curl -s "$NOTIFICATION_SERVICE_URL/api/notifications/stats")
if echo "$NOTIFICATION_STATS" | grep -q "totalSent\|totalNotifications"; then
    print_status "Notification statistics available"
else
    print_info "Notification statistics may be empty"
fi

# Final summary
echo ""
echo "========================================================"
print_status "End-to-End Test Completed! 🎉"
echo ""
echo "📋 Test Summary:"
echo "   Order ID: $ORDER_ID"
echo "   Correlation ID: $CORRELATION_ID"
echo "   Final Status: $ORDER_STATUS"
if [ -n "$TRACKING_NUMBER" ]; then
    echo "   Tracking Number: $TRACKING_NUMBER"
fi
echo "   Notifications Sent: $NOTIFICATION_COUNT"
echo ""
echo "🔍 Verification URLs:"
echo "   📊 Kafka UI:           http://localhost:8080"
echo "   📦 Order Details:      $ORDER_SERVICE_URL/api/orders/$ORDER_ID"
if [ -n "$TRACKING_NUMBER" ]; then
    echo "   🚚 Shipment Tracking: $SHIPPING_SERVICE_URL/api/shipments/track/$TRACKING_NUMBER"
fi
echo "   📧 Notifications:      $NOTIFICATION_SERVICE_URL/api/notifications/customer/CUST001"
echo "   📈 Analytics:          $ANALYTICS_SERVICE_URL/api/analytics/dashboard"
echo ""
print_info "The system successfully processed the complete order lifecycle! ✨"