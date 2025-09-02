# Kafka Order Tracking System

A distributed order management system built with Spring Boot, Kafka, and PostgreSQL. Started as a learning project but grew into something more comprehensive.

## What This Does

Handles the complete order lifecycle from creation to delivery across multiple microservices:
- **Order Service** - Creates and manages orders
- **Inventory Service** - Checks stock and reserves items  
- **Shipping Service** - Handles shipment tracking
- **Notification Service** - Sends customer emails
- **Analytics Service** - Collects business metrics

## Quick Start

### Prerequisites
- Java 21
- Docker Desktop (make sure it's running!)
- Git

### Running the System

```bash
# Clone and setup
git clone <your-repo-url>
cd kafka-order-tracking

# Start infrastructure (this takes a few minutes)
cd infrastructure
docker-compose up -d

# Wait for everything to start, then build services
cd ..
./gradlew clean build

# Start all services
./scripts/start-system.sh

# Test that it works
./scripts/test-order-flow.sh
```

## Architecture

```
Client -> Order Service -> Kafka -> [Inventory, Shipping, Notification, Analytics]
```

Each service has its own PostgreSQL database. Everything communicates through Kafka events.

## Services

| Service | Port | Database Port | What It Does |
|---------|------|---------------|--------------|
| Order Service | 8081 | 5432 | Order CRUD operations |
| Inventory Service | 8082 | 5433 | Stock management |
| Shipping Service | 8083 | 5434 | Shipment tracking |
| Notification Service | 8084 | 5435 | Email notifications |
| Analytics Service | 8085 | 5436 | Business metrics |
| Kafka UI | 8080 | - | Monitor Kafka topics |

## Making Orders

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerInfo": {
      "customerId": "CUST-001",
      "firstName": "John", 
      "lastName": "Doe",
      "email": "john@example.com",
      "phone": "+1234567890",
      "address": "123 Main St",
      "city": "Anytown",
      "state": "CA",
      "zipCode": "12345"
    },
    "orderItems": [{
      "productId": "PROD-001",
      "quantity": 2,
      "unitPrice": 25.99
    }],
    "totalAmount": 51.98
  }'
```

## Development

### Building
```bash
./gradlew clean build
```

### Running Individual Services
```bash
# Order service
./gradlew :order-service:bootRun

# Inventory service  
./gradlew :inventory-service:bootRun
```

### Testing
```bash
# Unit tests
./gradlew test

# Integration tests (needs Docker running)
./gradlew :integration-tests:test
```

## Configuration

Each service has its own `application.yml`. The main things to configure:

- Database connections (PostgreSQL)
- Kafka bootstrap servers
- Circuit breaker settings (added after some production issues)

## Monitoring

- **Health Checks**: Each service has `/actuator/health`
- **Kafka UI**: http://localhost:8080 to see topics and messages
- **System Status**: Run `./scripts/system-status.sh`

## Common Issues

### "Connection refused" when starting
Make sure Docker Desktop is running and all containers are up:
```bash
docker ps
```

### Tests failing
The integration tests can be flaky with timing. If they fail, try:
```bash
./scripts/stop-system.sh
./scripts/start-system.sh
# Wait 30 seconds
./scripts/test-order-flow.sh
```

### Kafka not receiving messages
Check the Kafka UI at http://localhost:8080. Sometimes the topics need to be created manually:
```bash
docker exec -it kafka-controller kafka-topics --create --topic orders.events --bootstrap-server localhost:29092 --partitions 3 --replication-factor 1
```

## Project Structure

```
kafka-order-tracking/
├── shared-models/          # Common DTOs and events
├── order-service/          # Order management
├── inventory-service/      # Stock management  
├── shipping-service/       # Shipment tracking
├── notification-service/   # Email notifications
├── analytics-service/      # Business metrics
├── integration-tests/      # End-to-end tests
├── infrastructure/         # Docker compose files
├── scripts/               # Utility scripts
└── monitoring/            # Prometheus/Grafana setup
```

## What I Learned

This started as a simple CRUD app but grew as I learned about:

- Event-driven architecture with Kafka
- Circuit breakers and resilience patterns  
- TestContainers for integration testing
- Multi-database transactions (the hard way)
- Why you need correlation IDs for debugging

The notification service is probably over-engineered, and the analytics service was added later which is why the patterns are a bit different.

## TODO

- [ ] Add API gateway (probably Spring Cloud Gateway)
- [ ] Implement proper security (OAuth2/JWT)
- [ ] Add proper logging aggregation  
- [ ] Fix that flaky integration test
- [ ] Add more comprehensive error handling in inventory service
- [ ] Performance test the analytics queries

## Tech Stack

- **Java 21** - Latest LTS
- **Spring Boot 3.x** - Framework
- **Apache Kafka** - Event streaming  
- **PostgreSQL** - Database per service
- **Docker** - Containerization
- **TestContainers** - Integration testing
- **Gradle** - Build tool
- **Resilience4j** - Circuit breakers

---

*Note: This system handles about 100 orders/minute in testing. Haven't tried production scale.*