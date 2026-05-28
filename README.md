# IoT Server Application

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A comprehensive Spring Boot backend platform for managing IoT-enabled smart lamps at scale. This system provides real-time control, fleet management, telemetry monitoring, OTA firmware updates, and smart automation features for RGB + White LED devices.

## 📋 Table of Contents

- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Architecture](#-architecture)
- [Prerequisites](#-prerequisites)
- [Installation & Setup](#-installation--setup)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Usage Examples](#-usage-examples)
- [MQTT Topics](#-mqtt-topics)
- [Protocol Buffers](#-protocol-buffers)
- [Testing](#-testing)
- [Development](#-development)
- [Deployment](#-deployment)
- [Contributing](#-contributing)
- [License](#-license)
- [Author](#-author)

## ✨ Features

### Core Functionality
- **JWT Authentication** - Secure access/refresh token-based authentication with BCrypt password hashing
- **Real-time Lamp Control** - Control RGB, White, and Photo modes with instant MQTT communication
- **Fleet Management** - Organize and control multiple lamps in groups for bulk operations
- **OTA Firmware Updates** - Over-the-air firmware updates with AWS S3 storage integration
- **Device Health Monitoring** - Real-time telemetry tracking with automated alert system
- **Statistics & Analytics** - Comprehensive metrics for global, user, fleet, and individual lamp performance

### Smart Features
- **Circadian Rhythm Lighting** - Automatic color temperature adjustment based on time of day
- **Adaptive Brightness** - Dynamic brightness control based on ambient light sensors
- **Disco Effects** - Multiple lighting effects (Color Cycle, Strobe, Pulse)
- **Preset Management** - Quick-select lighting presets for common scenarios
- **Scheduled Automation** - Time-based and sensor-triggered lighting schedules

### Technical Features
- **Protocol Buffers** - Efficient binary serialization for device communication
- **MQTT via RabbitMQ** - Reliable message queuing for device commands and telemetry
- **RESTful API** - Comprehensive REST API with OpenAPI/Swagger documentation
- **Containerized Deployment** - Docker and Docker Compose support for easy deployment
- **Comprehensive Testing** - Unit and integration tests with Testcontainers

## 🛠 Technology Stack

### Backend Framework
- **Spring Boot 3.3.5** - Core application framework
- **Spring Security** - JWT-based authentication and authorization
- **Spring Data JPA** - Database access and ORM
- **Spring Integration** - MQTT and AMQP integration

### Database
- **PostgreSQL 15** - Production database
- **H2** - In-memory database for testing

### Messaging & Communication
- **RabbitMQ 3** - MQTT broker and message queue
- **Eclipse Paho MQTT 1.2.5** - MQTT client library
- **Protocol Buffers 3.24.0** - Binary serialization

### Cloud & Storage
- **AWS S3 SDK 2.20.0** - Firmware file storage

### Security
- **JWT (jjwt 0.11.5)** - JSON Web Token implementation
- **BCrypt** - Password hashing

### Documentation & API
- **SpringDoc OpenAPI 2.6.0** - API documentation and Swagger UI

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing with containers
- **Spring Security Test** - Security testing utilities
- **Spring Rabbit Test** - RabbitMQ testing support

### Build & Development
- **Maven** - Dependency management and build tool
- **Lombok** - Boilerplate code reduction
- **Docker & Docker Compose** - Containerization

## 🏗 Architecture

The application follows a layered architecture pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Applications                      │
│              (Web UI, Mobile Apps, IoT Devices)             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                            │
│  (Controllers: Auth, User, Lamp, Fleet, OTA, Stats)        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Service Layer                              │
│  (Business Logic: LampService, FleetService, OtaService)    │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  Repository │  │    MQTT     │  │     S3      │
│    Layer    │  │   Service   │  │   Storage   │
└──────┬──────┘  └──────┬──────┘  └─────────────┘
       │                │
       ▼                ▼
┌─────────────┐  ┌─────────────┐
│ PostgreSQL  │  │  RabbitMQ   │
│  Database   │  │ MQTT Broker │
└─────────────┘  └─────────────┘
```

### Key Components

- **Controllers** - Handle HTTP requests and responses
- **Services** - Implement business logic and orchestration
- **Repositories** - Data access layer using Spring Data JPA
- **MQTT Service** - Manages device communication via RabbitMQ
- **Schedulers** - Background tasks for monitoring and automation
- **Security** - JWT-based stateless authentication

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6+** (or use included Maven wrapper)
- **Docker** and **Docker Compose** (for containerized deployment)
- **PostgreSQL 15** (if running without Docker)
- **RabbitMQ 3** with MQTT plugin (if running without Docker)

Optional:
- **AWS Account** (for S3 firmware storage)
- **Git** (for cloning the repository)

## 🚀 Installation & Setup

### Option 1: Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/IoTServerApp.git
   cd IoTServerApp
   ```

2. **Create Docker network**
   ```bash
   docker network create iot-net
   ```

3. **Configure environment variables**
   
   Edit `docker-compose.yml` to set your environment variables:
   ```yaml
   environment:
     - JWT_SECRET=your-secret-key-here
     - MQTT_USERNAME=iotproject
     - MQTT_PASSWORD=iotproject
   ```

4. **Build and start services**
   ```bash
   docker-compose up -d
   ```

5. **Verify deployment**
   ```bash
   docker-compose ps
   ```

The application will be available at:
- **API & Web UI**: http://localhost:40142
- **Swagger UI**: http://localhost:40142/swagger-ui.html
- **RabbitMQ Management**: http://localhost:40132 (credentials: iotproject/iotproject)

### Option 2: Manual Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/IoTServerApp.git
   cd IoTServerApp
   ```

2. **Set up PostgreSQL**
   ```bash
   createdb iot_db
   ```

3. **Set up RabbitMQ**
   ```bash
   # Enable MQTT plugin
   rabbitmq-plugins enable rabbitmq_mqtt rabbitmq_auth_backend_http
   
   # Create user
   rabbitmqctl add_user iotproject iotproject
   rabbitmqctl set_permissions -p / iotproject ".*" ".*" ".*"
   ```

4. **Configure application**
   
   Edit `src/main/resources/application.yaml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/iot_db
       username: postgres
       password: your-password
   
   mqtt:
     broker-url: tcp://localhost:1883
     username: iotproject
     password: iotproject
   
   jwt:
     secret: your-secret-key-here
   ```

5. **Build the application**
   ```bash
   ./mvnw clean package -DskipTests
   ```

6. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

## ⚙️ Configuration

### Environment Variables

The application can be configured using environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://postgres:5432/iot_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `password` |
| `MQTT_BROKER` | MQTT broker URL | `tcp://rabbitmq:1883` |
| `MQTT_USERNAME` | MQTT username | `iotproject` |
| `MQTT_PASSWORD` | MQTT password | `iotproject` |
| `JWT_SECRET` | JWT signing secret | (required) |
| `AWS_ACCESS_KEY_ID` | AWS access key for S3 | (optional) |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key for S3 | (optional) |
| `AWS_REGION` | AWS region | `us-east-1` |

### Configuration Files

- **[application.yaml](src/main/resources/application.yaml)** - Main application configuration
- **[application-test.yaml](src/main/resources/application-test.yaml)** - Test environment configuration
- **[docker-compose.yml](docker-compose.yml)** - Docker Compose configuration
- **[rabbitmq.conf](rabbitmq.conf)** - RabbitMQ configuration

### JWT Configuration

Generate a secure JWT secret:
```bash
openssl rand -hex 32
```

Set it in your environment or configuration file.

## 📚 API Documentation

### Swagger UI

Interactive API documentation is available at:
```
http://localhost:40142/swagger-ui.html
```

### API Endpoint Categories

#### Authentication (`/auth`)
- `POST /auth/login` - User login (returns access & refresh tokens)
- `POST /auth/refresh` - Refresh access token

#### User Management (`/users`)
- `POST /users` - Register new user
- `GET /users/me` - Get current user profile
- `PUT /users/me` - Update user profile
- `PUT /users/me/password` - Change password
- `DELETE /users/me` - Delete account

#### Lamp Control (`/lamps`)
- `GET /lamps` - List all user's lamps
- `POST /lamps` - Add new lamp
- `GET /lamps/{lampId}` - Get lamp details
- `PUT /lamps/{lampId}/name` - Update lamp name
- `DELETE /lamps/{lampId}` - Remove lamp
- `GET /lamps/{lampId}/status` - Get lamp status (Protobuf)
- `GET /lamps/{lampId}/config` - Get lamp configuration (Protobuf)
- `PUT /lamps/{lampId}/config` - Update lamp configuration (Protobuf)
- `POST /lamps/{lampId}/command` - Send command to lamp (Protobuf)
- `GET /lamps/{lampId}/metrics` - Get lamp metrics
- `GET /lamps/{lampId}/alerts` - Get lamp alerts
- `POST /lamps/{lampId}/alerts/{alertId}/acknowledge` - Acknowledge alert

#### Fleet Management (`/fleets`)
- `GET /fleets` - List all fleets
- `POST /fleets` - Create new fleet
- `GET /fleets/{fleetId}` - Get fleet details
- `PUT /fleets/{fleetId}` - Update fleet
- `DELETE /fleets/{fleetId}` - Delete fleet
- `POST /fleets/{fleetId}/lamps/{lampId}` - Add lamp to fleet
- `DELETE /fleets/{fleetId}/lamps/{lampId}` - Remove lamp from fleet
- `POST /fleets/{fleetId}/command` - Send command to all lamps in fleet

#### OTA Updates (`/ota`)
- `GET /ota/releases` - List firmware releases
- `POST /ota/releases` - Upload new firmware
- `GET /ota/releases/{version}` - Get firmware details
- `DELETE /ota/releases/{version}` - Delete firmware
- `GET /ota/check` - Check for updates (device endpoint)

#### Statistics (`/stats`)
- `GET /stats/global` - Get global statistics
- `GET /stats/user` - Get user statistics
- `GET /stats/fleet/{fleetId}` - Get fleet statistics
- `GET /stats/lamp/{lampId}/history` - Get lamp history

#### MQTT Authentication (`/api/mqtt/auth`)
- `POST /api/mqtt/auth/user` - Authenticate MQTT user
- `POST /api/mqtt/auth/vhost` - Authorize MQTT vhost
- `POST /api/mqtt/auth/resource` - Authorize MQTT resource
- `POST /api/mqtt/auth/topic` - Authorize MQTT topic

## 💡 Usage Examples

### User Registration and Login

```bash
# Register a new user
curl -X POST http://localhost:40142/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'

# Login to get JWT tokens
curl -X POST http://localhost:40142/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'

# Response:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
# }
```

### Adding a Lamp

```bash
# Add a new lamp to your account
curl -X POST http://localhost:40142/lamps \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "lampId": "LAMP001",
    "name": "Living Room Lamp"
  }'
```

### Controlling a Lamp

```bash
# Get lamp status (returns Protobuf binary data)
curl -X GET http://localhost:40142/lamps/LAMP001/status \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Accept: application/x-protobuf" \
  --output status.bin

# Send RGB color command (Protobuf)
# Note: This requires encoding the Protobuf message
curl -X POST http://localhost:40142/lamps/LAMP001/command \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/x-protobuf" \
  --data-binary @command.bin
```

### Creating a Fleet

```bash
# Create a new fleet
curl -X POST http://localhost:40142/fleets \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Office Lamps",
    "description": "All lamps in the office area"
  }'

# Add lamp to fleet
curl -X POST http://localhost:40142/fleets/1/lamps/LAMP001 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Send command to all lamps in fleet
curl -X POST http://localhost:40142/fleets/1/command \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/x-protobuf" \
  --data-binary @command.bin
```

### Checking for OTA Updates

```bash
# Device checks for firmware updates
curl -X GET "http://localhost:40142/ota/check?lampId=LAMP001&currentVersion=1.0.0" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Response:
# {
#   "updateAvailable": true,
#   "latestVersion": "1.1.0",
#   "downloadUrl": "https://s3.amazonaws.com/bucket/firmware-1.1.0.bin",
#   "releaseNotes": "Bug fixes and performance improvements"
# }
```

### Java/Spring Boot Client Example

```java
// Using RestTemplate
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(accessToken);
headers.setContentType(MediaType.APPLICATION_JSON);

// Login
LoginRequest loginRequest = new LoginRequest("admin", "password123");
HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);
AuthResponse response = restTemplate.postForObject(
    "http://localhost:40142/auth/login",
    request,
    AuthResponse.class
);

// Get lamp status
headers.setBearerAuth(response.getAccessToken());
HttpEntity<Void> entity = new HttpEntity<>(headers);
ResponseEntity<byte[]> statusResponse = restTemplate.exchange(
    "http://localhost:40142/lamps/LAMP001/status",
    HttpMethod.GET,
    entity,
    byte[].class
);

// Parse Protobuf response
IotProtos.StatusReport status = IotProtos.StatusReport.parseFrom(
    statusResponse.getBody()
);
```

## 📡 MQTT Topics

The application uses the following MQTT topic structure:

### Device → Server (Telemetry)

```
lamps/{lampId}/metrics
```
- **Purpose**: Device sends telemetry data (temperature, light, status)
- **Format**: Protocol Buffers (`StatusReport` message)
- **Frequency**: Configurable (default: every 60 seconds)

### Server → Device (Commands)

```
lamps/{lampId}/command
```
- **Purpose**: Server sends control commands to device
- **Format**: Protocol Buffers (`LampCommand` message)
- **QoS**: 1 (at least once delivery)

### Command Types

The system supports the following commands (see [iot_service.proto](src/main/resources/proto/iot_service.proto)):

- `SetDirectSettingsCommand` - Direct RGB/White LED control
- `SetPhotoWhiteSettingsCommand` - Photo mode white light
- `SetPhotoColorSettingsCommand` - Photo mode color light
- `SetMode` - Switch between modes (Daylight, Preset, Disco)
- `SetPresetCommand` - Activate a preset
- `BlinkLedCommand` - Blink LED for identification
- `DownloadOtaUpdateCommand` - Trigger OTA update
- `RebootCommand` - Reboot device
- `AcknowledgeAlert` - Acknowledge device alerts

### MQTT Authentication

Devices authenticate using the RabbitMQ HTTP authentication backend:
- Username: `{lampId}`
- Password: JWT token issued by the server

## 🔧 Protocol Buffers

The application uses Protocol Buffers for efficient binary communication with IoT devices.

### Proto Definition

The protocol is defined in [iot_service.proto](src/main/resources/proto/iot_service.proto):

```protobuf
message StatusReport {
  uint32 version = 1;
  int64 ts = 2;
  uint32 uptime_seconds = 3;
  repeated uint32 temperature_readings = 4;
  uint32 ambient_light = 5;
  uint32 ambient_noise = 6;
  optional DirectSettings led_settings = 7;
  bool is_abnormal = 8;
  bool has_more_alerts = 9;
  repeated Alert active_alerts = 10;
  string firmware_version = 11;
}

message LampCommand {
  uint32 version = 1;
  int64 ts = 2;
  oneof command {
    SetDirectSettingsCommand set_direct_settings_command = 5;
    SetPhotoWhiteSettingsCommand set_photo_white_settings_command = 6;
    // ... other commands
  }
}
```

### Generating Java Classes

Protocol Buffer classes are automatically generated during Maven build:

```bash
./mvnw clean compile
```

Generated classes are located in `target/generated-sources/protobuf/`.

### Using Protobuf in Code

```java
// Create a command
IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
    .setVersion(1)
    .setTs(System.currentTimeMillis() / 1000)
    .setSetDirectSettingsCommand(
        IotProtos.SetDirectSettingsCommand.newBuilder()
            .setDirectSettings(
                IotProtos.DirectSettings.newBuilder()
                    .setRed(255)
                    .setGreen(0)
                    .setBlue(0)
                    .build()
            )
            .build()
    )
    .build();

// Serialize to bytes
byte[] data = command.toByteArray();

// Deserialize from bytes
IotProtos.StatusReport status = IotProtos.StatusReport.parseFrom(data);
```

## 🧪 Testing

### Running All Tests

```bash
./mvnw test
```

### Running Specific Test Classes

```bash
# Unit tests
./mvnw test -Dtest=LampServiceTest

# Integration tests
./mvnw test -Dtest=LampControllerTest
```

### Test Coverage

The project includes comprehensive tests:

- **Unit Tests**: Service layer logic
  - [LampServiceTest.java](src/test/java/org/qualv13/iotbackend/service/LampServiceTest.java)
  - [FleetServiceTest.java](src/test/java/org/qualv13/iotbackend/service/FleetServiceTest.java)
  - [StatsServiceTest.java](src/test/java/org/qualv13/iotbackend/service/StatsServiceTest.java)

- **Integration Tests**: Full API testing with Testcontainers
  - [AuthControllerTest.java](src/test/java/org/qualv13/iotbackend/controller/AuthControllerTest.java)
  - [LampControllerTest.java](src/test/java/org/qualv13/iotbackend/controller/LampControllerTest.java)
  - [FleetControllerTest.java](src/test/java/org/qualv13/iotbackend/controller/FleetControllerTest.java)

- **Security Tests**: JWT and authentication
  - [JwtServiceTest.java](src/test/java/org/qualv13/iotbackend/security/JwtServiceTest.java)

### Test Configuration

Tests use H2 in-memory database and Testcontainers for PostgreSQL:

```yaml
# src/test/resources/application.yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
```

## 🔨 Development

### Project Structure

```
IoTServerApp/
├── src/
│   ├── main/
│   │   ├── java/org/qualv13/iotbackend/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── enums/           # Enumerations
│   │   │   ├── model/           # Domain models
│   │   │   ├── repository/      # Data repositories
│   │   │   ├── scheduler/       # Scheduled tasks
│   │   │   ├── security/        # Security configuration
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       ├── proto/           # Protocol Buffer definitions
│   │       ├── application.yaml # Main configuration
│   │       └── static/          # Static web resources
│   └── test/                    # Test classes
├── docker-compose.yml           # Docker Compose configuration
├── Dockerfile                   # Docker image definition
├── pom.xml                      # Maven configuration
└── README.md                    # This file
```

### Adding New Features

1. **Create Entity** (if needed)
   ```java
   @Entity
   @Table(name = "your_table")
   public class YourEntity {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       // fields, getters, setters
   }
   ```

2. **Create Repository**
   ```java
   public interface YourRepository extends JpaRepository<YourEntity, Long> {
       // custom query methods
   }
   ```

3. **Create Service**
   ```java
   @Service
   @RequiredArgsConstructor
   public class YourService {
       private final YourRepository repository;
       // business logic
   }
   ```

4. **Create Controller**
   ```java
   @RestController
   @RequestMapping("/your-endpoint")
   @RequiredArgsConstructor
   public class YourController {
       private final YourService service;
       // API endpoints
   }
   ```

5. **Add Tests**
   ```java
   @SpringBootTest
   class YourServiceTest {
       @Test
       void testYourFeature() {
           // test implementation
       }
   }
   ```

### Code Style

- Follow Java naming conventions
- Use Lombok annotations to reduce boilerplate
- Add JavaDoc comments for public APIs
- Keep methods focused and single-purpose
- Write tests for new features

### Debugging

Enable debug logging in `application.yaml`:

```yaml
logging:
  level:
    org.qualv13.iotbackend: DEBUG
    org.springframework.security: DEBUG
```

## 🚢 Deployment

### Production Considerations

1. **Security**
   - Use strong JWT secret (minimum 256 bits)
   - Enable HTTPS/TLS
   - Configure CORS properly
   - Use environment variables for secrets
   - Enable RabbitMQ authentication

2. **Database**
   - Use connection pooling
   - Configure proper indexes
   - Set up regular backups
   - Monitor query performance

3. **Monitoring**
   - Enable Spring Boot Actuator
   - Set up application logging
   - Monitor MQTT message rates
   - Track device health metrics

4. **Scaling**
   - Use load balancer for multiple instances
   - Configure session affinity if needed
   - Scale RabbitMQ cluster
   - Use read replicas for database

### Docker Production Deployment

```bash
# Build production image
docker build -t iot-server:latest .

# Run with production configuration
docker run -d \
  --name iot-server \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=${JWT_SECRET} \
  -e SPRING_DATASOURCE_URL=${DB_URL} \
  -e SPRING_DATASOURCE_USERNAME=${DB_USER} \
  -e SPRING_DATASOURCE_PASSWORD=${DB_PASS} \
  iot-server:latest
```

### Kubernetes Deployment

Example deployment configuration:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: iot-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: iot-server
  template:
    metadata:
      labels:
        app: iot-server
    spec:
      containers:
      - name: iot-server
        image: iot-server:latest
        ports:
        - containerPort: 8080
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: iot-secrets
              key: jwt-secret
```

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request


## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**qualv13**

- GitHub: [@qualv13](https://github.com/qualv13)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- RabbitMQ team for reliable messaging
- Protocol Buffers team for efficient serialization
- All contributors and users of this project
- My friends from Uni for help with Protobuf, MQTT and explaining how to implement my side of tasks
- Google for Gemini LLM (but not for Protobuf, it's hard)

---

**Note**: This is a learning/demonstration project. For production use, ensure proper security audits, load testing, and compliance with relevant regulations.

For questions, issues, or feature requests, please open an issue on GitHub.
