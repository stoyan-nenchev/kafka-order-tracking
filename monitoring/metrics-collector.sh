#!/bin/bash

# System Metrics Collection Script
METRICS_FILE="monitoring/logs/system-metrics-$(date +%Y%m%d).csv"

# Initialize metrics file if it doesn't exist
if [ ! -f "$METRICS_FILE" ]; then
    echo "timestamp,cpu_usage,memory_usage,disk_usage,docker_containers,active_connections" > "$METRICS_FILE"
fi

collect_metrics() {
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # CPU Usage
    cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//' || echo "0")
    
    # Memory Usage
    memory_info=$(free | grep Mem)
    memory_total=$(echo $memory_info | awk '{print $2}')
    memory_used=$(echo $memory_info | awk '{print $3}')
    memory_usage=$((memory_used * 100 / memory_total))
    
    # Disk Usage
    disk_usage=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
    
    # Docker Container Count
    docker_containers=$(docker ps -q | wc -l)
    
    # Active Network Connections
    active_connections=$(netstat -an | grep ESTABLISHED | wc -l)
    
    # Write to CSV
    echo "$timestamp,$cpu_usage,$memory_usage,$disk_usage,$docker_containers,$active_connections" >> "$METRICS_FILE"
    
    echo "Metrics collected at $timestamp"
}

# Run continuous collection if argument provided
if [ "$1" = "--continuous" ]; then
    echo "Starting continuous metrics collection..."
    while true; do
        collect_metrics
        sleep 60  # Collect every minute
    done
else
    collect_metrics
fi
