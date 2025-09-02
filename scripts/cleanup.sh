#!/bin/bash

# Kafka Order Tracking System - Cleanup Script
# This script performs a complete cleanup of the system

echo "🧹 Kafka Order Tracking System - Complete Cleanup"
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

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Confirmation prompt
print_warning "This will completely clean up the system including:"
echo "   • Stop all running services"
echo "   • Remove all Docker containers and volumes"
echo "   • Clean all build artifacts"
echo "   • Remove all log files"
echo "   • Reset all databases"
echo ""

read -p "Are you sure you want to proceed? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_info "Cleanup cancelled."
    exit 0
fi

echo ""
print_info "Starting complete cleanup..."

# Step 1: Stop all services
print_info "Step 1: Stopping all services..."
./scripts/stop-system.sh --clean-logs

# Step 2: Clean Docker resources
print_info "Step 2: Cleaning Docker resources..."
cd infrastructure

# Stop and remove all containers
docker-compose down --remove-orphans --volumes

# Remove specific volumes
print_info "Removing Docker volumes..."
docker volume rm \
    infrastructure_postgres_order_data \
    infrastructure_postgres_inventory_data \
    infrastructure_postgres_shipping_data \
    infrastructure_postgres_notification_data \
    infrastructure_postgres_analytics_data \
    infrastructure_kafka_controller_data \
    infrastructure_kafka_broker1_data \
    infrastructure_kafka_broker2_data \
    2>/dev/null || true

cd ..

# Step 3: Clean build artifacts
print_info "Step 3: Cleaning build artifacts..."
./gradlew clean
rm -rf .gradle/
rm -rf */build/
rm -rf */.gradle/

# Step 4: Clean logs and temporary files
print_info "Step 4: Cleaning logs and temporary files..."
rm -rf logs/
rm -rf */logs/
rm -rf temp/
rm -rf */temp/

# Step 5: Clean IDE files (optional)
if [ "$1" = "--include-ide" ]; then
    print_info "Step 5: Cleaning IDE files..."
    rm -rf .idea/
    rm -rf */.idea/
    rm -rf .vscode/
    rm -rf */.vscode/
    rm -rf *.iml
    rm -rf */*.iml
fi

# Step 6: Docker system cleanup (optional)
if [ "$1" = "--docker-prune" ] || [ "$2" = "--docker-prune" ]; then
    print_info "Step 6: Docker system cleanup..."
    docker system prune -f
    docker image prune -f
fi

# Step 7: Reset file permissions (Unix/Linux/Mac only)
if [[ "$OSTYPE" != "msys" && "$OSTYPE" != "win32" ]]; then
    print_info "Step 7: Resetting file permissions..."
    chmod +x scripts/*.sh
    chmod +x gradlew
fi

# Recreate necessary directories
print_info "Recreating necessary directories..."
mkdir -p logs
mkdir -p temp

echo ""
echo "=================================================="
print_status "Complete cleanup finished! 🎉"
echo ""
echo "📋 What was cleaned:"
echo "   ✅ All services stopped"
echo "   ✅ Docker containers and volumes removed"
echo "   ✅ Build artifacts cleaned"
echo "   ✅ Log files removed"
echo "   ✅ Temporary files cleaned"

if [ "$1" = "--include-ide" ]; then
    echo "   ✅ IDE files removed"
fi

if [ "$1" = "--docker-prune" ] || [ "$2" = "--docker-prune" ]; then
    echo "   ✅ Docker system pruned"
fi

echo ""
echo "🚀 Next steps:"
echo "   • Run './scripts/start-system.sh' to start fresh"
echo "   • All data will be recreated with sample data"
echo "   • Services will start with clean state"
echo ""
print_info "System is ready for a fresh start! ✨"