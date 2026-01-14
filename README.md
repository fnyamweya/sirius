# Sirius - Enterprise Treasury Platform

A production-grade, modular Enterprise Treasury Management platform built with Java 21, Spring Boot 3, PostgreSQL, and Redis.

## 🏗️ Architecture

The application follows a clean, modular architecture with clear separation of concerns:

```
sirius/
├── sirius-api/          # REST API layer with Spring Security & JWT
├── sirius-core/         # Core domain models and business logic
├── sirius-data/         # Data access layer with JPA & PostgreSQL
└── sirius-infra/        # Infrastructure (Redis cache & streams)
```

## 🚀 Technology Stack

- **Java 17** - LTS version with modern language features
- **Spring Boot 3.2.1** - Enterprise application framework
- **Spring Security** - Authentication and authorization with JWT
- **PostgreSQL** - Robust relational database
- **Redis** - Caching and event streaming (Redis Streams)
- **Flyway** - Database migration management
- **Maven** - Dependency management and build tool
- **Docker Compose** - Local development environment

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.8+
- Docker and Docker Compose (for local development)

## 🛠️ Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/fnyamweya/sirius.git
cd sirius
```

### 2. Start Infrastructure Services

Start PostgreSQL and Redis using Docker Compose:

```bash
docker-compose up -d
```

Verify services are running:

```bash
docker-compose ps
```

### 3. Build the Application

```bash
mvn clean install
```

### 4. Run the Application

```bash
cd sirius-api
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## 🔐 Authentication

The platform uses JWT (JSON Web Tokens) for authentication.

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

### Using the Token

Include the token in subsequent requests:

```bash
curl -X GET http://localhost:8080/api/health \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## 🏥 Health Checks

### Application Health

```bash
curl http://localhost:8080/api/health
```

### Actuator Health Endpoint

```bash
curl http://localhost:8080/actuator/health
```

## 📦 Module Overview

### sirius-api
- REST controllers
- Spring Security configuration
- JWT authentication filter
- Exception handling
- API documentation

### sirius-core
- Domain models
- Business logic services
- Core exceptions
- Domain events

### sirius-data
- JPA entities
- Spring Data repositories
- Database migrations (Flyway)
- Audit configuration

### sirius-infra
- Redis cache configuration
- Redis Streams configuration
- External service integrations

## 🗄️ Database

### Schema Management

Database migrations are managed by Flyway. Migration scripts are located in:
```
sirius-data/src/main/resources/db/migration/
```

Flyway runs automatically on application startup.

### Default User

A default admin user is created during initial migration:
- **Username:** admin
- **Password:** admin123

⚠️ **Change this password in production!**

## 💾 Redis Configuration

Redis is used for two purposes:

1. **Caching** - Spring Cache abstraction with Redis backend
2. **Event Streaming** - Redis Streams for event-driven communication

### Cache Examples

```java
@Cacheable("users")
public User getUserById(Long id) {
    // Method implementation
}
```

### Redis Streams

The platform includes Redis Streams configuration for event-driven architecture. See `RedisStreamConfig.java` for details.

## 🔧 Configuration

Key configuration properties in `application.yml`:

- **Database:** PostgreSQL connection settings
- **Redis:** Cache and connection pool settings
- **JWT:** Secret key and token expiration
- **Actuator:** Health and metrics endpoints
- **Logging:** Application logging levels

### Environment Variables

Override defaults using environment variables:

```bash
export JWT_SECRET=your-secret-key
export SPRING_DATASOURCE_PASSWORD=your-db-password
```

## 🧪 Testing

Run all tests:

```bash
mvn test
```

Run tests for a specific module:

```bash
cd sirius-api
mvn test
```

## 📊 Monitoring

### Actuator Endpoints

- Health: `/actuator/health`
- Info: `/actuator/info`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## 🐳 Docker Support

### Running with Docker Compose

The `docker-compose.yml` includes:
- PostgreSQL with persistent storage
- Redis with AOF persistence
- Health checks for both services
- Custom network for service communication

### Stopping Services

```bash
docker-compose down
```

### Stopping and Removing Volumes

```bash
docker-compose down -v
```

## 🔒 Security Features

- JWT-based authentication
- BCrypt password encoding
- Stateless session management
- CSRF protection disabled (suitable for REST APIs)
- Method-level security annotations
- Audit logging for all database operations

## 📝 Development Guidelines

### Code Structure

- Use domain-driven design principles
- Keep modules loosely coupled
- Follow SOLID principles
- Write comprehensive tests
- Document public APIs

### Adding New Endpoints

1. Create controller in `sirius-api/controller`
2. Implement service in `sirius-core/service`
3. Add repository if needed in `sirius-data/repository`
4. Write integration tests

### Database Migrations

1. Create new migration in `sirius-data/src/main/resources/db/migration/`
2. Follow naming convention: `V{version}__{description}.sql`
3. Test migration locally before committing

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

For issues and questions, please open an issue on GitHub.

---

**Note:** This is a starter template for an Enterprise Treasury platform. Extend and customize based on your specific requirements.