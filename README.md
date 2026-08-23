<p align="center">
  <!-- <img src="./assets/iotserverapp-banner.png" alt="IoTServerApp banner" width="100%" /> -->
  <img width="742" height="41" alt="{E1082DD7-1500-4994-BE77-0B693C5D4F1A}" src="https://github.com/user-attachments/assets/87591812-89db-4a8a-9ba1-9f54df3c56c1" />

</p> 

<h1 align="center">IoTServerApp</h1>

<p align="center">
  Full-stack backend platform for managing connected smart lamps with secure APIs, MQTT-based device communication, telemetry ingestion, and OTA firmware delivery.
</p>

<p align="center">
  <a href="https://github.com/qualv13/IoTServerApp/actions/workflows/ci.yml"><img src="https://github.com/qualv13/IoTServerApp/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
</p>

<p align="center">
  <a href="https://github.com/qualv13/IoTServerApp"><img src="https://img.shields.io/badge/GitHub-IoTServerApp-181717?style=for-the-badge&logo=github" alt="GitHub"></a>
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.3.5">
  <img src="https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL 15">
  <img src="https://img.shields.io/badge/RabbitMQ-MQTT-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" alt="RabbitMQ MQTT">
  <img src="https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
  <img src="https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="OpenAPI">
</p>

---

## Overview [Live frontend](https://iot-frontend-2r8o.onrender.com/) . Screenshots below

**IoTServerApp** is a Spring Boot backend for operating IoT-enabled smart lamps at scale. It combines secure user management, fleet-level device orchestration, real-time MQTT messaging, telemetry processing, and firmware lifecycle management in one platform.

This project demonstrates practical backend engineering across:
- secure authentication and authorization,
- event-driven device communication,
- binary device protocols with Protocol Buffers,
- cloud-backed OTA delivery,
- operational monitoring and scheduling,
- containerized local deployment.



## Value Proposition

For an IoT product, the backend is where reliability, security, and maintainability either hold together or fail. IoTServerApp addresses that by providing:

- **Centralized device control** for individual lamps and fleets
- **Low-latency command delivery** over MQTT
- **Operational visibility** through telemetry and alert tracking
- **Secure access** with JWT-based authentication
- **Firmware rollout support** through OTA update workflows
- **Production-oriented deployment** with Docker, PostgreSQL, and RabbitMQ

---

## Features

### Core platform capabilities
- Secure **JWT authentication** with access/refresh token flow
- **User management** and profile update endpoints
- **Lamp registration** and lifecycle management
- **Fleet management** for grouping and bulk operations
- **Real-time lamp control** via MQTT command topics
- **Telemetry ingestion** from device metrics topics
- **Statistics endpoints** for lamp, fleet, and user-level insights
- **OTA firmware update** workflow with S3-backed artifact delivery
- **RabbitMQ-backed MQTT broker** integration
- **Swagger / OpenAPI** documentation support

### Smart lighting capabilities
- RGB and white-channel control
- Photo white and photo color modes
- Preset-based lighting workflows
- Disco effects:
  - Color cycle
  - Strobe
  - Pulse
- Circadian/daylight scheduling
- Adaptive brightness configuration
- Scheduled smart feature execution

### Engineering highlights
- **Protocol Buffers** for compact device payloads
- **Layered Spring architecture** with controllers, services, repositories, DTOs, and entities
- **Containerized local stack** with PostgreSQL + RabbitMQ + backend
- **Test support** with JUnit 5, Spring test tooling, H2, and Testcontainers

---

## Tech Stack

| Category | Technologies |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security, JWT, BCrypt |
| Data | Spring Data JPA, PostgreSQL, H2 |
| Messaging | RabbitMQ, MQTT, Spring Integration, Spring AMQP, Eclipse Paho |
| Serialization | Protocol Buffers |
| Cloud Storage | AWS S3 SDK |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build Tool | Maven |
| Testing | JUnit 5, Testcontainers, Spring Security Test, Spring Rabbit Test |
| Deployment | Docker, Docker Compose |

### Stack badges
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![Protocol Buffers](https://img.shields.io/badge/Protocol_Buffers-Serialization-4285F4?style=flat-square&logo=google&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-Storage-FF9900?style=flat-square&logo=amazonaws&logoColor=white)

---

## Architecture Overview

The application follows a layered architecture pattern:  

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Applications                     │
│              (Web UI, Mobile Apps, IoT Devices)             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                           │
│  (Controllers: Auth, User, Lamp, Fleet, OTA, Stats)         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Service Layer                             │
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

### High-level flow

```text
Client Apps / Admin UI
        |
        v
 REST API (Spring Boot Controllers)
        |
        v
 Service Layer
  |        |         |
  |        |         +--> OTA / S3 integration
  |        |
  |        +------------> MQTT / RabbitMQ messaging
  |
  +---------------------> JPA Repositories / PostgreSQL

IoT Devices
  | 
  +--> publish metrics / alerts
  +--> receive commands / OTA instructions
```

### Verified backend modules

```text
config/
controller/
dto/
entity/
repository/
scheduler/
security/
service/
resources/proto/
```

### Notable controllers
- `AuthController`
- `UserController`
- `LampController`
- `FleetController`
- `StatsController`
- `OtaController`
- `RabbitAuthController`

### Notable services
- `LampService`
- `FleetService`
- `StatsService`
- `MqttService`
- `MqttListenerService`
- `OtaService`
- `RabbitMqListener`

---

## Project Structure

```text
src/main/java/org/qualv13/iotbackend/
├── config/          # Spring, MQTT, RabbitMQ, S3, OpenAPI, web config
├── controller/      # REST endpoints
├── dto/             # Request/response DTOs
├── entity/          # JPA entities
├── enums/           # Alert levels and causes
├── repository/      # Spring Data repositories
├── scheduler/       # Scheduled smart/device tasks
├── security/        # JWT filter, JWT service, security config
├── service/         # Business logic and integrations
└── IoTServerAppApplication.java

src/main/resources/
├── application.yaml
├── application-test.yaml
├── application.properties
├── proto/iot_service.proto
└── static/index.html
```

---

## Installation and Setup

## Prerequisites

- Java 17
- Maven 3.9+
- Docker and Docker Compose
- PostgreSQL 15
- RabbitMQ with MQTT enabled

## Option 1: Run with Docker Compose

The repository includes a multi-service setup for:
- PostgreSQL
- RabbitMQ management + MQTT
- Spring Boot backend

```bash
git clone https://github.com/qualv13/IoTServerApp.git
cd IoTServerApp
docker network create iot-net
docker compose up --build
```

### Exposed ports from the verified compose file

| Service | Port | Purpose |
|---|---:|---|
| Backend | `40142` | REST API |
| RabbitMQ Management | `40132` | Broker admin UI |
| MQTT | `40131` | Device MQTT traffic |

## Option 2: Run locally with Maven

```bash
git clone https://github.com/qualv13/IoTServerApp.git
cd IoTServerApp
mvn spring-boot:run
```

If running locally without Docker, update datasource and MQTT settings to match your local services.

---

## Configuration

The verified runtime configuration includes:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/iot_db
    username: postgres
    password: password

mqtt:
  broker-url: tcp://rabbitmq-mqtt-kierzno:1883
  username: iotproject
  password: iotproject

jwt:
  secret: <hex-secret>
```

### Recommended environment variables

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/iot_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=password

export MQTT_BROKER=tcp://localhost:1883
export MQTT_USERNAME=iotproject
export MQTT_PASSWORD=iotproject

export JWT_SECRET=your_jwt_secret_here
```

> Security note: the repository contains development defaults. For any serious deployment, move secrets to environment variables or a secret manager.

---

## Usage Examples

## Authentication example

```bash
curl -X POST http://localhost:40142/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "your-password"
  }'
```

## Example authenticated request

```bash
curl http://localhost:40142/api/lamps \
  -H "Authorization: Bearer <access-token>"
```

## Example lamp command payload concept

The project uses Protocol Buffers for device communication. A simplified command intent looks like:

```proto
message LampCommand {
  uint32 version = 1;
  int64 ts = 2;
  oneof command {
    SetDirectSettingsCommand set_direct_settings_command = 5;
    SetMode set_mode_command = 8;
    DownloadOtaUpdateCommand download_ota_update_command = 10;
    SetPresetCommand set_preset_command = 12;
  }
}
```

## Example MQTT topic pattern

```text
lamps/{lampId}/command
lamps/{lampId}/metrics
```

The verified config uses wildcard subscriptions:

```text
lamps/+/command
lamps/+/metrics
```

---

## Device Protocol

The repository includes a protobuf contract at:

```text
src/main/resources/proto/iot_service.proto
```

### Supported message families
- `StatusReport`
- `Alert`
- `LampConfig`
- `LampCommand`

### Supported command types
- Set Wi-Fi parameters
- Blink LED
- Set direct RGB/white settings
- Set photo white settings
- Set photo color settings
- Change mode
- Reboot device
- Download OTA update
- Register lamp
- Set preset
- Acknowledge alerts

This is a strong design choice for IoT systems because it keeps payloads compact and explicit while preserving schema evolution options.

---

## API Documentation

The project includes **SpringDoc OpenAPI** support.

### Expected API areas
| Area | Purpose |
|---|---|
| Auth | Login, token refresh, registration |
| Users | Profile and account operations |
| Lamps | Device CRUD and control |
| Fleets | Grouping and fleet-level operations |
| Stats | Metrics and analytics |
| OTA | Firmware release and update workflows |

---

## Screenshots

<img width="975" height="603" alt="{0E937B49-EE0F-4392-AC27-F23BC36D0C92}" src="https://github.com/user-attachments/assets/364879cb-8b04-44ca-99ff-9ca314b19aea" />
<img width="977" height="499" alt="{D0EBA7BA-3A24-48D6-9A9B-B734DA10D8EB}" src="https://github.com/user-attachments/assets/9778d1f5-09e3-4afd-b815-f71209f22cae" />
<img width="977" height="704" alt="{3835CA20-0F10-4DFF-B295-DFE83A1EE8C0}" src="https://github.com/user-attachments/assets/48a40c73-6bff-4d61-8492-3129c537c991" />
<img width="976" height="497" alt="{1F5F3150-00D2-4B65-A061-0B85E26CB180}" src="https://github.com/user-attachments/assets/90b1b016-7639-4aa7-9768-a01cee8672f6" />
<img width="975" height="615" alt="{C8767108-1B7D-450F-B8BF-FE0876E55505}" src="https://github.com/user-attachments/assets/fe7c94a8-b140-40e8-bff5-b24354553915" />

---

## Testing

```bash
mvn test
```

9 tests run on every push, against H2 in memory with the MQTT client mocked
out. No database, no broker and no credentials required, which is the point:
a test that only passes on the machine it was written on is not a test.

| Class | Covers | Tests |
| --- | --- | --- |
| `AuthControllerTest` | login, refresh, account deletion | 2 |
| `FleetControllerTest` | fleet creation and lamp assignment | 2 |
| `JwtServiceTest` | token issue and validation | 2 |
| `FleetServiceTest` | fleet service logic | 1 |
| `LampMetricRepositoryTest` | metric queries | 1 |
| `IoTServerAppApplicationTests` | the context starts | 1 |

### Still parked

Six more classes in `src/test` are commented out, and they are not
decoration: uncommented, they compile and run, they just do not pass yet.

| Class | Why it is parked |
| --- | --- |
| `LampControllerTest`, `LampControllerProtoTest`, `OtaControllerTest`, `UserControllerTest` | assertions written against older response shapes |
| `LampServiceTest` | `LampService` gained two constructor arguments (`LampAlertRepository`, `MqttService`) |
| `RabbitMqListenerTest` | one assertion drifted from the current listener |
| `StatsServiceTest` | `getUserStats` returns `DetailedStatsDto` now, with a different API |
| `MqttListenerServiceTest` | its subject, `MqttListenerService`, is itself commented out; the MQTT path moved to `RabbitMqListener` |

That is 7 failing assertions, not 7 broken features. Each one is a decision
about what the endpoint should return today, which is why they are listed here
instead of being quietly deleted.

---

## Deployment

## Docker image build

```bash
docker build -t iotserverapp .
```

## Compose deployment

```bash
docker network create iot-net
docker compose up -d --build
```

## Production hardening checklist
- Replace default JWT secret
- Replace default database credentials
- Restrict CORS
- Secure RabbitMQ management access
- Use managed secret storage
- Add observability and structured logging
- Add CI/CD validation for tests and image builds

---

## Contributing

Contributions are welcome.

### Suggested workflow
1. Fork the repository
2. Create a feature branch
3. Make focused changes
4. Add or update tests
5. Open a pull request with implementation notes

### Commit guidance
- Keep commits small and reviewable
- Document API or protocol changes
- Include setup notes for infra-related changes

---

## License

The current public README references **MIT**.

---

## Contact and Links

- GitHub profile: [qualv13](https://github.com/qualv13)
- Repository: [qualv13/IoTServerApp](https://github.com/qualv13/IoTServerApp)
