# MoneyWise — Microservices Migration Guide

> Step-by-step implementation guide for converting the MoneyWise monolith into microservices.  
> **Reference:** [ADR-001](./adr/ADR-001-microservices-migration.md)

---

## Phase 1: Project Setup

### 1.1 Create Multi-Module Maven Structure

```bash
# From project root
mkdir -p service-discovery/src/main/java/com/moneywise/discovery
mkdir -p service-discovery/src/main/resources

mkdir -p api-gateway/src/main/java/com/moneywise/gateway/config
mkdir -p api-gateway/src/main/java/com/moneywise/gateway/filter
mkdir -p api-gateway/src/main/resources

mkdir -p auth-service/src/main/java/com/moneywise/auth/{controller,service,repository,entity,model/request,utils,exceptions,config}
mkdir -p auth-service/src/main/resources

mkdir -p category-service/src/main/java/com/moneywise/category/{controller,service,repository,entity,config}
mkdir -p category-service/src/main/resources

mkdir -p transaction-service/src/main/java/com/moneywise/transaction/{controller,service,repository,entity,model/request,model/response,client,config}
mkdir -p transaction-service/src/main/resources
```

### 1.2 Update Parent `pom.xml`

Convert the existing `pom.xml` to a parent POM:

```xml
<packaging>pom</packaging>

<modules>
    <module>service-discovery</module>
    <module>api-gateway</module>
    <module>auth-service</module>
    <module>category-service</module>
    <module>transaction-service</module>
</modules>

<!-- Add Spring Cloud BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2024.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## Phase 2: Service Discovery (Eureka Server)

### 2.1 `service-discovery/pom.xml`

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
</dependencies>
```

### 2.2 `ServiceDiscoveryApplication.java`

```java
@SpringBootApplication
@EnableEurekaServer
public class ServiceDiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceDiscoveryApplication.class, args);
    }
}
```

### 2.3 `application.properties`

```properties
server.port=8761
spring.application.name=service-discovery
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

### 2.4 Verify

```bash
cd service-discovery && mvn spring-boot:run
# Open http://localhost:8761 — Eureka dashboard should load
```

---

## Phase 3: API Gateway

### 3.1 `api-gateway/pom.xml`

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <!-- JWT libs for token validation -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

### 3.2 `application.properties`

```properties
server.port=8080
spring.application.name=api-gateway
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

# Route definitions
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=lb://auth-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/moneywise/auth/**

spring.cloud.gateway.routes[1].id=category-service
spring.cloud.gateway.routes[1].uri=lb://category-service
spring.cloud.gateway.routes[1].predicates[0]=Path=/moneywise/app-category/**

spring.cloud.gateway.routes[2].id=transaction-service
spring.cloud.gateway.routes[2].uri=lb://transaction-service
spring.cloud.gateway.routes[2].predicates[0]=Path=/moneywise/transaction/**

spring.cloud.gateway.routes[3].id=transaction-history
spring.cloud.gateway.routes[3].uri=lb://transaction-service
spring.cloud.gateway.routes[3].predicates[0]=Path=/moneywise/transaction-history/**

spring.cloud.gateway.routes[4].id=landing-service
spring.cloud.gateway.routes[4].uri=lb://transaction-service
spring.cloud.gateway.routes[4].predicates[0]=Path=/moneywise/landing/**
```

### 3.3 `JWTAuthFilter.java` (Global Gateway Filter)

```java
@Component
public class JWTAuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JWTUtil jwtUtil;

    private final List<String> openEndpoints = List.of("/moneywise/auth", "/actuator");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip auth for open endpoints
        if (openEndpoints.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtUtil.extractUsername(token);
            // Add user info as headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Name", username)
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
```

### 3.4 Verify

```bash
cd api-gateway && mvn spring-boot:run
# Should register on Eureka at http://localhost:8761
```

---

## Phase 4: Auth Service

### 4.1 Move Files

```
FROM (monolith)                              → TO (auth-service)
─────────────────────────────────────────────────────────────────
controller/LoginController.java              → controller/AuthController.java
service/UserService.java                     → service/UserService.java
repository/UserRepository.java               → repository/UserRepository.java
entity/User.java                             → entity/User.java
model/request/UserDTO.java                   → model/request/UserDTO.java
model/request/AuthRequest.java               → model/request/AuthRequest.java
utils/JWTUtil.java                           → utils/JWTUtil.java
exceptions/InvalidUserException.java         → exceptions/InvalidUserException.java
```

### 4.2 `application.properties`

```properties
server.port=8081
spring.application.name=auth-service
server.servlet.context-path=/moneywise

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

spring.datasource.url=jdbc:postgresql://localhost:5433/moneywise
spring.datasource.username=vaibhaw
spring.datasource.password=vaibhaw
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### 4.3 Simplified `SecurityConfig.java`

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // Gateway handles JWT
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 4.4 Verify

```bash
cd auth-service && mvn spring-boot:run

# Register
curl -X POST http://localhost:8081/moneywise/auth/createUser \
  -H "Content-Type: application/json" \
  -d '{"userName":"test","password":"test123","email":"test@test.com"}'

# Login
curl -X POST http://localhost:8081/moneywise/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test123"}'
```

---

## Phase 5: Category Service

### 5.1 Move Files

```
FROM (monolith)                              → TO (category-service)
──────────────────────────────────────────────────────────────────────
controller/AppCategory.java                  → controller/CategoryController.java
service/AppCategoryService.java              → service/CategoryService.java
repository/CategoryRepository.java           → repository/CategoryRepository.java
repository/CategoryTypeRepository.java       → repository/CategoryTypeRepository.java
entity/Category.java                         → entity/Category.java
entity/CategoryType.java                     → entity/CategoryType.java
```

### 5.2 Add New Endpoint — `CategoryTypeController.java`

Needed by Transaction Service via Feign:

```java
@RestController
@RequestMapping("/app-category")
public class CategoryTypeController {

    @Autowired
    private CategoryTypeRepository categoryTypeRepository;

    @GetMapping("/types")
    public ResponseEntity<Object> getAllCategoryTypes() {
        try {
            return new ResponseEntity<>(categoryTypeRepository.findAll(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getLocalizedMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
```

### 5.3 `application.properties`

```properties
server.port=8082
spring.application.name=category-service
server.servlet.context-path=/moneywise

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

spring.datasource.url=jdbc:postgresql://localhost:5433/moneywise
spring.datasource.username=vaibhaw
spring.datasource.password=vaibhaw
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### 5.4 Verify

```bash
cd category-service && mvn spring-boot:run

curl http://localhost:8082/moneywise/app-category/get
curl http://localhost:8082/moneywise/app-category/types
```

---

## Phase 6: Transaction Service

### 6.1 Move Files

```
FROM (monolith)                              → TO (transaction-service)
────────────────────────────────────────────────────────────────────────
controller/TransactionController.java        → controller/TransactionController.java
controller/HistoryController.java            → controller/HistoryController.java
controller/LandingController.java            → controller/LandingController.java
service/TransactionService.java              → service/TransactionService.java
service/HistoryService.java                  → service/HistoryService.java  ⚠️ MODIFY
service/LandingService.java                  → service/LandingService.java  ⚠️ MODIFY
repository/TransactionRepository.java        → repository/TransactionRepository.java
entity/Transaction.java                      → entity/Transaction.java
model/request/TransactionRequestDto.java     → model/request/TransactionRequestDto.java
model/response/HistoryResponseDTO.java       → model/response/HistoryResponseDTO.java
```

### 6.2 Add Feign Client — `CategoryClient.java` (NEW)

```java
@FeignClient(name = "category-service", path = "/moneywise")
public interface CategoryClient {

    @GetMapping("/app-category/get")
    List<CategoryDTO> getAllCategories();

    @GetMapping("/app-category/types")
    List<CategoryTypeDTO> getAllCategoryTypes();
}
```

### 6.3 Add DTOs for Feign Responses (NEW)

**`CategoryDTO.java`**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CategoryDTO {
    private Integer id;
    private String categoryName;
    private Integer categoryTypeId;
    private String categoryIcon;
    private boolean isActive;
}
```

**`CategoryTypeDTO.java`**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CategoryTypeDTO {
    private Integer id;
    private String categoryTypeName;
}
```

### 6.4 Modify `HistoryService.java`

Replace direct repository calls with Feign client:

```diff
- @Autowired
- private CategoryRepository categoryRepository;
- @Autowired
- private CategoryTypeRepository categoryTypeRepository;
+ @Autowired
+ private CategoryClient categoryClient;

  public Object getUserHistory(...) {
-     Map<Integer,Category> categoryIdNameMap = categoryRepository.findAll()
-         .stream().collect(Collectors.toMap(Category::getId, Function.identity()));
-     Map<Integer, String> categoryTypeIdNameMap = categoryTypeRepository.findAll()
-         .stream().collect(Collectors.toMap(CategoryType::getId, CategoryType::getCategoryTypeName));
+     Map<Integer, CategoryDTO> categoryIdNameMap = categoryClient.getAllCategories()
+         .stream().collect(Collectors.toMap(CategoryDTO::getId, Function.identity()));
+     Map<Integer, String> categoryTypeIdNameMap = categoryClient.getAllCategoryTypes()
+         .stream().collect(Collectors.toMap(CategoryTypeDTO::getId, CategoryTypeDTO::getCategoryTypeName));
  }
```

### 6.5 Modify `LandingService.java`

Same pattern — replace repository with Feign client:

```diff
- @Autowired
- private CategoryRepository categoryRepository;
- @Autowired
- private CategoryTypeRepository categoryTypeRepository;
+ @Autowired
+ private CategoryClient categoryClient;

  public Map<String, Object> getUserMonthlyStats(...) {
-     Map<Integer, Category> categoryIdMap = categoryRepository.findAll()
-         .stream().collect(Collectors.toMap(Category::getId, Function.identity()));
-     Map<Integer, String> categoryTypeIdNameMap = categoryTypeRepository.findAll()
-         .stream().collect(Collectors.toMap(CategoryType::getId, CategoryType::getCategoryTypeName));
+     Map<Integer, CategoryDTO> categoryIdMap = categoryClient.getAllCategories()
+         .stream().collect(Collectors.toMap(CategoryDTO::getId, Function.identity()));
+     Map<Integer, String> categoryTypeIdNameMap = categoryClient.getAllCategoryTypes()
+         .stream().collect(Collectors.toMap(CategoryTypeDTO::getId, CategoryTypeDTO::getCategoryTypeName));
  }
```

### 6.6 `application.properties`

```properties
server.port=8083
spring.application.name=transaction-service
server.servlet.context-path=/moneywise

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

spring.datasource.url=jdbc:postgresql://localhost:5433/moneywise
spring.datasource.username=vaibhaw
spring.datasource.password=vaibhaw
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### 6.7 Enable Feign in `TransactionServiceApplication.java`

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class TransactionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
```

### 6.8 Verify

```bash
cd transaction-service && mvn spring-boot:run

# Add transaction
curl -X POST http://localhost:8083/moneywise/transaction/addEdit \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"txnAmount":500,"txnCategoryId":6,"txnDate":"2026-06-28","txnMessage":"Test","txnDateInt":20260628,"isModify":0,"modifyCount":0}'

# Get history
curl "http://localhost:8083/moneywise/transaction-history/get?userId=1&startDate=20260601&endDate=20260630"
```

---

## Phase 7: Docker Compose (All Services)

### Updated `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16
    container_name: postgres-db
    restart: unless-stopped
    environment:
      POSTGRES_USER: vaibhaw
      POSTGRES_PASSWORD: vaibhaw
      POSTGRES_DB: moneywise
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U vaibhaw -d moneywise"]
      interval: 10s
      timeout: 5s
      retries: 5

  service-discovery:
    build: ./service-discovery
    container_name: service-discovery
    ports:
      - "8761:8761"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5

  api-gateway:
    build: ./api-gateway
    container_name: api-gateway
    ports:
      - "8080:8080"
    depends_on:
      service-discovery:
        condition: service_healthy
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/

  auth-service:
    build: ./auth-service
    container_name: auth-service
    ports:
      - "8081:8081"
    depends_on:
      postgres:
        condition: service_healthy
      service-discovery:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/moneywise
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/

  category-service:
    build: ./category-service
    container_name: category-service
    ports:
      - "8082:8082"
    depends_on:
      postgres:
        condition: service_healthy
      service-discovery:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/moneywise
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/

  transaction-service:
    build: ./transaction-service
    container_name: transaction-service
    ports:
      - "8083:8083"
    depends_on:
      postgres:
        condition: service_healthy
      service-discovery:
        condition: service_healthy
      category-service:
        condition: service_started
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/moneywise
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/

volumes:
  postgres_data:
```

### Dockerfile (same for each service)

Create a `Dockerfile` in each service directory:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE <PORT>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Replace `<PORT>` with the respective service port (8761, 8080, 8081, 8082, 8083).

---

## Phase 8: Startup Order & Verification

### Startup Order (Local Dev — IntelliJ IDEA)

Since this is now a multi-module Maven project, you can manage all services directly from IntelliJ IDEA:

1. **Import Project:**
   - Open IntelliJ IDEA and go to `File > Open`.
   - Select the root `moneywise` directory (which contains the parent `pom.xml`).
   - IntelliJ will automatically detect it as a multi-module Maven project and load all 5 sub-modules.
2. **Setup Run Dashboard / Services Tool Window:**
   - Open the **Services** tool window (`View > Tool Windows > Services` or `Alt+8` / `Cmd+8`).
   - Click the `+` button -> `Run Configuration Type` -> select `Spring Boot`.
   - IntelliJ will automatically detect the 5 `@SpringBootApplication` main classes and list them.
3. **Start the Database:**
   - You must still run the PostgreSQL database locally. You can use the provided Docker Compose file just for the DB: `docker compose up -d postgres`
4. **Boot Order (Crucial):**
   - **First:** Start `ServiceDiscoveryApplication` (Wait for it to fully initialize and show "Started Eureka Server").
   - **Second:** Start `ApiGatewayApplication`.
   - **Third:** Start `AuthServiceApplication`, `CategoryServiceApplication`.
   - **Last:** Start `TransactionServiceApplication` (since it depends on Feign resolution of Category Service).

### Startup Order (Local Dev — without Docker CLI)

```bash
# Terminal 1 — Service Discovery (start FIRST, wait for it to be UP)
cd service-discovery && mvn spring-boot:run

# Terminal 2 — API Gateway
cd api-gateway && mvn spring-boot:run

# Terminal 3 — Auth Service
cd auth-service && mvn spring-boot:run

# Terminal 4 — Category Service
cd category-service && mvn spring-boot:run

# Terminal 5 — Transaction Service (start LAST — depends on Category Service)
cd transaction-service && mvn spring-boot:run
```

### Startup Order (Docker — one command)

```bash
docker compose up -d
# Docker Compose handles ordering via depends_on + healthcheck
```

### End-to-End Verification

```bash
# 1. Check Eureka dashboard — all 4 services should be registered
open http://localhost:8761

# 2. Register a user (via gateway)
curl -X POST http://localhost:8080/moneywise/auth/createUser \
  -H "Content-Type: application/json" \
  -d '{"userName":"test","password":"test123","email":"test@test.com"}'

# 3. Login (via gateway)
TOKEN=$(curl -s -X POST http://localhost:8080/moneywise/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test123"}' | jq -r '.token')

# 4. Get categories (via gateway → category-service)
curl http://localhost:8080/moneywise/app-category/get \
  -H "Authorization: Bearer $TOKEN"

# 5. Add transaction (via gateway → transaction-service)
curl -X POST http://localhost:8080/moneywise/transaction/addEdit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":1,"txnAmount":500,"txnCategoryId":6,"txnDate":"2026-06-28","txnMessage":"Test","txnDateInt":20260628,"isModify":0,"modifyCount":0}'

# 6. Get history (via gateway → transaction-service → category-service via Feign)
curl "http://localhost:8080/moneywise/transaction-history/get?userId=1&startDate=20260601&endDate=20260630" \
  -H "Authorization: Bearer $TOKEN"

# 7. Get landing stats (via gateway → transaction-service → category-service via Feign)
curl "http://localhost:8080/moneywise/landing/get?userId=1&startDate=20260601&endDate=20260630" \
  -H "Authorization: Bearer $TOKEN"

# 8. Health check
curl http://localhost:8080/moneywise/actuator/health
```

---

## Checklist

- [ ] Phase 1 — Create multi-module Maven structure + parent POM
- [ ] Phase 2 — Service Discovery (Eureka)
- [ ] Phase 3 — API Gateway + JWT filter + route config
- [ ] Phase 4 — Auth Service (move files + simplified security)
- [ ] Phase 5 — Category Service (move files + new types endpoint)
- [ ] Phase 6 — Transaction Service (move files + Feign client)
- [ ] Phase 7 — Docker Compose + Dockerfiles for all services
- [ ] Phase 8 — End-to-end testing via API Gateway
- [ ] Update `SETUP_AND_API_DOCS.md` for microservices
- [ ] Update `seed_data.sh` if DB changes needed
