#!/bin/bash

# Kafka Order Tracking System - System Status Check Script
# This script checks the health and status of all system components

echo "🔍 Kafka Order Tracking System - Status Check"
echo "=============================================="

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

# Function to check HTTP endpoint
check_http_endpoint() {
    local name=$1
    local url=$2
    local expected=${3:-"UP"}
    
    if response=$(curl -s "$url" 2>/dev/null); then
        if echo "$response" | grep -q "$expected"; then
            print_status "$name is responding correctly"
            return 0
        else
            print_warning "$name is responding but content unexpected"
            return 1
        fi
    else
        print_error "$name is not responding"
        return 1
    fi
}

# Function to check port
check_port() {
    local name=$1
    local port=$2
    
    if lsof -i :$port > /dev/null 2>&1; then
        print_status "$name (port $port) is listening"
        return 0
    else
        print_error "$name (port $port) is not listening"
        return 1
    fi
}

# Function to check docker container
check_docker_container() {
    local name=$1
    local container=$2
    
    if docker ps --filter "name=$container" --filter "status=running" | grep -q "$container"; then
        print_status "$name container is running"
        return 0
    else
        print_error "$name container is not running"
        return 1
    fi
}

echo ""
print_info "Checking Docker Infrastructure..."

# Check Docker containers
containers=(
    "Kafka Controller:kafka-controller"
    "Kafka Broker 1:kafka-broker1"
    "Kafka Broker 2:kafka-broker2"
    "Kafka UI:kafka-ui"
    "PostgreSQL Order DB:postgres-order-db"
    "PostgreSQL Inventory DB:postgres-inventory-db"
    "PostgreSQL Shipping DB:postgres-shipping-db"
    "PostgreSQL Notification DB:postgres-notification-db"
    "PostgreSQL Analytics DB:postgres-analytics-db"
)

docker_healthy=true
for container in "${containers[@]}"; do
    IFS=':' read -r name container_name <<< "$container"
    if ! check_docker_container "$name" "$container_name"; then
        docker_healthy=false
    fi
done

echo ""
print_info "Checking Microservices..."

# Check microservices health endpoints
services=(
    "Order Service:http://localhost:8081/actuator/health"
    "Inventory Service:http://localhost:8082/actuator/health"
    "Shipping Service:http://localhost:8083/actuator/health"
    "Notification Service:http://localhost:8084/actuator/health"
    "Analytics Service:http://localhost:8085/actuator/health"
)

services_healthy=true
for service in "${services[@]}"; do
    IFS=':' read -r name url <<< "$service"
    if ! check_http_endpoint "$name" "$url"; then
        services_healthy=false
    fi
done

echo ""
print_info "Checking Network Ports..."

# Check important ports
ports=(
    "Order Service:8081"
    "Inventory Service:8082"
    "Shipping Service:8083"
    "Notification Service:8084"
    "Analytics Service:8085"
    "Kafka UI:8080"
    "Kafka Controller:9092"
    "Kafka Broker 1:9093"
    "Kafka Broker 2:9094"
    "PostgreSQL Order:5432"
    "PostgreSQL Inventory:5433"
    "PostgreSQL Shipping:5434"
    "PostgreSQL Notification:5435"
    "PostgreSQL Analytics:5436"
)

ports_healthy=true
for port_info in "${ports[@]}"; do
    IFS=':' read -r name port <<< "$port_info"
    if ! check_port "$name" "$port"; then
        ports_healthy=false
    fi
done

echo ""
print_info "Checking API Endpoints..."

# Check API endpoints
api_endpoints=(
    "Kafka UI:http://localhost:8080"
    "Order API:http://localhost:8081/api/orders"
    "Shipping API:http://localhost:8083/api/shipments"
    "Notification API:http://localhost:8084/api/notifications"
    "Analytics API:http://localhost:8085/api/analytics/dashboard"
)

apis_healthy=true
for endpoint in "${api_endpoints[@]}"; do
    IFS=':' read -r name url <<< "$endpoint"
    if ! check_http_endpoint "$name" "$url" ""; then
        apis_healthy=false
    fi
done

echo ""
print_info "System Resource Usage..."

# Show Docker stats
if command -v docker >/dev/null 2>&1; then
    echo "Docker Container Resources:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null | head -10
fi

echo ""
print_info "System Logs (last 5 lines per service)..."

# Show recent logs from each service
if [ -d "logs" ]; then
    for service in order-service inventory-service shipping-service notification-service analytics-service; do
        if [ -f "logs/$service.log" ]; then
            echo ""
            echo "📋 $service (last 5 lines):"
            tail -5 "logs/$service.log" 2>/dev/null || echo "   No logs available"
        fi
    done
else
    print_info "No logs directory found"
fi

# Final status summary
echo ""
echo "=============================================="
print_info "System Status Summary:"

if $docker_healthy; then
    print_status "Docker Infrastructure: All containers running"
else
    print_error "Docker Infrastructure: Some containers have issues"
fi

if $services_healthy; then
    print_status "Microservices: All services healthy"
else
    print_error "Microservices: Some services have issues"
fi

if $ports_healthy; then
    print_status "Network Ports: All ports listening"
else
    print_error "Network Ports: Some ports not available"
fi

if $apis_healthy; then
    print_status "API Endpoints: All APIs responding"
else
    print_error "API Endpoints: Some APIs not responding"
fi

if $docker_healthy && $services_healthy && $ports_healthy && $apis_healthy; then
    echo ""
    print_status "🎉 System Status: ALL SYSTEMS OPERATIONAL! 🎉"
    echo ""
    echo "🌐 Access URLs:"
    echo "   📊 Kafka UI:          http://localhost:8080"
    echo "   📦 Order Service:     http://localhost:8081"
    echo "   📋 Inventory Service: http://localhost:8082"
    echo "   🚚 Shipping Service:  http://localhost:8083"
    echo "   📧 Notification Svc:  http://localhost:8084"
    echo "   📈 Analytics Service: http://localhost:8085"
else
    echo ""
    print_warning "⚠️  System Status: SOME ISSUES DETECTED ⚠️"
    echo ""
    echo "🔧 Troubleshooting:"
    echo "   • Run './scripts/start-system.sh' to restart all services"
    echo "   • Check Docker Desktop is running"
    echo "   • Run 'docker-compose logs' in infrastructure/ for detailed logs"
    echo "   • Check individual service logs in logs/ directory"
fi

echo ""