#!/bin/bash

# Performance Testing Script for Kafka Order Tracking System
# This script performs load testing and performance monitoring

set -e

echo "🚀 Performance Testing - Kafka Order Tracking System"
echo "================================================="

# Configuration
ORDER_SERVICE_URL="http://localhost:8081"
CONCURRENT_USERS=${1:-10}
TOTAL_REQUESTS=${2:-100}
RAMP_UP_TIME=${3:-30}
TEST_DURATION=${4:-300}

echo "📊 Test Configuration:"
echo "   • Concurrent Users: $CONCURRENT_USERS"
echo "   • Total Requests: $TOTAL_REQUESTS"  
echo "   • Ramp-up Time: ${RAMP_UP_TIME}s"
echo "   • Test Duration: ${TEST_DURATION}s"
echo ""

# Check if services are running
check_service() {
    local service_url=$1
    local service_name=$2
    
    if curl -s --max-time 5 "$service_url/api/health" > /dev/null 2>&1; then
        echo "✅ $service_name is running"
        return 0
    else
        echo "❌ $service_name is not responding"
        return 1
    fi
}

echo "🔍 Checking service availability..."
check_service "$ORDER_SERVICE_URL" "Order Service" || exit 1
check_service "http://localhost:8082" "Inventory Service" || exit 1
check_service "http://localhost:8083" "Shipping Service" || exit 1
check_service "http://localhost:8084" "Notification Service" || exit 1
check_service "http://localhost:8085" "Analytics Service" || exit 1

echo ""
echo "🏃‍♂️ Starting performance tests..."

# Create test data directory
mkdir -p performance-results
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_DIR="performance-results/test_$TIMESTAMP"
mkdir -p "$RESULT_DIR"

# Function to create sample order
create_sample_order() {
    local customer_id=$1
    cat <<EOF
{
  "customerInfo": {
    "customerId": "PERF-CUST-$customer_id",
    "firstName": "Load",
    "lastName": "Test$customer_id",
    "email": "loadtest$customer_id@example.com",
    "phone": "+123456789$customer_id",
    "address": "$customer_id Performance St",
    "city": "Test City",
    "state": "CA",
    "zipCode": "12345"
  },
  "orderItems": [
    {
      "productId": "PERF-PROD-001",
      "quantity": 1,
      "unitPrice": 29.99
    }
  ],
  "totalAmount": 29.99
}
EOF
}

# Test 1: Basic Load Test
echo "📈 Test 1: Basic Load Test"
echo "Creating $TOTAL_REQUESTS orders with $CONCURRENT_USERS concurrent users..."

# Generate test data
for i in $(seq 1 $CONCURRENT_USERS); do
    create_sample_order $i > "$RESULT_DIR/order_$i.json"
done

# Function to send concurrent requests
send_requests() {
    local thread_id=$1
    local requests_per_thread=$((TOTAL_REQUESTS / CONCURRENT_USERS))
    local order_file="$RESULT_DIR/order_$thread_id.json"
    local result_file="$RESULT_DIR/thread_${thread_id}_results.txt"
    
    echo "Thread $thread_id: Sending $requests_per_thread requests..."
    
    for i in $(seq 1 $requests_per_thread); do
        start_time=$(date +%s%N)
        
        response=$(curl -s -w "%{http_code}:%{time_total}" \
                       -H "Content-Type: application/json" \
                       -H "X-Correlation-ID: perf-test-$thread_id-$i" \
                       -X POST \
                       -d @"$order_file" \
                       "$ORDER_SERVICE_URL/api/v1/orders" 2>/dev/null || echo "000:0")
        
        end_time=$(date +%s%N)
        duration=$((($end_time - $start_time) / 1000000)) # Convert to milliseconds
        
        http_code=$(echo $response | cut -d: -f1)
        curl_time=$(echo $response | cut -d: -f2)
        
        echo "$i,$http_code,$duration,$curl_time" >> "$result_file"
        
        # Brief pause to avoid overwhelming
        sleep 0.1
    done
}

# Start concurrent load test
echo "Starting concurrent requests at $(date)"
for i in $(seq 1 $CONCURRENT_USERS); do
    send_requests $i &
done

# Wait for all background jobs to complete
wait

echo "✅ Load test completed at $(date)"

# Test 2: System Resource Monitoring During Load
echo ""
echo "📊 Test 2: System Resource Monitoring"

monitor_resources() {
    local duration=$1
    local interval=5
    local result_file="$RESULT_DIR/resource_monitoring.csv"
    
    echo "timestamp,cpu_usage,memory_usage,docker_containers" > "$result_file"
    
    for i in $(seq 1 $((duration / interval))); do
        timestamp=$(date '+%Y-%m-%d %H:%M:%S')
        
        # Get system CPU usage
        cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//')
        
        # Get system memory usage
        memory_info=$(free | grep Mem)
        memory_total=$(echo $memory_info | awk '{print $2}')
        memory_used=$(echo $memory_info | awk '{print $3}')
        memory_usage=$((memory_used * 100 / memory_total))
        
        # Count running Docker containers
        docker_containers=$(docker ps -q | wc -l)
        
        echo "$timestamp,$cpu_usage,$memory_usage,$docker_containers" >> "$result_file"
        
        sleep $interval
    done
}

echo "Monitoring system resources for ${TEST_DURATION}s..."
monitor_resources $TEST_DURATION &
MONITOR_PID=$!

# Test 3: Stress Test - Increasing Load
echo ""
echo "🔥 Test 3: Stress Test - Ramping up load"

stress_test() {
    local max_concurrent=$1
    local ramp_time=$2
    local step_time=$((ramp_time / max_concurrent))
    
    echo "Ramping up from 1 to $max_concurrent users over ${ramp_time}s"
    
    for concurrent in $(seq 1 $max_concurrent); do
        echo "Adding user $concurrent ($(date))"
        
        # Start a new user
        (
            for req in $(seq 1 10); do
                create_sample_order $concurrent | \
                curl -s -H "Content-Type: application/json" \
                     -H "X-Correlation-ID: stress-$concurrent-$req" \
                     -X POST -d @- \
                     "$ORDER_SERVICE_URL/api/v1/orders" > /dev/null
                sleep 1
            done
        ) &
        
        sleep $step_time
    done
    
    # Wait for all stress test requests to complete
    wait
}

stress_test $CONCURRENT_USERS $RAMP_UP_TIME

# Stop resource monitoring
kill $MONITOR_PID 2>/dev/null || true

# Generate Performance Report
echo ""
echo "📋 Generating Performance Report..."

generate_report() {
    local report_file="$RESULT_DIR/performance_report.md"
    
    cat > "$report_file" <<EOF
# Performance Test Report

**Test Date:** $(date)
**Test Configuration:**
- Concurrent Users: $CONCURRENT_USERS
- Total Requests: $TOTAL_REQUESTS
- Ramp-up Time: ${RAMP_UP_TIME}s
- Test Duration: ${TEST_DURATION}s

## Test Results Summary

### Response Time Analysis
EOF
    
    # Analyze response times
    if ls "$RESULT_DIR"/thread_*_results.txt >/dev/null 2>&1; then
        echo "Analyzing response times..." 
        
        # Combine all thread results
        cat "$RESULT_DIR"/thread_*_results.txt > "$RESULT_DIR/all_results.csv"
        
        # Calculate statistics
        total_requests=$(wc -l < "$RESULT_DIR/all_results.csv")
        successful_requests=$(awk -F, '$2 >= 200 && $2 < 300 {count++} END {print count+0}' "$RESULT_DIR/all_results.csv")
        error_requests=$((total_requests - successful_requests))
        success_rate=$((successful_requests * 100 / total_requests))
        
        avg_response_time=$(awk -F, '{sum+=$3; count++} END {print sum/count}' "$RESULT_DIR/all_results.csv")
        
        cat >> "$report_file" <<EOF

- **Total Requests:** $total_requests
- **Successful Requests:** $successful_requests
- **Failed Requests:** $error_requests  
- **Success Rate:** ${success_rate}%
- **Average Response Time:** ${avg_response_time}ms

### Error Analysis
EOF
        
        # Error breakdown
        echo "**HTTP Status Code Distribution:**" >> "$report_file"
        awk -F, '{status[$2]++} END {for(code in status) printf("- %s: %d requests\n", code, status[code])}' "$RESULT_DIR/all_results.csv" >> "$report_file"
    fi
    
    cat >> "$report_file" <<EOF

## Resource Usage

See resource_monitoring.csv for detailed system resource usage during the test.

## Recommendations

1. Monitor response times under different load levels
2. Check error rates and investigate failures
3. Analyze resource usage patterns
4. Consider scaling strategies based on results

EOF
    
    echo "📄 Report generated: $report_file"
}

generate_report

# Cleanup
echo ""
echo "🧹 Cleaning up test data..."
rm -f "$RESULT_DIR"/order_*.json

echo ""
echo "✅ Performance testing completed!"
echo "📁 Results saved to: $RESULT_DIR"
echo "📊 Key files:"
echo "   • Performance Report: $RESULT_DIR/performance_report.md"
echo "   • Detailed Results: $RESULT_DIR/all_results.csv" 
echo "   • Resource Monitoring: $RESULT_DIR/resource_monitoring.csv"
echo ""
echo "🔍 Quick Summary:"
if [ -f "$RESULT_DIR/all_results.csv" ]; then
    total=$(wc -l < "$RESULT_DIR/all_results.csv")
    success=$(awk -F, '$2 >= 200 && $2 < 300 {count++} END {print count+0}' "$RESULT_DIR/all_results.csv")
    avg_time=$(awk -F, '{sum+=$3; count++} END {printf "%.2f", sum/count}' "$RESULT_DIR/all_results.csv")
    
    echo "   • Total Requests: $total"
    echo "   • Successful: $success"
    echo "   • Success Rate: $((success * 100 / total))%"
    echo "   • Avg Response Time: ${avg_time}ms"
fi