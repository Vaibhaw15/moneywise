# 💰 MoneyWise — Setup & API Documentation

A personal finance tracker API built with **Spring Boot**, **PostgreSQL**, and **JWT Authentication**.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Database Setup (Docker)](#database-setup-docker)
- [Running the Application](#running-the-application)
- [Seeding Default Data](#seeding-default-data)
- [API Reference](#api-reference)
  - [Authentication](#1--authentication)
  - [Categories](#2--categories)
  - [Transactions](#3--transactions)
  - [Landing / Dashboard](#4--landing--dashboard)
  - [Transaction History](#5--transaction-history)
  - [Health Check](#6--health-check)
- [Database Schema](#database-schema)
- [Default Seed Data](#default-seed-data)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Tool       | Version | Purpose            |
|------------|---------|--------------------|
| Java       | 17+     | Runtime            |
| Maven      | 3.8+    | Build tool         |
| Docker     | 20+     | PostgreSQL database|
| Git        | 2.x     | Version control    |

---

## Quick Start

```bash
# 1. Clone the repo
git clone https://github.com/Vaibhaw15/moneywise.git
cd moneywise

# 2. Start PostgreSQL via Docker
docker compose up -d

# 3. Run the Spring Boot app
mvn spring-boot:run

# 4. (In a new terminal) Seed default data
./seed_data.sh

# 5. App is running at:
#    http://localhost:8080/moneywise
```

---

## Database Setup (Docker)

### Option A — Using Docker Compose (Recommended)

```bash
# Start the database
docker compose up -d

# Check it's running
docker compose ps

# View logs
docker compose logs -f postgres

# Stop the database
docker compose down

# Stop AND delete all data
docker compose down -v
```

### Option B — Manual Docker Run

```bash
docker run --name postgres-db \
  -e POSTGRES_USER=vaibhaw \
  -e POSTGRES_PASSWORD=vaibhaw \
  -e POSTGRES_DB=moneywise \
  -p 5433:5432 \
  -d postgres:16
```

### Database Connection Details

| Property | Value                                          |
|----------|-------------------------------------------------|
| Host     | `localhost`                                     |
| Port     | `5433`                                          |
| Database | `moneywise`                                     |
| Username | `vaibhaw`                                       |
| Password | `vaibhaw`                                       |
| JDBC URL | `jdbc:postgresql://localhost:5433/moneywise`     |

### Connect to the Database Manually

```bash
# Open psql shell inside the container
docker exec -it postgres-db psql -U vaibhaw -d moneywise

# Run a query
docker exec -it postgres-db psql -U vaibhaw -d moneywise -c "SELECT * FROM app_users;"
```

---

## Running the Application

```bash
# Build and run
mvn spring-boot:run

# Or build a JAR and run it
mvn clean package -DskipTests
java -jar target/moneywise-0.0.1-SNAPSHOT.jar
```

The app starts at: **`http://localhost:8080/moneywise`**

> **Note:** All API endpoints are prefixed with `/moneywise` (configured via `server.servlet.context-path`).

---

## Seeding Default Data

> **Important:** The Spring Boot app must be running first so Hibernate creates the tables (`ddl-auto=update`).

### Step 1 — Make the script executable (first time only)
```bash
chmod +x seed_data.sh
```

### Step 2 — Run the seed script
```bash
./seed_data.sh
```

### Step 3 — Verify data
```bash
docker exec -it postgres-db psql -U vaibhaw -d moneywise -c "SELECT * FROM app_category_type;"
docker exec -it postgres-db psql -U vaibhaw -d moneywise -c "SELECT * FROM app_category;"
docker exec -it postgres-db psql -U vaibhaw -d moneywise -c "SELECT * FROM app_users;"
docker exec -it postgres-db psql -U vaibhaw -d moneywise -c "SELECT * FROM app_transaction;"
```

The script is **idempotent** — safe to run multiple times without creating duplicates.

---

## API Reference

**Base URL:** `http://localhost:8080/moneywise`

### Authentication

All endpoints **except `/auth/**`** and **`/actuator/**`** require a JWT token in the `Authorization` header:

```
Authorization: Bearer <your_jwt_token>
```

---

### 1. 🔐 Authentication

#### Register a New User

```
POST /moneywise/auth/createUser
```

**Headers:**
| Key          | Value            |
|--------------|------------------|
| Content-Type | application/json |

**Request Body:**
```json
{
  "userName": "john_doe",
  "password": "mypassword123",
  "email": "john@example.com"
}
```

**Success Response:** `201 Created`
```json
"User Register Successfull!!"
```

**Error Responses:**
| Status | Condition              |
|--------|------------------------|
| `409`  | Email already exists   |
| `400`  | Missing required field |

**cURL Example:**
```bash
curl -X POST http://localhost:8080/moneywise/auth/createUser \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "john_doe",
    "password": "mypassword123",
    "email": "john@example.com"
  }'
```

---

#### Login

```
POST /moneywise/auth/login
```

**Headers:**
| Key          | Value            |
|--------------|------------------|
| Content-Type | application/json |

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "mypassword123"
}
```

**Success Response:** `200 OK`
```json
{
  "userId": "1",
  "userName": "john_doe",
  "userEmail": "john@example.com",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Error Responses:**
| Status | Condition           |
|--------|---------------------|
| `400`  | Invalid credentials |
| `400`  | User not found      |

**cURL Example:**
```bash
curl -X POST http://localhost:8080/moneywise/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "mypassword123"
  }'
```

> 💡 **Save the `token` from the response — you'll need it for all other API calls.**

---

### 2. 📂 Categories

#### Get All Categories

```
GET /moneywise/app-category/get
```

**Headers:**
| Key           | Value              |
|---------------|--------------------|
| Authorization | Bearer `<token>`   |

**Success Response:** `200 OK`
```json
[
  {
    "id": 1,
    "categoryName": "Salary",
    "categoryTypeId": 1,
    "categoryIcon": "💰",
    "active": true
  },
  {
    "id": 6,
    "categoryName": "Food & Dining",
    "categoryTypeId": 2,
    "categoryIcon": "🍔",
    "active": true
  }
]
```

**cURL Example:**
```bash
curl -X GET http://localhost:8080/moneywise/app-category/get \
  -H "Authorization: Bearer <your_token>"
```

---

### 3. 💸 Transactions

#### Add / Edit a Transaction

```
POST /moneywise/transaction/addEdit
```

**Headers:**
| Key           | Value              |
|---------------|--------------------|
| Content-Type  | application/json   |
| Authorization | Bearer `<token>`   |

**Request Body (Add New Transaction):**
```json
{
  "userId": 1,
  "txnAmount": 500,
  "txnCategoryId": 6,
  "txnDate": "2026-06-28",
  "txnMessage": "Dinner at restaurant",
  "txnDateInt": 20260628,
  "isModify": 0,
  "modifyCount": 0
}
```

**Request Body (Edit Existing Transaction):**
```json
{
  "id": 5,
  "userId": 1,
  "txnAmount": 750,
  "txnCategoryId": 6,
  "txnDate": "2026-06-28",
  "txnMessage": "Updated - Dinner at restaurant",
  "txnDateInt": 20260628,
  "isModify": 1,
  "modifyCount": 1
}
```

**Field Descriptions:**
| Field            | Type    | Required | Description                                       |
|------------------|---------|----------|---------------------------------------------------|
| `id`             | Integer | No       | Include only when editing an existing transaction  |
| `userId`         | Integer | Yes      | ID of the user                                     |
| `txnAmount`      | Integer | Yes      | Transaction amount                                 |
| `txnCategoryId`  | Integer | Yes      | Category ID (from `/app-category/get`)             |
| `txnDate`        | String  | Yes      | Date in `YYYY-MM-DD` format                        |
| `txnMessage`     | String  | Yes      | Description of the transaction                     |
| `txnDateInt`     | Integer | Yes      | Date as integer `YYYYMMDD` (for sorting)           |
| `isModify`       | Integer | Yes      | `0` = new, `1` = edited                           |
| `modifyCount`    | Integer | Yes      | Number of times this transaction has been modified |

**Success Response:** `201 Created`

**cURL Example:**
```bash
curl -X POST http://localhost:8080/moneywise/transaction/addEdit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your_token>" \
  -d '{
    "userId": 1,
    "txnAmount": 500,
    "txnCategoryId": 6,
    "txnDate": "2026-06-28",
    "txnMessage": "Dinner at restaurant",
    "txnDateInt": 20260628,
    "isModify": 0,
    "modifyCount": 0
  }'
```

---

### 4. 🏠 Landing / Dashboard

#### Get Monthly Stats

```
GET /moneywise/landing/get
```

**Query Parameters:**
| Param       | Type    | Required | Example        | Description               |
|-------------|---------|----------|----------------|---------------------------|
| `userId`    | Integer | Yes      | `1`            | User ID                   |
| `startDate` | String  | Yes      | `"2026-06-01"` | Start date (`YYYY-MM-DD`) |
| `endDate`   | String  | Yes      | `"2026-06-30"` | End date (`YYYY-MM-DD`)   |

**Headers:**
| Key           | Value              |
|---------------|--------------------|
| Authorization | Bearer `<token>`   |

**Success Response:** `200 OK`

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/moneywise/landing/get?userId=1&startDate=2026-06-01&endDate=2026-06-30" \
  -H "Authorization: Bearer <your_token>"
```

---

### 5. 📜 Transaction History

#### Get Transaction History

```
GET /moneywise/transaction-history/get
```

**Query Parameters:**
| Param       | Type    | Required | Example      | Description                          |
|-------------|---------|----------|--------------|--------------------------------------|
| `userId`    | Integer | Yes      | `1`          | User ID                              |
| `startDate` | Integer | Yes      | `20260601`   | Start date as integer (`YYYYMMDD`)   |
| `endDate`   | Integer | Yes      | `20260630`   | End date as integer (`YYYYMMDD`)     |

**Headers:**
| Key           | Value              |
|---------------|--------------------|
| Authorization | Bearer `<token>`   |

**Success Response:** `200 OK`
```json
[
  {
    "id": 1,
    "userId": 1,
    "transactionAmount": 50000,
    "transactionCategoryId": 1,
    "transactionMessage": "June salary",
    "transactionDate": "2026-06-01",
    "transactionDateInt": 20260601,
    "isModify": 0,
    "transactionModificationCount": 0,
    "categoryType": "Income",
    "categoryName": "Salary",
    "categoryTypeName": "Income"
  }
]
```

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/moneywise/transaction-history/get?userId=1&startDate=20260601&endDate=20260630" \
  -H "Authorization: Bearer <your_token>"
```

---

### 6. ❤️ Health Check

```
GET /moneywise/actuator/health
```

> No authentication required.

**Success Response:** `200 OK`
```json
{
  "status": "UP"
}
```

**cURL Example:**
```bash
curl http://localhost:8080/moneywise/actuator/health
```

---

## Database Schema

### `app_category_type`

| Column              | Type         | Nullable | Description          |
|---------------------|--------------|----------|----------------------|
| `id`                | INTEGER (PK) | No       | Auto-generated ID    |
| `category__type_name` | VARCHAR(255) | No       | Type name            |

### `app_category`

| Column             | Type         | Nullable | Description              |
|--------------------|--------------|----------|--------------------------|
| `id`               | INTEGER (PK) | No       | Auto-generated ID        |
| `category_name`    | VARCHAR(255) | No       | Category name            |
| `category_type_id` | INTEGER      | No       | FK → `app_category_type` |
| `category_icon`    | VARCHAR(255) | No       | Emoji icon               |
| `is_active`        | BOOLEAN      | No       | Active status            |

### `app_users`

| Column      | Type         | Nullable | Description           |
|-------------|--------------|----------|-----------------------|
| `id`        | INTEGER (PK) | No       | Auto-generated ID     |
| `user_name` | VARCHAR(255) | No       | Username              |
| `password`  | VARCHAR(255) | No       | BCrypt hashed password|
| `email`     | VARCHAR(255) | No       | Email address         |

### `app_transaction`

| Column                           | Type         | Nullable | Description           |
|----------------------------------|--------------|----------|-----------------------|
| `id`                             | INTEGER (PK) | No       | Auto-generated ID     |
| `user_id`                        | INTEGER      | No       | FK → `app_users`      |
| `transaction_amount`             | INTEGER      | No       | Amount                |
| `transaction_category_id`        | INTEGER      | No       | FK → `app_category`   |
| `transaction_message`            | VARCHAR(255) | No       | Description           |
| `transaction_date`               | VARCHAR(255) | No       | Date (`YYYY-MM-DD`)   |
| `transaction_date_int`           | INTEGER      | No       | Date as `YYYYMMDD`    |
| `is_modifiy`                     | INTEGER      | No       | 0 = new, 1 = edited   |
| `transaction_modification_count` | INTEGER      | No       | Edit count            |

---

## Default Seed Data

The `seed_data.sh` script inserts the following data:

### Category Types
| ID | Name     |
|----|----------|
| 1  | Income   |
| 2  | Expense  |
| 3  | Transfer |

### Categories

| Icon | Name           | Type     |
|------|----------------|----------|
| 💰   | Salary         | Income   |
| 💻   | Freelance      | Income   |
| 📈   | Investments    | Income   |
| 🎁   | Gifts          | Income   |
| 💵   | Other Income   | Income   |
| 🍔   | Food & Dining  | Expense  |
| 🚗   | Transport      | Expense  |
| 🛍️   | Shopping       | Expense  |
| 🏠   | Rent           | Expense  |
| 💡   | Utilities      | Expense  |
| 🎬   | Entertainment  | Expense  |
| 🏥   | Healthcare     | Expense  |
| 📚   | Education      | Expense  |
| 🛒   | Groceries      | Expense  |
| 📱   | Subscriptions  | Expense  |
| ✈️   | Travel         | Expense  |
| 💇   | Personal Care  | Expense  |
| 🛡️   | Insurance      | Expense  |
| 💸   | Other Expense  | Expense  |
| 🏦   | Bank Transfer  | Transfer |
| 📲   | UPI Transfer   | Transfer |
| 🏧   | Cash Withdrawal| Transfer |
| 🔄   | Other Transfer | Transfer |

### Demo User
| Username    | Password   | Email                 |
|-------------|------------|-----------------------|
| `demo_user` | `demo1234` | `demo@moneywise.app`  |

> ⚠️ The demo user's password is **BCrypt hashed** in the database (inserted via the seed script as plain text, so use the `/auth/createUser` endpoint for proper hashing).

### Sample Transactions
14 sample transactions spanning June 2026, including income (salary, freelance), expenses (food, rent, utilities, etc.), and transfers.

---

## Troubleshooting

### Database connection refused
```
org.postgresql.util.PSQLException: Connection to localhost:5433 refused
```
**Fix:** Make sure the Docker container is running:
```bash
docker compose up -d
docker compose ps
```

### Tables don't exist when running seed script
```
ERROR: relation "app_category" does not exist
```
**Fix:** Start the Spring Boot app first so Hibernate creates the tables:
```bash
mvn spring-boot:run
```

### Port 5433 already in use
```bash
# Find what's using port 5433
lsof -i :5433

# Kill the process
kill -9 <PID>

# Or stop the Docker container using it
docker stop postgres-db && docker rm postgres-db
docker compose up -d
```

### Seed script permission denied
```bash
chmod +x seed_data.sh
./seed_data.sh
```

### JWT token expired / 401 Unauthorized
Login again to get a fresh token:
```bash
curl -X POST http://localhost:8080/moneywise/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "john@example.com", "password": "mypassword123"}'
```

---

## Project Structure

```
moneywise/
├── docker-compose.yml          # PostgreSQL Docker setup
├── seed_data.sh                # Database seeder script
├── pom.xml                     # Maven dependencies
└── src/main/java/com/moneywise/moneywise/
    ├── MoneywiseApplication.java
    ├── controller/
    │   ├── AppCategory.java           # GET  /app-category/get
    │   ├── HistoryController.java     # GET  /transaction-history/get
    │   ├── LandingController.java     # GET  /landing/get
    │   ├── LoginController.java       # POST /auth/login, /auth/createUser
    │   └── TransactionController.java # POST /transaction/addEdit
    ├── entity/
    │   ├── Category.java
    │   ├── CategoryType.java
    │   ├── Transaction.java
    │   └── User.java
    ├── model/
    │   ├── request/
    │   │   ├── AuthRequest.java
    │   │   ├── TransactionRequestDto.java
    │   │   └── UserDTO.java
    │   └── response/
    │       └── HistoryResponseDTO.java
    ├── security/
    │   ├── JWTAuthenticationFilter.java
    │   └── SecurityConfig.java
    ├── service/
    │   ├── AppCategoryService.java
    │   ├── HistoryService.java
    │   ├── LandingService.java
    │   ├── TransactionService.java
    │   └── UserService.java
    └── utils/
        └── JWTUtil.java
```
