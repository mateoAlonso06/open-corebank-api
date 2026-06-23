# Core Banking System

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot)
![Maven](https://img.shields.io/badge/Build-Maven-red?logo=apachemaven)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL%2016-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

A **portfolio-grade backend system** that models a real-world core banking platform. Built with **Java 21** and **Spring Boot 3.5**, it showcases how to apply **Hexagonal Architecture** (Ports & Adapters) in a complex financial domain — covering user authentication, KYC workflows, multi-currency accounts, and financial transactions.

The goal is not just to build features, but to do so with production-quality engineering: domain-driven design, distributed rate limiting, idempotency, optimistic locking, observability, and a full test strategy.

> **Live App:** [app.open-corebank.xyz](https://app.open-corebank.xyz) &nbsp;|&nbsp; **API Docs:** [open-corebank.xyz/swagger-ui](https://open-corebank.xyz/swagger-ui/index.html)

---

## Table of Contents

- [Highlights](#highlights)
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

## Highlights

These are the design decisions that go beyond a standard CRUD API:

- **Value Objects with self-validation** — `Money`, `Email`, `PersonName`, `AccountNumber` enforce invariants at the type level, preventing invalid state from entering the domain.
- **Domain Events for loose coupling** — registering a user triggers a `UserRegisteredEvent` that automatically creates a Customer profile and sends a verification email, without the auth module knowing about the others.
- **Permission-based authorization** — fine-grained `@PreAuthorize` checks instead of role-only access control, giving precise control over what each role can do.
- **Idempotency keys on transfers** — clients can safely retry failed requests without risk of double-processing.
- **Distributed rate limiting** — Redis + Bucket4j (token bucket algorithm) protects the API at the infrastructure level.
- **Optimistic locking on accounts** — prevents race conditions on concurrent balance modifications without pessimistic database locks.
- **Full observability stack** — metrics, logs, and distributed traces wired up out of the box via Docker Compose.

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
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

The domain layer has zero framework dependencies. Application services orchestrate use cases. Infrastructure adapters handle persistence, HTTP, and external services — and can be swapped without touching business logic.

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

## Modules

### Auth
User registration, login, JWT token management, email verification, password changes, and two-factor authentication (2FA) via email. Supports three roles — `CUSTOMER`, `ADMIN`, `BRANCH_MANAGER` — with granular permission-based access control. Refresh tokens are stored in HttpOnly cookies.

### Customer
Customer profiles linked 1:1 to users. Manages KYC (Know Your Customer) approval workflows and risk level assessment (`LOW`, `MEDIUM`, `HIGH`). Name changes automatically reset KYC status to `PENDING` to enforce re-verification.

### Account
Bank account creation and management. Generates 22-digit account numbers and user-friendly aliases. Supports multiple account types (`SAVINGS`, `CHECKING`, `INVESTMENT`), multi-currency (`ARS`, `USD`), and tracks both ledger balance and available balance (accounting for holds). Enforces one USD account per customer.

### Transaction
Deposits, withdrawals, and transfers between accounts. Supports transaction statuses (`PENDING`, `COMPLETED`, `FAILED`, `REVERSED`), fee tracking, transfer categories, and reversal capabilities. Accounts can be looked up by alias or account number.

**Transaction limits by account type:**

| Type | Daily Deposit | Monthly Deposit | Daily Withdrawal | Monthly Withdrawal |
|---|---|---|---|---|
| SAVINGS | 500,000 | 5,000,000 | 200,000 | 2,000,000 |
| CHECKING | 1,000,000 | 10,000,000 | 500,000 | 5,000,000 |
| INVESTMENT | 2,000,000 | 20,000,000 | 1,000,000 | 10,000,000 |

Limits are enforced as domain constants. Any operation that would exceed the daily or monthly accumulated total is rejected before hitting the database.

### Notification
Email notifications via Mailtrap SMTP. Sends verification emails on registration and welcome emails on account creation, rendered with Thymeleaf templates. Retry logic and circuit breaker patterns via Resilience4j ensure delivery reliability.

## API Endpoints

Full interactive documentation is available via Swagger UI:

- **Production:** [app.open-corebank.xyz](https://app.open-corebank.xyz)
- **Swagger:** [https://open-corebank.xyz/swagger-ui/index.html](https://open-corebank.xyz/swagger-ui/index.html)
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

| Service | Purpose | Local URL |
|---|---|---|
| Prometheus | Metrics collection | `http://localhost:9090` |
| Grafana | Dashboards and visualization | `http://localhost:3000` |
| Loki | Log aggregation | `http://localhost:3100` |
| Tempo | Distributed tracing (Zipkin-compatible) | `http://localhost:3200` |

```
Spring Boot App
    ├─ Metrics (Prometheus format) → Prometheus → Grafana
    ├─ Logs (Loki appender)        → Loki       → Grafana
    └─ Traces (Zipkin/OTLP)        → Tempo      → Grafana
```

Grafana datasources and dashboards are provisioned automatically from `observability/grafana/` — no manual setup required. The observability profile activates automatically when running via Docker Compose.

Spring Boot Actuator is exposed on a separate management port (`9090`), isolated from the public API:
- `/actuator/health` — system health
- `/actuator/prometheus` — Prometheus-format metrics
- `/actuator/circuitbreakers` — circuit breaker status

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose (recommended) **or** PostgreSQL 16 + Redis 7 running locally

### Run with Docker Compose (recommended)

```bash
# 1. Clone the repository
git clone https://github.com/mateoAlonso06/core-banking-api.git
cd core-banking-api

# 2. Create the environment file
cp .env.example .env
# Edit .env with your values — see the Environment Variables section below

# 3. Start all services (app + PostgreSQL + Redis + observability stack)
docker-compose up --build
```

The API will be available at `http://localhost:8080` and Grafana at `http://localhost:3000`.

### Run locally (without Docker)

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

Copy `.env.example` to `.env` and fill in your values:

| Variable | Description | Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `dev` |
| `APP_PORT` | Application port | `8080` |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `core_banking_db` |
| `DB_USER` | Database user | `banking_user` |
| `DB_PASSWORD` | Database password | `your_secure_password` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | `openssl rand -base64 32` |
| `JWT_EXPIRATION_MS` | Token expiry in milliseconds | `86400000` (24h) |
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

Managed by Flyway and run automatically on startup. Migration files live in `src/main/resources/db/migration/`:

| Migration | Description |
|---|---|
| V1 | Initial schema — users, roles, permissions, customers, accounts, transactions, transfers, audit_logs, email_verification_tokens |
| V2 | Seed data — default roles (`CUSTOMER`, `ADMIN`, `BRANCH_MANAGER`) with their associated permissions |
| V3 | Two-factor authentication — `two_factor_enabled` column on users, `two_factor_codes` table |
| V4 | Optimistic locking — `version` column on accounts to prevent concurrent modification race conditions |
| V5 | Password reset — `password_reset_tokens` table |

## Testing

Tests are split into profiles to keep the feedback loop fast during development:

```bash
# Unit tests only (default — fast, no external dependencies)
mvn clean verify

# Integration tests only (uses TestContainers + real PostgreSQL)
mvn clean verify -Pintegration-tests

# Full test suite
mvn clean verify -Pall-tests

# Coverage report (output: target/site/jacoco/index.html)
mvn test jacoco:report

# Run a specific test class
mvn test -Dtest=CustomerTest

# Run a specific integration test
mvn verify -Pintegration-tests -Dit.test=CustomerServiceIT
```

| Type | Convention | Scope |
|---|---|---|
| Unit tests | `*Test.java` | Domain models, value objects, application services |
| Integration tests | `*IT.java` | REST controllers with real PostgreSQL via TestContainers |

TestContainers is configured with container reuse — the first integration test run takes ~30–60 seconds to pull and start the container; subsequent runs take ~10–15 seconds.

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
│   └── grafana/                   # Datasources & dashboards (auto-provisioned)
├── postman/                       # Postman collection for API testing
├── docs/                          # ADRs and domain invariants
├── docker-compose.yml
├── Dockerfile                     # Multi-stage build (JDK 21 → JRE 21)
└── pom.xml
```
