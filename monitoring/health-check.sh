#!/bin/bash

# Comprehensive Health Check Script
SERVICES=(
    "http://localhost:8081:Order Service"
    "http://localhost:8082:Inventory Service" 
    "http://localhost:8083:Shipping Service"
    "http://localhost:8084:Notification Service"
    "http://localhost:8085:Analytics Service"
)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

check_service() {
    local url=$1
    local name=$2
    
    # Health endpoint check
    if response=$(curl -s --max-time 5 "$url/api/health" 2>/dev/null); then
        if echo "$response" | grep -q '"status":"UP"'; then
            echo -e "${GREEN}✓${NC} $name is healthy"
            
            # Additional checks
            if echo "$response" | grep -q '"diskSpace"'; then
                disk_status=$(echo "$response" | grep -o '"diskSpace":{"status":"[^"]*"' | cut -d'"' -f6)
                if [ "$disk_status" != "UP" ]; then
                    echo -e "${YELLOW}⚠${NC} $name has disk space issues"
                fi
            fi
            
            if echo "$response" | grep -q '"db"'; then
                db_status=$(echo "$response" | grep -o '"db":{"status":"[^"]*"' | cut -d'"' -f6)
                if [ "$db_status" != "UP" ]; then
                    echo -e "${YELLOW}⚠${NC} $name has database issues"
                fi
            fi
            
            return 0
        else
            echo -e "${RED}✗${NC} $name is unhealthy"
            return 1
        fi
    else
        echo -e "${RED}✗${NC} $name is not responding"
        return 1
    fi
}

# Check all services
healthy_count=0
total_count=${#SERVICES[@]}

echo "🏥 Health Check Report - $(date)"
echo "=================================="

for service in "${SERVICES[@]}"; do
    url=$(echo "$service" | cut -d: -f1-2)
    name=$(echo "$service" | cut -d: -f3-)
    
    if check_service "$url" "$name"; then
        healthy_count=$((healthy_count + 1))
    fi
done

echo ""
echo "Summary: $healthy_count/$total_count services healthy"

if [ $healthy_count -eq $total_count ]; then
    echo -e "${GREEN}🎉 All services are healthy!${NC}"
    exit 0
else
    echo -e "${RED}⚠️  Some services have issues${NC}"
    exit 1
fi
