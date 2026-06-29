# Circuit Breaker — Complete Notes

> A comprehensive guide covering what a Circuit Breaker is, why we need it in microservices, how it works internally, and how we implemented it in the MoneyWise project.

---

## 1. What is a Circuit Breaker?

A **Circuit Breaker** is a design pattern used in microservices to **prevent cascading failures**. It works just like an electrical circuit breaker in your house — if too much current flows (too many failures happen), the breaker **trips** to protect the entire system from going down.

### The Problem it Solves

In a monolith, if one method fails, you get a simple exception. But in microservices, services call each other over the **network**. Networks can be slow, unreliable, or a downstream service can crash entirely.

**Without a Circuit Breaker:**
```
User Request
    → API Gateway (✅ healthy)
        → Transaction Service (✅ healthy)
            → Category Service (❌ CRASHED)
                ← Feign waits 30 seconds... timeout
            ← Transaction Service thread is BLOCKED
        ← All threads get consumed waiting
    ← Transaction Service ALSO CRASHES 💀
← API Gateway returns 500 to ALL users
```

**With a Circuit Breaker:**
```
User Request
    → API Gateway (✅ healthy)
        → Transaction Service (✅ healthy)
            → Circuit Breaker checks: "Category Service is DOWN"
            → Immediately returns FALLBACK (empty list)
            ← Response in 1ms instead of 30 seconds!
        ← Transaction Service stays HEALTHY ✅
    ← User sees partial data (better than no data)
```

---

## 2. How Does it Work? (The 3 States)

A Circuit Breaker has **3 states**, just like a traffic light:

```
                    ┌─────────────────┐
         ┌─────────│     CLOSED      │─────────┐
         │         │  (Normal Flow)  │         │
         │         └────────┬────────┘         │
         │                  │                  │
         │    Failure rate > 50%               │
         │    (e.g., 5 out of 10 calls fail)   │
         │                  │                  │
         │                  ▼                  │
         │         ┌─────────────────┐         │
         │         │      OPEN       │         │
         │         │  (All Blocked)  │         │
         │         └────────┬────────┘         │
         │                  │                  │
         │    Wait 10 seconds                  │
         │                  │                  │
         │                  ▼                  │
         │         ┌─────────────────┐         │
         │         │   HALF-OPEN     │         │
         └─────────│  (Testing 3     │─────────┘
    Tests pass     │   requests)     │    Tests fail
    → go CLOSED    └─────────────────┘    → go OPEN
```

### State Details

| State | What Happens | When It Transitions |
|-------|-------------|-------------------|
| **🟢 CLOSED** | All requests flow normally to the downstream service. Every response (success/failure) is recorded in a sliding window. | → Goes to **OPEN** when the failure rate exceeds the threshold (e.g., 50% of the last 10 calls failed). |
| **🔴 OPEN** | All requests are **immediately rejected** — they don't even try to call the downstream service. Instead, the **fallback method** is called instantly. | → Goes to **HALF-OPEN** after the wait duration expires (e.g., 10 seconds). |
| **🟡 HALF-OPEN** | A **limited number of test requests** (e.g., 3) are allowed through to the downstream service. The circuit breaker is "testing" whether the service has recovered. | → Goes to **CLOSED** if test requests succeed. → Goes back to **OPEN** if test requests fail. |

---

## 3. Key Configuration Parameters

These are the settings you configure in `application.properties`:

| Parameter | What It Means | Our Value | Example |
|-----------|--------------|-----------|---------|
| `sliding-window-size` | How many recent calls to evaluate | `10` | Look at the last 10 calls |
| `failure-rate-threshold` | Percentage of failures to trigger OPEN | `50` | If 5 out of 10 calls fail → OPEN |
| `wait-duration-in-open-state` | How long to stay OPEN before testing | `10s` | Wait 10 seconds, then try again |
| `permitted-number-of-calls-in-half-open-state` | How many test calls in HALF-OPEN | `3` | Allow 3 test calls through |
| `minimum-number-of-calls` | Minimum calls before evaluating | `5` | Don't trip on the very first failure |
| `sliding-window-type` | COUNT_BASED or TIME_BASED | `COUNT_BASED` | Count the last N calls |

### What does `minimum-number-of-calls` mean?

If you set it to `5`, the circuit breaker will NOT open even if the first 3 calls all fail. It needs at least 5 calls to have enough data to evaluate the failure rate. This prevents **false positives** (tripping the circuit because of 1 unlucky failure at startup).

### What does `sliding-window-type` mean?

- **COUNT_BASED**: Evaluates the last N calls (e.g., last 10 calls regardless of when they happened).
- **TIME_BASED**: Evaluates all calls within a time window (e.g., all calls in the last 60 seconds).

---

## 4. Implementation in MoneyWise

We implemented the Circuit Breaker at **two levels**:

### Level 1: Feign Client (Transaction Service → Category Service)

This protects the `transaction-service` from crashing when `category-service` is down.

#### Step 1: Add the Dependency

In `transaction-service/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

#### Step 2: Create a Fallback Class

`CategoryClientFallback.java` — This class implements the same interface as `CategoryClient` and provides safe default return values:

```java
@Slf4j
@Component
public class CategoryClientFallback implements CategoryClient {

    @Override
    public List<CategoryDTO> getAllCategories() {
        log.warn("Circuit Breaker OPEN — category-service is unavailable.");
        return Collections.emptyList();  // Return empty list instead of crashing
    }

    @Override
    public List<CategoryTypeDTO> getAllCategoryTypes() {
        log.warn("Circuit Breaker OPEN — category-service is unavailable.");
        return Collections.emptyList();
    }
}
```

#### Step 3: Wire the Fallback to the Feign Client

In `CategoryClient.java`, add the `fallback` attribute:
```java
@FeignClient(
    name = "category-service",
    path = "/api/v1/category",
    fallback = CategoryClientFallback.class  // ← This is the magic line
)
public interface CategoryClient {
    @GetMapping("/get")
    List<CategoryDTO> getAllCategories();

    @GetMapping("/types")
    List<CategoryTypeDTO> getAllCategoryTypes();
}
```

#### Step 4: Enable Feign Circuit Breaker

In `application.properties`:
```properties
# Enable circuit breaker for Feign clients
spring.cloud.openfeign.circuitbreaker.enabled=true

# Configure the circuit breaker instance
resilience4j.circuitbreaker.instances.category-service.sliding-window-size=10
resilience4j.circuitbreaker.instances.category-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.category-service.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.category-service.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.category-service.minimum-number-of-calls=5
```

#### Step 5: Handle Null in Service Layer

Since the fallback returns empty lists, your service code must handle the case where a category lookup returns `null`:

```java
// ❌ WRONG — will crash with NullPointerException when fallback returns empty list
categoryIdNameMap.get(txnOne.getTransactionCategoryId()).getCategoryName();

// ✅ CORRECT — safely handles null
CategoryDTO category = categoryIdNameMap.get(txnOne.getTransactionCategoryId());
String categoryName = category != null ? category.getCategoryName() : "Unknown";
```

---

### Level 2: API Gateway (Route-Level Circuit Breaker)

This protects the **gateway** itself from hanging when any downstream service is completely unreachable.

#### Step 1: Add the Dependency

In `api-gateway/pom.xml` (note: **reactor** version for the reactive Gateway):
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

#### Step 2: Create a Fallback Controller

`FallbackController.java` — Returns a clean JSON error response:
```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public Mono<Map<String, Object>> authFallback() {
        return buildFallbackResponse("Auth Service");
    }

    @GetMapping("/category")
    public Mono<Map<String, Object>> categoryFallback() {
        return buildFallbackResponse("Category Service");
    }

    @GetMapping("/transaction")
    public Mono<Map<String, Object>> transactionFallback() {
        return buildFallbackResponse("Transaction Service");
    }

    private Mono<Map<String, Object>> buildFallbackResponse(String serviceName) {
        return Mono.just(Map.of(
            "error", serviceName + " is temporarily unavailable.",
            "status", 503,
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

#### Step 3: Add Circuit Breaker Filters to Routes

In `application.properties`:
```properties
# Each route gets a CircuitBreaker filter with a fallback URI
spring.cloud.gateway.routes[0].filters[0].name=CircuitBreaker
spring.cloud.gateway.routes[0].filters[0].args.name=authCircuitBreaker
spring.cloud.gateway.routes[0].filters[0].args.fallbackUri=forward:/fallback/auth
```

#### Step 4: Exclude Fallback Path from JWT Filter

The `/fallback` path is internal to the gateway. The JWT filter must NOT block it:
```java
private final List<String> openEndpoints = List.of(
    "/api/v1/auth", "/actuator", "/fallback"  // ← Add /fallback here
);
```

---

## 5. How to Test the Circuit Breaker

### Test 1: Feign-Level (Transaction → Category)

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Start all services normally | Everything works, real category names appear |
| 2 | Stop `CategoryServiceApplication` in IntelliJ | Category Service goes offline |
| 3 | Hit `GET /api/v1/transaction/history/get?userId=1&startDate=20260601&endDate=20260630` | Response returns with `"categoryName": "Unknown"` instead of crashing |
| 4 | Check IntelliJ console for Transaction Service | You should see: `WARN: Circuit Breaker OPEN — category-service is unavailable` |
| 5 | Start `CategoryServiceApplication` again | Category Service comes back online |
| 6 | Wait 10 seconds, then hit the same API | Real category names like "Salary", "Income" appear again |

### Test 2: Gateway-Level

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Stop `CategoryServiceApplication` in IntelliJ | Category Service goes offline |
| 2 | Hit `GET /api/v1/category/get` through the Gateway | Returns `{"error": "Category Service is temporarily unavailable"}` with status 503 |
| 3 | Start `CategoryServiceApplication` again | After ~10s, returns real category data |

---

## 6. Real-World Best Practices

### When to use a Circuit Breaker
- ✅ **Synchronous HTTP calls** between microservices (Feign, RestTemplate, WebClient).
- ✅ **Database connections** to external databases.
- ✅ **Third-party API calls** (payment gateways, email services, etc.).

### When NOT to use a Circuit Breaker
- ❌ **Asynchronous messaging** (Kafka, RabbitMQ) — messages are queued, not blocked.
- ❌ **Local method calls** — no network involved, no cascading failure risk.

### Production Configuration Tips
| Setting | Development | Production |
|---------|------------|------------|
| `failure-rate-threshold` | 50% | 50-70% |
| `wait-duration-in-open-state` | 10s | 30s-60s |
| `sliding-window-size` | 10 | 50-100 |
| `minimum-number-of-calls` | 5 | 10-20 |

### Monitoring in Production
In production, you should connect Resilience4j to monitoring tools like **Prometheus + Grafana** to visualize:
- How often circuits are opening/closing.
- Which services are failing the most.
- Average response times for each circuit breaker.

---

## 7. Summary Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                         USER REQUEST                             │
│                    (from Postman / Frontend)                      │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                       API GATEWAY                                │
│  ┌─────────────────────────────────────────┐                     │
│  │  Circuit Breaker Filter (per route)     │                     │
│  │  If downstream service is DOWN:         │                     │
│  │  → forward to /fallback/{service}       │                     │
│  │  → Returns clean 503 JSON response      │                     │
│  └─────────────────────────────────────────┘                     │
└──────────────────────────┬───────────────────────────────────────┘
                           │ (if service is UP)
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
      ┌──────────┐  ┌──────────┐  ┌──────────────┐
      │   Auth   │  │ Category │  │ Transaction  │
      │ Service  │  │ Service  │  │   Service    │
      └──────────┘  └──────────┘  └──────┬───────┘
                                         │
                                         │ Feign Call
                                         ▼
                              ┌──────────────────────┐
                              │  Circuit Breaker      │
                              │  (Feign-Level)        │
                              │                       │
                              │  If Category Service  │
                              │  is DOWN:             │
                              │  → CategoryClient     │
                              │    Fallback returns   │
                              │    empty lists        │
                              └──────────────────────┘
```

---

## 8. Files Modified in MoneyWise

| File | What Was Changed |
|------|-----------------|
| `transaction-service/pom.xml` | Added `spring-cloud-starter-circuitbreaker-resilience4j` |
| `transaction-service/.../client/CategoryClient.java` | Added `fallback = CategoryClientFallback.class` |
| `transaction-service/.../client/CategoryClientFallback.java` | **NEW** — Returns empty lists as fallback |
| `transaction-service/.../service/HistoryService.java` | Added null-safety checks for category lookups |
| `transaction-service/src/main/resources/application.properties` | Added Resilience4j circuit breaker config |
| `api-gateway/pom.xml` | Added `spring-cloud-starter-circuitbreaker-reactor-resilience4j` |
| `api-gateway/.../controller/FallbackController.java` | **NEW** — Returns 503 JSON fallback |
| `api-gateway/.../filter/JWTAuthFilter.java` | Added `/fallback` to open endpoints |
| `api-gateway/src/main/resources/application.properties` | Added CircuitBreaker filters to routes |
