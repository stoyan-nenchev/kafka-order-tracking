#!/bin/bash

# Kafka Order Tracking System - Startup Script
# This script starts the complete system infrastructure and all microservices

set -e

echo "🚀 Starting Kafka Order Tracking System..."
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop first."
    exit 1
fi

print_status "Docker is running"

# Build the application
echo ""
print_info "Building the application..."
./gradlew clean buildAll
if [ $? -eq 0 ]; then
    print_status "Application built successfully"
else
    print_error "Application build failed"
    exit 1
fi

# Start infrastructure services
echo ""
print_info "Starting infrastructure services (Kafka, PostgreSQL, Kafka UI)..."
cd infrastructure
docker-compose down --remove-orphans > /dev/null 2>&1 || true
docker-compose up -d

# Wait for infrastructure to be ready
print_info "Waiting for infrastructure services to be ready..."
sleep 30

# Check if Kafka is ready
print_info "Checking Kafka cluster health..."
for i in {1..10}; do
    if docker exec kafka-controller kafka-broker-api-versions --bootstrap-server localhost:29092 > /dev/null 2>&1; then
        print_status "Kafka cluster is ready"
        break
    fi
    if [ $i -eq 10 ]; then
        print_error "Kafka cluster failed to start within timeout"
        exit 1
    fi
    echo "Waiting for Kafka... (attempt $i/10)"
    sleep 10
done

# Check if PostgreSQL databases are ready
print_info "Checking PostgreSQL databases..."
for port in 5432 5433 5434 5435 5436; do
    for i in {1..6}; do
        if docker exec postgres-order-db pg_isready -p $port > /dev/null 2>&1; then
            print_status "PostgreSQL on port $port is ready"
            break
        fi
        if [ $i -eq 6 ]; then
            print_warning "PostgreSQL on port $port may not be ready"
        fi
        sleep 5
    done
done

cd ..

# Start microservices
echo ""
print_info "Starting microservices..."

# Array of services with their ports
declare -a services=("order-service:8081" "inventory-service:8082" "shipping-service:8083" "notification-service:8084" "analytics-service:8085")

for service_info in "${services[@]}"; do
    IFS=':' read -r service port <<< "$service_info"
    
    print_info "Starting $service on port $port..."
    
    # Kill any existing process on the port
    lsof -ti:$port | xargs kill -9 > /dev/null 2>&1 || true
    
    # Start the service in background
    cd $service
    nohup ../gradlew bootRun --args='--spring.profiles.active=docker' > ../logs/$service.log 2>&1 &
    echo $! > ../logs/$service.pid
    cd ..
    
    # Wait a moment for service to start
    sleep 5
    
    # Check if service is responding
    for i in {1..12}; do
        if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
            print_status "$service is running on port $port"
            break
        fi
        if [ $i -eq 12 ]; then
            print_warning "$service may not be ready yet on port $port"
        fi
        sleep 5
    done
done

# Create logs directory if it doesn't exist
mkdir -p logs

echo ""
echo "=================================================="
print_status "System startup completed!"
echo ""
echo "🌐 Service URLs:"
echo "   📊 Kafka UI:           http://localhost:8080"
echo "   📦 Order Service:      http://localhost:8081"
echo "   📋 Inventory Service:  http://localhost:8082" 
echo "   🚚 Shipping Service:   http://localhost:8083"
echo "   📧 Notification Svc:   http://localhost:8084"
echo "   📈 Analytics Service:  http://localhost:8085"
echo ""
echo "🔍 Health Checks:"
for service_info in "${services[@]}"; do
    IFS=':' read -r service port <<< "$service_info"
    echo "   $service: http://localhost:$port/actuator/health"
done
echo ""
echo "📊 System Status:"
print_info "Run './scripts/system-status.sh' to check all services"
print_info "Run './scripts/test-order-flow.sh' to test end-to-end flow"
print_info "Run './scripts/stop-system.sh' to shutdown all services"
echo ""
print_status "System is ready for use! 🎉"