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
