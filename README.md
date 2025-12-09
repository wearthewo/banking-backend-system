# Banking Backend System

A secure and scalable banking backend system built with Spring Boot, Kafka, and MySQL, featuring monitoring with Prometheus and Grafana.

## Features

- **Account Management**: Create, read, update, and delete accounts
- **Transaction Processing**: Secure money transfers between accounts
- **Event-Driven Architecture**: Using Apache Kafka for asynchronous processing
- **Monitoring**: Integrated Prometheus and Grafana for observability
- **Containerized**: Easy deployment with Docker and Docker Compose
- **CI/CD**: GitHub Actions workflow for automated testing and deployment

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker 20.10+
- Docker Compose 2.0+
- MySQL 8.0+
- Kafka 3.0+

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/banking-backend-system.git
   cd banking-backend-system
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Build the application**
   ```bash
   mvn clean install
   ```

4. **Start the services**
   ```bash
   docker-compose up -d
   ```

5. **Access the application**
   - API: http://localhost:8080/api
   - Swagger UI: http://localhost:8080/api/swagger-ui.html
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000 (admin/admin)
   - Kafka UI: http://localhost:8081

## Project Structure

```
banking-backend-system/
├── src/
│   ├── main/
│   │   ├── java/com/banking/...
│   │   └── resources/
│   └── test/
├── monitoring/
│   ├── grafana/
│   └── prometheus/
├── .github/workflows/
├── .env.example
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## API Documentation

API documentation is available via Swagger UI at:
http://localhost:8080/api/swagger-ui.html

## Monitoring

The application includes monitoring with:
- **Prometheus**: Metrics collection
- **Grafana**: Visualization dashboards
- **Actuator**: Health checks and metrics

## CI/CD

The project includes a GitHub Actions workflow for:
- Building and testing on push/pr
- Building and pushing Docker images
- Deploying to staging/production (configured in GitHub Secrets)

## Security

- JWT-based authentication
- Password hashing with BCrypt
- Environment-based configuration
- Secure defaults for all services

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
