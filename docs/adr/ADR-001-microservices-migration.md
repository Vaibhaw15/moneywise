# ADR-001: Migrate MoneyWise from Monolith to Microservices Architecture

| Field         | Value                          |
|---------------|--------------------------------|
| **Status**    | Proposed                       |
| **Date**      | 2026-06-28                     |
| **Author**    | Vaibhaw Soni                   |
| **Deciders**  | Vaibhaw Soni                   |

---

## Context

MoneyWise is a personal finance tracker API built as a **single Spring Boot monolith**. All features — authentication, categories, transactions, history, and dashboard — live in one application, share one codebase, and connect to one PostgreSQL database.

### Current Monolith Structure

```
moneywise (single app — port 8080)
├── Auth        → LoginController, UserService, JWTUtil
├── Categories  → AppCategory, AppCategoryService
├── Transactions→ TransactionController, TransactionService
├── History     → HistoryController, HistoryService
└── Dashboard   → LandingController, LandingService
```

### Problems with the Current Approach

1. **Tight coupling** — A change in authentication logic requires redeploying the entire application, including unrelated transaction code.
2. **Scaling limitations** — Cannot scale transaction processing independently from authentication.
3. **Single point of failure** — A bug in one module (e.g., landing stats) can crash the entire application.
4. **Team scalability** — As the project grows, multiple developers working on the same monolith leads to merge conflicts and coordination overhead.

---

## Decision

We will **decompose the monolith into 5 microservices** using Spring Cloud, following a phased approach.

### Target Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Client Applications                   │
└──────────────────────┬──────────────────────────────────┘
                       │
              ┌────────▼────────┐
              │   API Gateway   │  ← JWT validation, routing, CORS
              │   (port 8080)   │
              └───┬────┬────┬───┘
                  │    │    │
        ┌─────────┘    │    └─────────┐
        │              │              │
  ┌─────▼─────┐ ┌─────▼──────┐ ┌─────▼───────────┐
  │   Auth    │ │  Category  │ │  Transaction     │
  │  Service  │ │  Service   │ │  Service         │
  │ (port 8081)│ │ (port 8082)│ │ (port 8083)     │
  └─────┬─────┘ └─────┬──────┘ └──┬──────────────┘
        │              │           │
        └──────────────┼───────────┘
                       │
              ┌────────▼────────┐
              │   PostgreSQL    │
              │   (port 5433)   │
              └─────────────────┘
              
  ┌─────────────────────────┐
  │   Service Discovery     │  ← All services register here
  │   Eureka (port 8761)    │
  └─────────────────────────┘
```

### Service Decomposition

| # | Service | Port | Responsibility | Source Files |
|---|---------|------|----------------|--------------|
| 1 | **Service Discovery** | `8761` | Service registration & discovery (Eureka) | New |
| 2 | **API Gateway** | `8080` | Route requests, validate JWT, handle CORS | New + `SecurityConfig`, `JWTAuthenticationFilter` |
| 3 | **Auth Service** | `8081` | User registration, login, JWT generation | `LoginController`, `UserService`, `UserRepository`, `JWTUtil` |
| 4 | **Category Service** | `8082` | Category & CategoryType management | `AppCategory`, `AppCategoryService`, `CategoryRepository`, `CategoryTypeRepository` |
| 5 | **Transaction Service** | `8083` | Transactions, history, landing/dashboard | `TransactionController`, `HistoryController`, `LandingController` + all related services |

---

## Detailed Design Decisions

### D1: Multi-Module Maven Monorepo

**Decision:** Use a single Git repository with a multi-module Maven parent POM.

**Rationale:**
- Simpler dependency management and versioning
- Easier local development (one `git clone`)
- Shared parent POM avoids duplicating Spring Boot / Spring Cloud versions
- Appropriate for a small team / solo developer

**Trade-off:** Separate repos per service would allow fully independent CI/CD pipelines, but that's premature for this project's scale.

### D2: Shared Database (Single PostgreSQL Instance)

**Decision:** All microservices connect to the **same PostgreSQL database** (`moneywise`).

**Rationale:**
- The current schema is small (4 tables) with clear ownership boundaries
- Avoids complexity of distributed data (sagas, eventual consistency)
- Transactions, history, and landing all query the same `app_transaction` table
- Migration to database-per-service can be done later if needed

**Trade-off:** Services are coupled at the database level. Direct table access from multiple services violates strict microservice boundaries. Acceptable at this scale.

### D3: JWT Validation at API Gateway Only

**Decision:** JWT token validation happens **only at the API Gateway**. Individual services trust requests that arrive through the gateway.

**Rationale:**
- Eliminates redundant JWT validation in every service
- Simplifies individual service security configuration
- Gateway adds `X-User-Name` and `X-User-Id` headers for downstream services
- `/auth/**` routes are exempted from JWT validation

**Trade-off:** If a service is accessed directly (bypassing the gateway), there's no auth check. Mitigated by ensuring services are only accessible within the Docker network.

### D4: Inter-Service Communication via OpenFeign (Synchronous REST)

**Decision:** Use **Spring Cloud OpenFeign** for synchronous REST calls between services.

**Rationale:**
- Transaction Service needs category data from Category Service (for history & landing)
- Feign provides declarative HTTP client with Eureka-based service discovery
- Simple and familiar REST pattern
- Built-in retry, fallback, and circuit breaker support (via Resilience4j)

**Trade-off:** Synchronous calls add network latency and create runtime coupling. Asynchronous messaging (Kafka/RabbitMQ) would decouple further but adds infrastructure complexity.

**Affected Code:**
- `HistoryService` — currently uses `CategoryRepository` directly → will use `CategoryClient` (Feign)
- `LandingService` — currently uses `CategoryRepository` + `CategoryTypeRepository` → will use `CategoryClient` (Feign)

### D5: Spring Cloud Gateway (Reactive)

**Decision:** Use **Spring Cloud Gateway** (reactive, non-blocking) as the API Gateway.

**Rationale:**
- First-class Spring Cloud integration
- Built-in support for Eureka service discovery
- Programmatic and declarative route configuration
- Better performance than Zuul (non-blocking I/O)

**Route Configuration:**

| Route Pattern | Target Service | Auth Required |
|---|---|---|
| `/moneywise/auth/**` | `auth-service` | ❌ No |
| `/moneywise/app-category/**` | `category-service` | ✅ Yes |
| `/moneywise/transaction/**` | `transaction-service` | ✅ Yes |
| `/moneywise/transaction-history/**` | `transaction-service` | ✅ Yes |
| `/moneywise/landing/**` | `transaction-service` | ✅ Yes |
| `/moneywise/actuator/**` | (all services) | ❌ No |

---

## File Migration Map

### Auth Service (`auth-service`)

| Source (monolith) | Destination | Action |
|---|---|---|
| `controller/LoginController.java` | `auth-service/controller/AuthController.java` | Move + Rename |
| `service/UserService.java` | `auth-service/service/UserService.java` | Move |
| `repository/UserRepository.java` | `auth-service/repository/UserRepository.java` | Move |
| `entity/User.java` | `auth-service/entity/User.java` | Move |
| `model/request/UserDTO.java` | `auth-service/model/request/UserDTO.java` | Move |
| `model/request/AuthRequest.java` | `auth-service/model/request/AuthRequest.java` | Move |
| `utils/JWTUtil.java` | `auth-service/utils/JWTUtil.java` | Move |
| `exceptions/InvalidUserException.java` | `auth-service/exceptions/InvalidUserException.java` | Move |

### Category Service (`category-service`)

| Source (monolith) | Destination | Action |
|---|---|---|
| `controller/AppCategory.java` | `category-service/controller/CategoryController.java` | Move + Rename |
| `service/AppCategoryService.java` | `category-service/service/CategoryService.java` | Move + Rename |
| `repository/CategoryRepository.java` | `category-service/repository/CategoryRepository.java` | Move |
| `repository/CategoryTypeRepository.java` | `category-service/repository/CategoryTypeRepository.java` | Move |
| `entity/Category.java` | `category-service/entity/Category.java` | Move |
| `entity/CategoryType.java` | `category-service/entity/CategoryType.java` | Move |
| — | `category-service/controller/CategoryTypeController.java` | **New** (expose types for Feign) |

### Transaction Service (`transaction-service`)

| Source (monolith) | Destination | Action |
|---|---|---|
| `controller/TransactionController.java` | `transaction-service/controller/TransactionController.java` | Move |
| `controller/HistoryController.java` | `transaction-service/controller/HistoryController.java` | Move |
| `controller/LandingController.java` | `transaction-service/controller/LandingController.java` | Move |
| `service/TransactionService.java` | `transaction-service/service/TransactionService.java` | Move |
| `service/HistoryService.java` | `transaction-service/service/HistoryService.java` | Move + **Modify** (Feign) |
| `service/LandingService.java` | `transaction-service/service/LandingService.java` | Move + **Modify** (Feign) |
| `repository/TransactionRepository.java` | `transaction-service/repository/TransactionRepository.java` | Move |
| `entity/Transaction.java` | `transaction-service/entity/Transaction.java` | Move |
| `model/request/TransactionRequestDto.java` | `transaction-service/model/request/TransactionRequestDto.java` | Move |
| `model/response/HistoryResponseDTO.java` | `transaction-service/model/response/HistoryResponseDTO.java` | Move |
| — | `transaction-service/client/CategoryClient.java` | **New** (Feign client) |
| — | `transaction-service/model/CategoryDTO.java` | **New** (DTO for Feign response) |
| — | `transaction-service/model/CategoryTypeDTO.java` | **New** (DTO for Feign response) |

---

## Project Structure (Final)

```
moneywise/
├── pom.xml                          ← Parent POM (packaging: pom)
├── docker-compose.yml               ← All services + PostgreSQL
├── seed_data.sh                     ← Database seeder
├── SETUP_AND_API_DOCS.md
├── docs/
│   └── adr/
│       └── ADR-001-microservices-migration.md
│
├── service-discovery/
│   ├── pom.xml
│   └── src/main/java/.../ServiceDiscoveryApplication.java
│
├── api-gateway/
│   ├── pom.xml
│   └── src/main/java/...
│       ├── ApiGatewayApplication.java
│       ├── config/GatewayConfig.java
│       └── filter/JWTAuthFilter.java
│
├── auth-service/
│   ├── pom.xml
│   └── src/main/java/...
│       ├── AuthServiceApplication.java
│       ├── controller/AuthController.java
│       ├── service/UserService.java
│       ├── repository/UserRepository.java
│       ├── entity/User.java
│       ├── model/request/UserDTO.java
│       ├── utils/JWTUtil.java
│       └── config/SecurityConfig.java
│
├── category-service/
│   ├── pom.xml
│   └── src/main/java/...
│       ├── CategoryServiceApplication.java
│       ├── controller/CategoryController.java
│       ├── controller/CategoryTypeController.java
│       ├── service/CategoryService.java
│       ├── repository/CategoryRepository.java
│       ├── repository/CategoryTypeRepository.java
│       ├── entity/Category.java
│       └── entity/CategoryType.java
│
└── transaction-service/
    ├── pom.xml
    └── src/main/java/...
        ├── TransactionServiceApplication.java
        ├── controller/TransactionController.java
        ├── controller/HistoryController.java
        ├── controller/LandingController.java
        ├── service/TransactionService.java
        ├── service/HistoryService.java
        ├── service/LandingService.java
        ├── repository/TransactionRepository.java
        ├── entity/Transaction.java
        ├── client/CategoryClient.java           ← Feign Client
        ├── model/CategoryDTO.java               ← Feign DTO
        └── model/CategoryTypeDTO.java           ← Feign DTO
```

---

## Consequences

### Positive
- **Independent deployability** — Each service can be deployed, scaled, and restarted independently
- **Technology flexibility** — Each service can evolve its own tech stack over time
- **Fault isolation** — A failure in one service doesn't crash others
- **Better scalability** — Scale transaction-heavy services without scaling auth

### Negative
- **Increased complexity** — More moving parts (5 services, service discovery, gateway)
- **Network latency** — Inter-service calls (Feign) are slower than in-process method calls
- **Operational overhead** — More containers to monitor, more logs to aggregate
- **Distributed debugging** — Tracing a request across services is harder

### Risks
| Risk | Mitigation |
|---|---|
| Category Service down → Transaction Service fails | Add Resilience4j circuit breaker + fallback cache |
| Shared DB schema change breaks multiple services | Strict DB ownership per service + migration scripts |
| Docker resource usage on local dev machine | Use lightweight JRE base images (eclipse-temurin:21-jre-alpine) |

---

## References

- [Spring Cloud Netflix Eureka](https://spring.io/projects/spring-cloud-netflix)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign)
- [ADR GitHub Organization](https://adr.github.io/)
- [Microservices Patterns by Chris Richardson](https://microservices.io/patterns/)
