#!/bin/bash

# Kafka Order Tracking System - Shutdown Script
# This script stops all microservices and infrastructure

set -e

echo "🛑 Stopping Kafka Order Tracking System..."
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Stop microservices
print_info "Stopping microservices..."

# Array of services with their ports
declare -a services=("order-service:8081" "inventory-service:8082" "shipping-service:8083" "notification-service:8084" "analytics-service:8085")

for service_info in "${services[@]}"; do
    IFS=':' read -r service port <<< "$service_info"
    
    print_info "Stopping $service..."
    
    # Kill process using PID file
    if [ -f "logs/$service.pid" ]; then
        pid=$(cat "logs/$service.pid")
        if ps -p $pid > /dev/null 2>&1; then
            kill $pid
            sleep 2
            if ps -p $pid > /dev/null 2>&1; then
                kill -9 $pid
            fi
        fi
        rm -f "logs/$service.pid"
        print_status "$service stopped"
    else
        # Kill any process using the port
        lsof -ti:$port | xargs kill -9 > /dev/null 2>&1 || true
        print_status "Cleaned up port $port"
    fi
done

# Stop infrastructure services
print_info "Stopping infrastructure services..."
cd infrastructure
docker-compose down --remove-orphans
if [ $? -eq 0 ]; then
    print_status "Infrastructure services stopped"
else
    print_info "Some containers may already be stopped"
fi
cd ..

# Clean up log files (optional)
if [ "$1" = "--clean-logs" ]; then
    print_info "Cleaning up log files..."
    rm -rf logs/*.log
    print_status "Log files cleaned"
fi

echo ""
echo "=================================================="
print_status "System shutdown completed! 🏁"
echo ""
echo "💡 Tips:"
echo "   • Use '--clean-logs' flag to also remove log files"
echo "   • Use 'docker system prune' to clean up unused containers"
echo "   • Use './scripts/start-system.sh' to restart the system"
echo ""