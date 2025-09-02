#!/bin/bash

# System Monitoring and Observability Setup
# Sets up monitoring dashboards and health checks

set -e

echo "📊 Setting up System Monitoring and Observability"
echo "================================================"

# Configuration
MONITORING_DIR="monitoring"
DASHBOARD_PORT=3000
METRICS_PORT=9090

# Create monitoring directory
mkdir -p $MONITORING_DIR/{dashboards,alerts,logs}

# Function to create Prometheus configuration
create_prometheus_config() {
    cat > "$MONITORING_DIR/prometheus.yml" <<EOF
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alerts/*.yml"

scrape_configs:
  - job_name: 'order-service'
    static_configs:
      - targets: ['localhost:8081']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    
  - job_name: 'inventory-service'
    static_configs:
      - targets: ['localhost:8082']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    
  - job_name: 'shipping-service'
    static_configs:
      - targets: ['localhost:8083']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    
  - job_name: 'notification-service'
    static_configs:
      - targets: ['localhost:8084']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    
  - job_name: 'analytics-service'
    static_configs:
      - targets: ['localhost:8085']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s

  - job_name: 'kafka'
    static_configs:
      - targets: ['localhost:9092']
    scrape_interval: 30s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
EOF
}

# Function to create alert rules
create_alert_rules() {
    cat > "$MONITORING_DIR/alerts/service-alerts.yml" <<EOF
groups:
  - name: microservices
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 30s
        labels:
          severity: critical
        annotations:
          summary: "Service {{ \$labels.instance }} is down"
          description: "{{ \$labels.job }} service has been down for more than 30 seconds"

      - alert: HighResponseTime
        expr: http_server_requests_seconds{quantile="0.95"} > 0.5
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "High response time for {{ \$labels.job }}"
          description: "95th percentile response time is {{ \$value }}s for {{ \$labels.job }}"

      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate for {{ \$labels.job }}"
          description: "Error rate is {{ \$value }} errors per second for {{ \$labels.job }}"

      - alert: DatabaseConnectionIssues
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "{{ \$labels.job }} is using {{ \$value }}% of database connections"

  - name: kafka
    rules:
      - alert: KafkaConsumerLag
        expr: kafka_consumer_lag_max > 1000
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High consumer lag in Kafka"
          description: "Consumer lag is {{ \$value }} messages for {{ \$labels.topic }}"

  - name: system
    rules:
      - alert: HighCPUUsage
        expr: system_cpu_usage > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ \$value }}%"

      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM memory usage"
          description: "JVM memory usage is {{ \$value }}% for {{ \$labels.job }}"
EOF
}

# Function to create Grafana dashboard configuration
create_grafana_dashboard() {
    cat > "$MONITORING_DIR/dashboards/kafka-order-tracking.json" <<EOF
{
  "dashboard": {
    "id": null,
    "title": "Kafka Order Tracking System",
    "tags": ["kafka", "microservices", "orders"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Service Health",
        "type": "stat",
        "targets": [
          {
            "expr": "up",
            "legendFormat": "{{ job }}"
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 0
        }
      },
      {
        "id": 2,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[1m])",
            "legendFormat": "{{ job }} - {{ uri }}"
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 12,
          "y": 0
        }
      },
      {
        "id": 3,
        "title": "Response Time (95th percentile)",
        "type": "graph",
        "targets": [
          {
            "expr": "http_server_requests_seconds{quantile=\"0.95\"}",
            "legendFormat": "{{ job }}"
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 8
        }
      },
      {
        "id": 4,
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[1m])",
            "legendFormat": "{{ job }} - 5xx errors"
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 12,
          "y": 8
        }
      },
      {
        "id": 5,
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "{{ job }} - Active"
          },
          {
            "expr": "hikaricp_connections_max",
            "legendFormat": "{{ job }} - Max"
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 16
        }
      },
      {
        "id": 6,
        "title": "JVM Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes",
            "legendFormat": "{{ job }} - {{ area }}"
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 12,
          "y": 16
        }
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "5s"
  }
}
EOF
}

# Function to create health check script
create_health_check_script() {
    cat > "$MONITORING_DIR/health-check.sh" <<'EOF'
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
EOF
    
    chmod +x "$MONITORING_DIR/health-check.sh"
}

# Function to create log aggregation setup
create_log_monitoring() {
    cat > "$MONITORING_DIR/log-monitor.sh" <<'EOF'
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
EOF
    
    chmod +x "$MONITORING_DIR/log-monitor.sh"
}

# Function to create system metrics collector
create_metrics_collector() {
    cat > "$MONITORING_DIR/metrics-collector.sh" <<'EOF'
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
EOF
    
    chmod +x "$MONITORING_DIR/metrics-collector.sh"
}

# Main setup function
main() {
    echo "📋 Creating monitoring configuration files..."
    
    create_prometheus_config
    echo "✅ Prometheus configuration created"
    
    create_alert_rules  
    echo "✅ Alert rules created"
    
    create_grafana_dashboard
    echo "✅ Grafana dashboard configuration created"
    
    create_health_check_script
    echo "✅ Health check script created"
    
    create_log_monitoring
    echo "✅ Log monitoring scripts created"
    
    create_metrics_collector
    echo "✅ Metrics collection script created"
    
    # Create monitoring Docker Compose
    cat > "$MONITORING_DIR/docker-compose-monitoring.yml" <<EOF
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alerts:/etc/prometheus/alerts
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-storage:/var/lib/grafana
      - ./dashboards:/etc/grafana/provisioning/dashboards

volumes:
  grafana-storage:
EOF
    echo "✅ Monitoring Docker Compose created"
    
    # Create startup script for monitoring
    cat > "$MONITORING_DIR/start-monitoring.sh" <<'EOF'
#!/bin/bash

echo "🚀 Starting Monitoring Stack..."

# Start Prometheus and Grafana
docker-compose -f docker-compose-monitoring.yml up -d

echo "⏳ Waiting for services to start..."
sleep 10

# Check if services are running
if curl -s http://localhost:9090/-/healthy >/dev/null 2>&1; then
    echo "✅ Prometheus is running on http://localhost:9090"
else
    echo "❌ Prometheus failed to start"
fi

if curl -s http://localhost:3000/api/health >/dev/null 2>&1; then
    echo "✅ Grafana is running on http://localhost:3000"
    echo "   Default login: admin/admin"
else
    echo "❌ Grafana failed to start"
fi

echo ""
echo "📊 Monitoring Stack Started"
echo "   • Prometheus: http://localhost:9090"
echo "   • Grafana: http://localhost:3000"
echo ""
echo "Next steps:"
echo "   • Import dashboards in Grafana"
echo "   • Configure alert channels"
echo "   • Set up notification webhooks"
EOF
    
    chmod +x "$MONITORING_DIR/start-monitoring.sh"
    echo "✅ Monitoring startup script created"
    
    echo ""
    echo "🎉 Monitoring and Observability Setup Complete!"
    echo ""
    echo "📁 Files created in $MONITORING_DIR/:"
    echo "   • prometheus.yml - Prometheus configuration"
    echo "   • alerts/service-alerts.yml - Alert rules"
    echo "   • dashboards/kafka-order-tracking.json - Grafana dashboard"
    echo "   • health-check.sh - Service health checker"
    echo "   • log-monitor.sh - Log analysis tool"
    echo "   • metrics-collector.sh - System metrics collector"
    echo "   • docker-compose-monitoring.yml - Monitoring stack"
    echo "   • start-monitoring.sh - Quick start script"
    echo ""
    echo "🚀 To start monitoring:"
    echo "   cd $MONITORING_DIR && ./start-monitoring.sh"
    echo ""
    echo "📋 To check system health:"
    echo "   ./monitoring/health-check.sh"
    echo ""
    echo "📊 To collect metrics:"
    echo "   ./monitoring/metrics-collector.sh"
}

# Run setup
main "$@"