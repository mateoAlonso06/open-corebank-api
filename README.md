# Core Banking System

A backend core banking system built with **Java 21** and **Spring Boot 3.5**, following **Hexagonal Architecture** (Ports & Adapters). It provides RESTful APIs for user authentication, customer management with KYC workflows, multi-currency bank account operations, and financial transactions including deposits, withdrawals, and transfers.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Modules](#modules)
- [API Endpoints](#api-endpoints)
- [Observability](#observability)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Database Migrations](#database-migrations)
- [Testing](#testing)
- [Project Structure](#project-structure)

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.9 |
| Security | Spring Security 6, JWT (java-jwt 4.5.0), BCrypt |
| Database | PostgreSQL 16 |
| Cache / Rate Limiting | Redis 7, Bucket4j 8.10.1, Lettuce |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Mapping | MapStruct 1.6.3, Lombok |
| API Docs | SpringDoc OpenAPI 2.8.5 (Swagger UI) |
| Resilience | Resilience4j 2.2.0 (retry, circuit breaker) |
| Testing | JUnit 5, TestContainers 1.19.8, JaCoCo |
| Build | Maven |
| Containerization | Docker, Docker Compose |
| Observability | Prometheus, Grafana, Loki, Tempo |
| Notifications | Spring Mail + Thymeleaf templates (Mailtrap SMTP) |

## Architecture

The project follows **Hexagonal Architecture** with strict dependency rules — dependencies always point inward:

```
Infrastructure  →  Application  →  Domain
(adapters)         (services)      (business logic)
```

Each module is structured as:

```
<module>/
├── domain/                    # Pure business logic, no framework deps
│   ├── model/                 # Entities, value objects, enums
│   ├── port/
│   │   ├── in/               # Inbound ports (use case interfaces)
│   │   └── out/              # Outbound ports (repository interfaces)
│   ├── service/              # Domain services
│   └── exception/            # Domain-specific exceptions
├── application/               # Orchestration layer
│   ├── service/              # Use case implementations
│   ├── dto/                  # Commands, queries, responses
│   ├── mapper/               # Domain ↔ DTO mapping
│   └── event/                # Domain events
└── infraestructure/           # Framework & external dependencies
    ├── adapter/
    │   ├── in/rest/          # REST controllers
    │   └── out/persistence/  # JPA repository adapters
    └── config/               # Spring configuration
```

Key design decisions:
- **Value Objects** with self-validation (`Money`, `Email`, `PersonName`, `AccountNumber`)
- **Domain Events** for cross-module communication (`UserRegisteredEvent` → auto-creates Customer, triggers verification email)
- **Permission-based authorization** (not role-based) via `@PreAuthorize`
- **Idempotency keys** on transfers to prevent duplicate processing
- **Distributed rate limiting** via Redis + Bucket4j (token bucket algorithm) to protect API from abuse

## Modules

### Auth
User registration, login, JWT token management, email verification, password changes, and two-factor authentication (2FA) via email. Supports three roles: `CUSTOMER`, `ADMIN`, `BRANCH_MANAGER` with granular permissions. Refresh tokens stored in HttpOnly cookies.

### Customer
Customer profiles linked 1:1 to users. Manages KYC (Know Your Customer) approval workflows and risk level assessment (`LOW`, `MEDIUM`, `HIGH`). Name changes automatically reset KYC status to `PENDING`.

### Account
Bank account creation and management. Generates 22-digit account numbers and user-friendly aliases. Supports multiple account types (`SAVINGS`, `CHECKING`, `INVESTMENT`), multi-currency (`ARS`, `USD`), and tracks balance with available balance (accounting for holds). Enforces one USD account per customer.

### Transaction
Deposits, withdrawals, and transfers between accounts. Supports transaction statuses (`PENDING`, `COMPLETED`, `FAILED`, `REVERSED`), fee tracking, transfer categories, and reversal capabilities. Lookup by account alias or account number.

**Deposit & withdrawal limits by account type:**

| Type | Daily Deposit | Monthly Deposit | Daily Withdrawal | Monthly Withdrawal |
|---|---|---|---|---|
| SAVINGS | 500,000.00 | 5,000,000.00 | 200,000.00 | 2,000,000.00 |
| CHECKING | 1,000,000.00 | 10,000,000.00 | 500,000.00 | 5,000,000.00 |
| INVESTMENT | 2,000,000.00 | 20,000,000.00 | 1,000,000.00 | 10,000,000.00 |

These limits are domain constants (not configurable per account). Any operation that would exceed the daily or monthly accumulated total is rejected.

### Notification
Email notifications via Mailtrap SMTP. Sends verification emails on registration and welcome emails on account creation using Thymeleaf templates. Includes retry logic and circuit breaker patterns via Resilience4j.

### Audit
Audit trail infrastructure (database table in place, module scaffolded).

## API Endpoints

Full interactive documentation is available via Swagger UI:

- **Production:** [https://open-corebank.xyz/swagger-ui/index.html](https://open-corebank.xyz/swagger-ui/index.html)
- **Local:** `http://localhost:8080/swagger-ui/index.html` (requires `dev` profile)

All paths are prefixed with `/api/v1`.

| Module | Endpoints |
|---|---|
| **Auth** | Register, login, logout, token refresh, email verification, password change, forgot/reset password, 2FA toggle and verification |
| **Customers** | View and update own profile, admin CRUD, KYC approve/reject |
| **Accounts** | Create, view, close, balance inquiry, search by alias |
| **Transactions** | Deposits, withdrawals, transaction history |
| **Transfers** | Transfer between accounts (by account number or alias), transfer history |
| **Admin / Users** | Block and unblock user accounts |

## Observability

The project includes a full observability stack via Docker Compose:

| Service | Purpose | URL |
|---|---|---|
| Prometheus | Metrics collection | `http://localhost:9090` |
| Grafana | Dashboards and visualization | `http://localhost:3000` |
| Loki | Log aggregation | `http://localhost:3100` |
| Tempo | Distributed tracing (Zipkin-compatible) | `http://localhost:3200` |

```
Spring Boot App
    ├─ Metrics (Prometheus format) → Prometheus → Grafana
    ├─ Logs (Loki appender)        → Loki     → Grafana
    └─ Traces (Zipkin/OTLP)        → Tempo    → Grafana
```

Pre-configured Grafana datasources and dashboards are provisioned automatically from `observability/grafana/`. The observability profile is activated automatically when using Docker Compose.

Actuator endpoints are exposed on a separate management port (`9090`), isolated from the public API:
- `/actuator/health` — system health
- `/actuator/prometheus` — Prometheus-format metrics
- `/actuator/circuitbreakers` — circuit breaker status

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose (recommended) **or** PostgreSQL 16 + Redis 7

### Run with Docker Compose (recommended)

```bash
# 1. Clone the repository
git clone <repository-url>
cd core-banking-system

# 2. Create environment file
cp .env.example .env
# Edit .env with your values (see Environment Variables section)

# 3. Start all services (PostgreSQL, Redis, App + observability stack)
docker-compose up --build

# The API will be available at http://localhost:8080
# Grafana at http://localhost:3000
```

### Run locally

```bash
# 1. Start PostgreSQL (port 5432) and Redis (port 6379)
# 2. Create the database
createdb core_banking_db

# 3. Set environment variables or update application.yml

# 4. Build and run
mvn clean compile
mvn spring-boot:run
```

## Environment Variables

Copy `.env.example` to `.env` and configure:

| Variable | Description | Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring profile (`dev` or `prod`) | `dev` |
| `APP_PORT` | Application port | `8080` |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `core_banking_db` |
| `DB_USER` | Database user | `banking_user` |
| `DB_PASSWORD` | Database password | `your_secure_password` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | `openssl rand -base64 32` |
| `JWT_EXPIRATION_MS` | Token expiry in ms | `86400000` (24h) |
| `COOKIE_SECURE` | HTTPS-only cookies (`false` for local dev) | `true` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password (optional) | |
| `MAIL_HOST` | SMTP host | `sandbox.smtp.mailtrap.io` |
| `MAIL_PORT` | SMTP port | `2525` |
| `MAIL_USERNAME` | Mailtrap SMTP username | `your_username` |
| `MAIL_PASSWORD` | Mailtrap SMTP password | `your_password` |

## Database Migrations

Migrations are managed by Flyway and run automatically on startup. Files are located in `src/main/resources/db/migration/`:

| Migration | Description |
|---|---|
| V1 | Initial schema — users, roles, permissions, customers, accounts, transactions, transfers, audit_logs, email_verification_tokens |
| V2 | Seed data — default roles (CUSTOMER, ADMIN, BRANCH_MANAGER) with their associated permissions |
| V3 | Two-factor authentication — `two_factor_enabled` column on users, `two_factor_codes` table |
| V4 | Optimistic locking — `version` column on accounts to prevent concurrent modification race conditions |
| V5 | Password reset — `password_reset_tokens` table |

## Testing

The project uses Maven profiles to separate fast unit tests from slow integration tests (TestContainers):

```bash
# Run ONLY unit tests (default, fast)
mvn clean verify

# Run ONLY integration tests (uses TestContainers + PostgreSQL)
mvn clean verify -Pintegration-tests

# Run ALL tests (unit + integration)
mvn clean verify -Pall-tests

# Run with coverage report
mvn test jacoco:report
# Report at target/site/jacoco/index.html

# Run a specific test class
mvn test -Dtest=CustomerTest

# Run a specific integration test
mvn verify -Pintegration-tests -Dit.test=CustomerServiceIT
```

- **Unit tests** (`*Test.java`): Domain models, value objects, application services
- **Integration tests** (`*IT.java`): REST controllers with real PostgreSQL via TestContainers
- Coverage reporting via JaCoCo

## Project Structure

```
core-banking-system/
├── src/
│   ├── main/
│   │   ├── java/com/banking/system/
│   │   │   ├── auth/              # Authentication & authorization
│   │   │   ├── customer/          # Customer management & KYC
│   │   │   ├── account/           # Bank account operations
│   │   │   ├── transaction/       # Deposits, withdrawals, transfers
│   │   │   ├── notification/      # Email notifications (Thymeleaf)
│   │   │   ├── audit/             # Audit trail
│   │   │   └── common/            # Shared value objects & exceptions
│   │   └── resources/
│   │       ├── application.yml           # Default configuration
│   │       ├── application-dev.yml       # Development profile
│   │       ├── application-prod.yml      # Production profile
│   │       ├── db/migration/             # Flyway SQL migrations
│   │       └── templates/                # Thymeleaf email templates
│   └── test/                      # Unit & integration tests
├── observability/                 # Prometheus, Loki, Tempo, Grafana configs
│   ├── prometheus/prometheus.yml
│   ├── loki/loki-config.yml
│   ├── tempo/tempo.yml
│   └── grafana/                   # Datasources, dashboards
├── postman/                       # Postman collection for API testing
├── docs/                          # ADRs and domain invariants
├── docker-compose.yml
├── Dockerfile                     # Multi-stage build (JDK 21 → JRE 21)
└── pom.xml
```
