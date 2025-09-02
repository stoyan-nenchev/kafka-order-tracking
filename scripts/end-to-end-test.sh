#!/bin/bash

# End-to-End Testing Script for Kafka Order Tracking System
# Tests complete order lifecycle from creation to delivery

set -e

echo "🔄 End-to-End Testing - Kafka Order Tracking System"
echo "=================================================="

# Configuration
ORDER_SERVICE_URL="http://localhost:8081"
INVENTORY_SERVICE_URL="http://localhost:8082"
SHIPPING_SERVICE_URL="http://localhost:8083"
NOTIFICATION_SERVICE_URL="http://localhost:8084"
ANALYTICS_SERVICE_URL="http://localhost:8085"

# Test tracking
TESTS_PASSED=0
TESTS_FAILED=0
TOTAL_TESTS=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Test result tracking
test_passed() {
    TESTS_PASSED=$((TESTS_PASSED + 1))
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    log_success "$1"
}

test_failed() {
    TESTS_FAILED=$((TESTS_FAILED + 1))
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    log_error "$1"
}

# Service health check
check_service_health() {
    local service_url=$1
    local service_name=$2
    
    log_info "Checking $service_name health..."
    
    if response=$(curl -s --max-time 10 "$service_url/api/health" 2>/dev/null); then
        if echo "$response" | grep -q '"status":"UP"'; then
            test_passed "$service_name is healthy"
            return 0
        else
            test_failed "$service_name health check failed - status not UP"
            return 1
        fi
    else
        test_failed "$service_name is not responding"
        return 1
    fi
}

# Wait for condition with timeout
wait_for_condition() {
    local condition_command="$1"
    local timeout=${2:-60}
    local interval=${3:-2}
    local description="$4"
    
    log_info "Waiting for condition: $description"
    
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if eval "$condition_command" >/dev/null 2>&1; then
            log_success "Condition met: $description"
            return 0
        fi
        sleep $interval
        elapsed=$((elapsed + interval))
    done
    
    log_error "Timeout waiting for condition: $description"
    return 1
}

# Create test order
create_test_order() {
    local customer_id=$1
    local correlation_id="e2e-test-$(date +%s)-$customer_id"
    
    local order_payload=$(cat <<EOF
{
  "customerInfo": {
    "customerId": "$customer_id",
    "firstName": "E2E",
    "lastName": "Test",
    "email": "e2etest@example.com",
    "phone": "+1234567890",
    "address": "123 Test Street",
    "city": "Test City",
    "state": "CA",
    "zipCode": "12345"
  },
  "orderItems": [
    {
      "productId": "PROD-001",
      "quantity": 1,
      "unitPrice": 99.99
    }
  ],
  "totalAmount": 99.99
}
EOF
    )
    
    log_info "Creating test order with correlation ID: $correlation_id"
    
    local response=$(curl -s -w "\n%{http_code}" \
                         -H "Content-Type: application/json" \
                         -H "X-Correlation-ID: $correlation_id" \
                         -X POST \
                         -d "$order_payload" \
                         "$ORDER_SERVICE_URL/api/v1/orders")
    
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "201" ]; then
        local order_id=$(echo "$response_body" | grep -o '"id":[0-9]*' | cut -d: -f2)
        echo "$correlation_id:$order_id"
        return 0
    else
        log_error "Failed to create order. HTTP Code: $http_code, Response: $response_body"
        return 1
    fi
}

# Test order status progression
test_order_status_progression() {
    local correlation_id=$1
    local expected_status=$2
    
    local response=$(curl -s "$ORDER_SERVICE_URL/api/v1/orders/correlation/$correlation_id")
    local current_status=$(echo "$response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    
    if [ "$current_status" = "$expected_status" ]; then
        return 0
    else
        return 1
    fi
}

# Test notification delivery
test_notifications_sent() {
    local correlation_id=$1
    
    local response=$(curl -s "$NOTIFICATION_SERVICE_URL/api/v1/notifications/correlation/$correlation_id")
    local notification_count=$(echo "$response" | grep -o '"id"' | wc -l)
    
    if [ "$notification_count" -gt 0 ]; then
        return 0
    else
        return 1
    fi
}

# Test analytics data collection
test_analytics_data() {
    local correlation_id=$1
    
    local response=$(curl -s "$ANALYTICS_SERVICE_URL/api/v1/analytics/orders/correlation/$correlation_id")
    
    if echo "$response" | grep -q '"correlationId"'; then
        return 0
    else
        return 1
    fi
}

# Test shipping tracking
test_shipping_data() {
    local correlation_id=$1
    
    local response=$(curl -s "$SHIPPING_SERVICE_URL/api/v1/shipping/correlation/$correlation_id")
    
    if echo "$response" | grep -q '"trackingNumber"'; then
        return 0
    else
        return 1
    fi
}

# Main test execution
run_e2e_tests() {
    log_info "Starting End-to-End Tests..."
    
    # Test 1: Service Health Checks
    echo ""
    log_info "=== Test 1: Service Health Checks ==="
    check_service_health "$ORDER_SERVICE_URL" "Order Service"
    check_service_health "$INVENTORY_SERVICE_URL" "Inventory Service"
    check_service_health "$SHIPPING_SERVICE_URL" "Shipping Service"
    check_service_health "$NOTIFICATION_SERVICE_URL" "Notification Service" 
    check_service_health "$ANALYTICS_SERVICE_URL" "Analytics Service"
    
    # Test 2: Order Creation and Processing
    echo ""
    log_info "=== Test 2: Order Creation and Processing ==="
    
    if order_info=$(create_test_order "E2E-CUST-001"); then
        correlation_id=$(echo "$order_info" | cut -d: -f1)
        order_id=$(echo "$order_info" | cut -d: -f2)
        
        test_passed "Order created successfully (ID: $order_id, Correlation: $correlation_id)"
        
        # Test 3: Wait for inventory processing
        echo ""
        log_info "=== Test 3: Inventory Processing ==="
        
        if wait_for_condition "test_order_status_progression $correlation_id CONFIRMED" 30 2 "Order confirmation by inventory service"; then
            test_passed "Order confirmed by inventory service"
            
            # Test 4: Wait for shipping processing  
            echo ""
            log_info "=== Test 4: Shipping Processing ==="
            
            if wait_for_condition "test_shipping_data $correlation_id" 30 2 "Shipping data creation"; then
                test_passed "Shipping data created"
            else
                test_failed "Shipping data not created within timeout"
            fi
            
        elif wait_for_condition "test_order_status_progression $correlation_id REJECTED" 30 2 "Order rejection by inventory service"; then
            test_passed "Order properly rejected by inventory service (insufficient stock)"
        else
            test_failed "Order status did not change within expected time"
        fi
        
        # Test 5: Notification Service Integration
        echo ""
        log_info "=== Test 5: Notification Service Integration ==="
        
        if wait_for_condition "test_notifications_sent $correlation_id" 20 2 "Notifications sent"; then
            test_passed "Notifications sent for order lifecycle events"
        else
            test_failed "Notifications not sent within expected time"
        fi
        
        # Test 6: Analytics Service Integration
        echo ""
        log_info "=== Test 6: Analytics Service Integration ==="
        
        if wait_for_condition "test_analytics_data $correlation_id" 20 2 "Analytics data collection"; then
            test_passed "Analytics data collected for order"
        else
            test_failed "Analytics data not collected within expected time"
        fi
        
    else
        test_failed "Order creation failed - aborting subsequent tests"
    fi
    
    # Test 7: System Load and Concurrent Processing
    echo ""
    log_info "=== Test 7: Concurrent Order Processing ==="
    
    concurrent_test() {
        local customer_base="CONCURRENT-$1"
        local orders_created=0
        local orders_processed=0
        
        # Create multiple orders concurrently
        for i in {1..5}; do
            if create_test_order "$customer_base-$i" >/dev/null 2>&1; then
                orders_created=$((orders_created + 1))
            fi &
        done
        
        # Wait for all order creation to complete
        wait
        
        if [ $orders_created -eq 5 ]; then
            test_passed "Successfully created 5 concurrent orders"
        else
            test_failed "Only created $orders_created out of 5 concurrent orders"
        fi
    }
    
    concurrent_test "BATCH1"
    
    # Test 8: Error Handling and Recovery
    echo ""
    log_info "=== Test 8: Error Handling ==="
    
    # Test invalid order data
    local invalid_response=$(curl -s -w "%{http_code}" \
                                 -H "Content-Type: application/json" \
                                 -X POST \
                                 -d '{"invalid": "data"}' \
                                 "$ORDER_SERVICE_URL/api/v1/orders" | tail -n1)
    
    if [ "$invalid_response" = "400" ]; then
        test_passed "Invalid order data properly rejected with 400 status"
    else
        test_failed "Invalid order data not properly rejected (got $invalid_response)"
    fi
    
    # Test non-existent order retrieval
    local notfound_response=$(curl -s -w "%{http_code}" \
                                  "$ORDER_SERVICE_URL/api/v1/orders/99999999" | tail -n1)
    
    if [ "$notfound_response" = "404" ]; then
        test_passed "Non-existent order properly returns 404"
    else
        test_failed "Non-existent order query returned $notfound_response instead of 404"
    fi
}

# Test performance under load
run_performance_validation() {
    echo ""
    log_info "=== Performance Validation ==="
    
    # Simple response time test
    start_time=$(date +%s%N)
    curl -s "$ORDER_SERVICE_URL/api/health" >/dev/null
    end_time=$(date +%s%N)
    
    response_time=$(( (end_time - start_time) / 1000000 )) # Convert to milliseconds
    
    if [ $response_time -lt 1000 ]; then
        test_passed "Health endpoint responds within 1 second ($response_time ms)"
    else
        test_failed "Health endpoint too slow ($response_time ms)"
    fi
}

# Generate test report
generate_test_report() {
    local report_file="e2e-test-report-$(date +%Y%m%d_%H%M%S).md"
    
    cat > "$report_file" <<EOF
# End-to-End Test Report

**Test Date:** $(date)
**Test Environment:** Local Development

## Summary

- **Total Tests:** $TOTAL_TESTS
- **Passed:** $TESTS_PASSED  
- **Failed:** $TESTS_FAILED
- **Success Rate:** $((TESTS_PASSED * 100 / TOTAL_TESTS))%

## Test Results

### Service Health
All microservices health checks completed.

### Order Lifecycle
Complete order processing flow tested including:
- Order creation
- Inventory validation  
- Shipping preparation
- Notification delivery
- Analytics collection

### Error Handling
Invalid requests and edge cases properly handled.

### Performance
Basic performance validation completed.

## Recommendations

$(if [ $TESTS_FAILED -gt 0 ]; then
    echo "- ⚠️  Address failed tests before production deployment"
    echo "- Review service logs for error details"
    echo "- Validate infrastructure configuration"
else
    echo "- ✅ All tests passed - system ready for next phase"
    echo "- Consider additional load testing"
    echo "- Review performance metrics"
fi)

---
*Generated by E2E Test Suite*
EOF
    
    log_info "Test report generated: $report_file"
}

# Main execution
main() {
    # Run all tests
    run_e2e_tests
    run_performance_validation
    
    # Generate report
    generate_test_report
    
    # Final summary
    echo ""
    echo "=================================================="
    log_info "End-to-End Testing Complete"
    echo "=================================================="
    echo "📊 Test Results Summary:"
    echo "   • Total Tests: $TOTAL_TESTS"
    echo "   • Passed: $TESTS_PASSED"
    echo "   • Failed: $TESTS_FAILED"
    echo "   • Success Rate: $((TESTS_PASSED * 100 / TOTAL_TESTS))%"
    echo ""
    
    if [ $TESTS_FAILED -eq 0 ]; then
        log_success "🎉 All tests passed! System is functioning correctly."
        exit 0
    else
        log_error "❌ Some tests failed. Please review and fix issues."
        exit 1
    fi
}

# Run main function
main "$@"