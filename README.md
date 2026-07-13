# Intelligent Takeout System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.3-brightgreen)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-21-orange)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

A full-stack intelligent takeout ordering platform built with Spring Boot, featuring AI-powered meal recommendations, real-time order processing, and WeChat Pay integration.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Key Optimizations](#key-optimizations)
- [Deployment](#deployment)
- [License](#license)

## Features

### Admin Console
- Employee authentication & role management
- Category management (dish categories, combo categories)
- Dish & combo CRUD with flavor configuration
- Order lifecycle management (accept, prepare, deliver, complete)
- Business analytics (revenue, user growth, top-selling items)
- Store operating status control

### Customer App (WeChat Mini Program)
- WeChat OAuth login
- Dish browsing & shopping cart
- Address book management
- Order placement & payment via WeChat Pay
- Order history & status tracking
- **AI-powered meal plan recommendations** — multi-agent reasoning via StateGraph

### System Optimizations
- **BCrypt password encryption** (migrated from MD5 with backward compatibility)
- **Redis distributed lock** — prevents duplicate order submission under concurrency
- **API rate limiting** — Redis sliding window + Lua script for per-user throttling
- **Null-value caching** — prevents cache penetration for non-existent data
- **MDC-based request tracing** — distributed log correlation with audit trail
- **Order timeout cancellation** — configurable TTL with batch SQL updates
- **JWT interceptor deduplication** — template method pattern for cleaner auth chain
- **Service-layer unit tests** — Mockito + AssertJ coverage for core business logic

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Framework** | Spring Boot 2.7.3, Spring MVC |
| **ORM** | MyBatis 2.2.0 |
| **Database** | MySQL 8.0 |
| **Connection Pool** | Druid 1.2.1 |
| **Cache** | Redis |
| **Security** | JWT (jjwt 0.9.1), BCrypt (spring-security-crypto) |
| **API Docs** | Knife4j 3.0.2 (Swagger) |
| **AI/LLM** | LangChain4j 1.0.0-beta1, StateGraph multi-agent engine |
| **Payments** | WeChat Pay API v3 |
| **File Storage** | Alibaba Cloud OSS |
| **Pagination** | PageHelper 1.3.0 |
| **JSON** | Fastjson 1.2.76 |
| **AOP** | AspectJ 1.9.4 |

## Project Structure

```
sky-take-out/
├── sky-common/          # Shared utilities, constants, exceptions, interceptors
│   └── src/main/java/com/sky/
│       ├── constant/        # Application constants
│       ├── context/         # Thread-local context (BaseContext)
│       ├── exception/       # Global exception handling
│       ├── interceptor/     # JWT authentication interceptor
│       ├── json/            # JSON serialization utilities
│       ├── properties/      # Configuration property classes
│       └── utils/           # Utility classes (JWT, HttpClient, Redis, etc.)
├── sky-pojo/            # Data transfer objects, entities, view objects
│   └── src/main/java/com/sky/
│       ├── dto/             # Data Transfer Objects
│       ├── entity/          # Database entity classes
│       └── vo/              # View Objects
└── sky-server/          # Core application module
    └── src/main/java/com/sky/
        ├── controller/      # REST controllers (admin + user)
        ├── service/         # Business logic layer
        ├── mapper/          # MyBatis mapper interfaces
        ├── annotation/      # Custom annotations (rate limiting)
        ├── aspect/          # AOP aspects (rate limiting, logging)
        ├── config/          # Spring configuration classes
        ├── task/            # Scheduled tasks (order timeout)
        └── websocket/       # WebSocket configuration
```

## Getting Started

### Prerequisites

- **JDK 21**
- **Maven 3.8+**
- **MySQL 8.0**
- **Redis 6.0+**

### Installation

1. **Clone the repository**

```bash
git clone git@github.com:Peter287351/sky-take-out.git
cd sky-take-out
```

2. **Set up the database**

Execute the SQL scripts in order to initialize tables and seed data.

3. **Configure environment variables**

Copy and edit the configuration files under `sky-server/src/main/resources/`:

- `application-dev.yml` — development profile
- `application-secrets.yml` — sensitive credentials (excluded from Git)

4. **Build & Run**

```bash
mvn clean package -DskipTests
java -jar sky-server/target/sky-server.jar
```

The server starts at `http://localhost:8080`.

## Configuration

Key configuration properties (set via environment variables or `application-secrets.yml`):

```yaml
# Database
sky.datasource.host=localhost
sky.datasource.port=3306
sky.datasource.database=sky_take_out
sky.datasource.username=root
sky.datasource.password=your_password

# Redis
sky.redis.host=localhost
sky.redis.port=6379
sky.redis.password=your_redis_password

# JWT
sky.jwt.admin-secret-key=your_admin_secret
sky.jwt.user-secret-key=your_user_secret

# WeChat Pay
sky.wechat.appid=your_wechat_appid
sky.wechat.secret=your_wechat_secret
sky.wechat.mchid=your_merchant_id

# Alibaba Cloud OSS
sky.alioss.endpoint=oss-cn-hangzhou.aliyuncs.com
sky.alioss.access-key-id=your_access_key
sky.alioss.access-key-secret=your_access_secret
sky.alioss.bucket-name=your_bucket
```

## API Documentation

Once the application is running, access the Swagger UI:

- **Admin APIs**: `http://localhost:8080/doc.html`
- **Customer APIs**: `http://localhost:8080/doc.html`

## Key Optimizations

This project goes beyond the basic takeout workflow with production-grade enhancements:

| # | Module | Description |
|---|--------|-------------|
| 1 | **BCrypt Migration** | Upgraded password hashing from MD5 to BCrypt with backward-compatible verification |
| 2 | **Distributed Lock** | Redis `SETNX` + `TransactionSynchronizationManager` prevents duplicate orders |
| 3 | **Rate Limiting** | Redis sliding window + Lua atomic scripting for per-user API throttling |
| 4 | **Null-Value Cache** | Caches empty results to prevent cache penetration on frequently queried missing data |
| 5 | **MDC Log Tracing** | Injects trace IDs into request context for end-to-end log correlation |
| 6 | **Unit Tests** | Mockito + AssertJ test suite covering BCrypt/MD5 compatibility and delete validations |
| 7 | **Order Timeout** | Configurable TTL cancellation with batch SQL for efficiency |
| 8 | **JWT Interceptor Dedup** | Template method pattern consolidates admin/user auth interceptors |
| 9 | **AI Meal Planner** | LangChain4j + StateGraph multi-node reasoning engine for personalized dish recommendations |

## Deployment

### Docker Compose (Recommended)

```bash
docker-compose up -d
```

This starts the application along with MySQL, Redis, and Nginx reverse proxy.

### Manual

```bash
mvn clean package -DskipTests
java -jar sky-server/target/sky-server.jar --spring.profiles.active=prod,secrets
```

## License

This project is licensed under the MIT License.
