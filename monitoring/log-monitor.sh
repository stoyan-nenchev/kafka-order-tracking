#!/bin/bash

# Log Monitoring and Analysis Script
LOG_DIR="logs"
MONITORING_DIR="monitoring/logs"

# Create monitoring log directory
mkdir -p "$MONITORING_DIR"

# Function to analyze logs for errors
analyze_logs() {
    local service=$1
    local log_file="$LOG_DIR/${service}.log"
    local analysis_file="$MONITORING_DIR/${service}-analysis.txt"
    
    if [ -f "$log_file" ]; then
        echo "Analyzing $service logs..." 
        
        {
            echo "=== Log Analysis for $service - $(date) ==="
            echo ""
            
            echo "Error Summary:"
            grep -i "error\|exception\|failed" "$log_file" | tail -n 20
            echo ""
            
            echo "Warning Summary:"
            grep -i "warn" "$log_file" | tail -n 10
            echo ""
            
            echo "Performance Issues:"
            grep -i "timeout\|slow\|performance" "$log_file" | tail -n 10
            echo ""
            
        } > "$analysis_file"
        
        echo "Analysis saved to $analysis_file"
    else
        echo "No log file found for $service"
    fi
}

# Analyze all services
for service in order inventory shipping notification analytics; do
    analyze_logs "${service}-service"
done

echo "Log analysis completed. Check $MONITORING_DIR for results."
