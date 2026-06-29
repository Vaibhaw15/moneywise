# Circuit Breaker — MoneyWise Microservices

## What is a Circuit Breaker?

In a microservices architecture, services depend on each other. For example, in MoneyWise, the **Transaction Service** calls the **Category Service** via Feign to fetch category data. If the Category Service goes down or becomes extremely slow, without protection the Transaction Service would also hang and eventually crash — this is called **cascading failure**.

A **Circuit Breaker** prevents cascading failures by monitoring the health of inter-service calls and "tripping" (opening) if too many failures are detected. When the circuit is open, calls are immediately redirected to a **fallback** method instead of waiting for a timeout.

Think of it exactly like the circuit breaker in your house — if too much current flows through the wire, the breaker trips to protect the entire house from catching fire.

---

## Circuit Breaker States

```
    ┌──────────────────┐
    │     CLOSED       │  ← Normal operation
    │  (Requests flow) │
    └──────┬───────────┘
           │ Failure rate exceeds threshold (e.g., 50%)
           ▼
    ┌──────────────────┐
    │      OPEN        │  ← All requests go to fallback
    │  (Tripped/Down)  │
    └──────┬───────────┘
           │ After wait-duration (e.g., 10 seconds)
           ▼
    ┌──────────────────┐
    │    HALF-OPEN     │  ← Allow a few test requests
    │  (Testing)       │
    └──────┬───────────┘
           │ If test requests succeed → CLOSED
           │ If test requests fail   → OPEN
           ▼
```

| State | Behavior |
|-------|----------|
| **CLOSED** | All requests flow normally. Failures are counted. If the failure rate exceeds the threshold, the circuit opens. |
| **OPEN** | All requests immediately return the fallback response. No calls are made to the downstream service. |
| **HALF-OPEN** | A limited number of test requests are allowed through. If they succeed, the circuit closes. If they fail, it opens again. |

---

## Implementation in MoneyWise

We use **Resilience4j** (the recommended circuit breaker library for Spring Cloud) at two levels:

### 1. Transaction Service → Category Service (Feign Client Level)

The `CategoryClient` Feign interface has a `fallback` class (`CategoryClientFallback`). When the Category Service is down:
- `getAllCategories()` returns an **empty list** instead of throwing an exception.
- `getAllCategoryTypes()` returns an **empty list** instead of throwing an exception.

This means the Landing and History APIs will still respond (with zeroed-out or empty data) instead of returning a 500 error.

**Files:**
- `transaction-service/src/main/java/com/moneywise/transaction/client/CategoryClient.java` — Feign client with `fallback` attribute.
- `transaction-service/src/main/java/com/moneywise/transaction/client/CategoryClientFallback.java` — Fallback implementation.
- `transaction-service/src/main/resources/application.properties` — Resilience4j configuration.

### 2. API Gateway → All Downstream Services (Route Level)

Each route in the API Gateway has a `CircuitBreaker` filter. If a downstream service is completely unreachable, the gateway forwards the request to a local `FallbackController` which returns a clean JSON error:

```json
{
  "error": "Transaction Service is temporarily unavailable. Please try again later.",
  "status": 503,
  "timestamp": "2026-06-29T20:30:00"
}
```

**Files:**
- `api-gateway/src/main/java/com/moneywise/gateway/controller/FallbackController.java` — Fallback REST controller.
- `api-gateway/src/main/resources/application.properties` — Route filters and Resilience4j config.

---

## Configuration Parameters

| Parameter | Value | Meaning |
|-----------|-------|---------|
| `sliding-window-size` | `10` | Evaluate the last 10 calls to determine health. |
| `failure-rate-threshold` | `50` | Open the circuit if 50% or more of those calls fail. |
| `wait-duration-in-open-state` | `10s` | Wait 10 seconds before transitioning to half-open. |
| `permitted-number-of-calls-in-half-open-state` | `3` | Allow 3 test calls when half-open. |
| `minimum-number-of-calls` | `5` | Need at least 5 calls before the circuit breaker can evaluate (prevents tripping on the very first failure). |

---

## How to Test the Circuit Breaker

### Step 1: Start everything normally
```bash
docker compose up -d postgres zipkin
```
Then start all 5 microservices in IntelliJ.

### Step 2: Verify normal operation
Use Postman to hit `GET /api/v1/transaction/landing/get` — you should get normal data.

### Step 3: Kill the Category Service
Stop **only** the `CategoryServiceApplication` in IntelliJ.

### Step 4: Hit the Landing API again
Send `GET /api/v1/transaction/landing/get` again.

**What you should see:**
- Instead of a 500 Internal Server Error, you will get a response with **zeroed-out stats** (income: 0, expense: 0, etc.) because the `CategoryClientFallback` returned empty lists.
- In the Transaction Service console, you will see a warning log: `Circuit Breaker OPEN — category-service is unavailable. Returning empty categories list.`

### Step 5: Restart the Category Service
Start `CategoryServiceApplication` again in IntelliJ.

### Step 6: Wait and retry
After ~10 seconds (the `wait-duration-in-open-state`), the circuit breaker will transition to **HALF-OPEN** and allow test calls. Once those succeed, the circuit closes and normal data flows again!

---

## Monitoring

You can monitor the circuit breaker state via the Spring Boot Actuator endpoint:

```
GET http://localhost:8083/api/v1/transaction/actuator/health
```

The response will include circuit breaker health details showing the current state (CLOSED, OPEN, or HALF_OPEN).
