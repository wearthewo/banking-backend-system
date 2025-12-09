# Banking System - Technical Documentation

## System Architecture

The banking system follows a microservices architecture with event-driven communication. Here's the high-level architecture:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│  API Gateway    │◄───►│  Auth Service   │◄───►│    MySQL DB     │
│  (Spring Cloud  │     │  (JWT)          │     │  (Accounts)     │
│   Gateway)      │     │                 │     │                 │
└────────┬────────┘     └─────────────────┘     └─────────────────┘
         │
         │  HTTP/HTTPS
         │
┌────────▼────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│  Transaction    │     │  Notification   │     │   Kafka Topics  │
│  Service        │◄───►│  Service        │◄───►│   (Events)      │
│                 │     │  (Email/SMS)    │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │
         │  gRPC
         │
┌────────▼────────┐     ┌─────────────────┐
│                 │     │                 │
│  Reporting      │     │  Audit Service  │
│  Service        │◄───►│  (MongoDB)      │
│  (Batch)        │     │                 │
└─────────────────┘     └─────────────────┘
```

## Service Communication

### 1. Synchronous Communication (HTTP/REST)
- **API Gateway** handles all incoming requests and routes them to appropriate services
- Service-to-service communication for time-sensitive operations
- Used for:
  - User authentication/authorization
  - Account management
  - Balance inquiries

### 2. Asynchronous Communication (Kafka)
- **Transaction Processing Flow**:
  1. Transaction request received by API Gateway
  2. Transaction Service validates and publishes event to Kafka
  3. Multiple consumers process the event:
     - Update account balances
     - Send notifications
     - Log audit trail
     - Update reporting data

### 3. gRPC Communication
- Used for high-performance service-to-service communication
- Implemented for:
  - Real-time balance updates
  - High-frequency data exchange
  - Internal service communication

## Data Flow

### Transaction Processing
1. Client initiates transaction via REST API
2. API Gateway authenticates and routes to Transaction Service
3. Transaction Service:
   - Validates transaction
   - Publishes `TransactionInitiated` event
   - Updates transaction status
4. Event consumers process in parallel:
   - Account Service: Updates balances
   - Notification Service: Sends alerts
   - Audit Service: Records transaction
   - Reporting Service: Updates analytics

## Monitoring and Observability

### Prometheus
- Scrapes metrics from all services
- Collects:
  - Application metrics (JVM, request rates)
  - System metrics (CPU, memory)
  - Business metrics (transactions per second)

### Grafana
- Pre-configured dashboards:
  - API Performance
  - Error Rates
  - Transaction Volume
  - System Health

### Alertmanager
- Handles alerts from Prometheus
- Sends notifications for:
  - Service outages
  - Performance degradation
  - Business anomalies

## CI/CD Pipeline

### 1. Build and Test
- Triggered on push/PR
- Runs:
  - Unit tests
  - Integration tests
  - Code quality checks
  - Security scans

### 2. Docker Build
- Builds container images
- Pushes to container registry
- Tags with git commit hash

### 3. Deployment
- Blue-green deployment to staging
- Automated rollback on failure
- Canary releases to production

## Environment Variables

Key environment variables (see `.env.example` for full list):

```env
# Database
DB_URL=jdbc:mysql://mysql:3306/banking
DB_USER=user
DB_PASSWORD=password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000

# Monitoring
PROMETHEUS_METRICS_ENABLED=true
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus
```

## Local Development Setup

1. Start all services:
   ```bash
   docker-compose up -d
   ```

2. Access services:
   - API: http://localhost:8080
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000
   - Kafka UI: http://localhost:8081

3. Run tests:
   ```bash
   mvn test
   ```

## Production Deployment

1. Set up environment variables in production
2. Configure monitoring and alerting
3. Set up backup and recovery
4. Configure scaling policies
5. Enable security features (TLS, WAF, etc.)
