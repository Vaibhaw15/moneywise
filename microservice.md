# MoneyWise Microservices API Documentation

This document contains all the available API endpoints for the MoneyWise microservices architecture. All requests go through the API Gateway (default port `8080`) and are routed to the underlying services.

### Global Requirements
- **Base URL:** `http://localhost:8080` (API Gateway)
- **Authentication:** All endpoints (except `/api/v1/auth/**`) require a valid JWT token in the `Authorization` header.
  - Header Format: `Authorization: Bearer <TOKEN>`

---

## 1. Auth Service (`auth-service`)

Handles user registration and authentication. Generates JWT tokens.

### 1.1 Register User
- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/createUser`
- **Authentication Required:** No
- **Request Body (JSON):**
  ```json
  {
    "userName": "johndoe",
    "password": "securepassword",
    "email": "johndoe@example.com"
  }
  ```
- **Response:** String message `"User Register Successfull!!"`

### 1.2 Login User
- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/login`
- **Authentication Required:** No
- **Request Body (JSON):**
  ```json
  {
    "email": "johndoe@example.com",
    "password": "securepassword",
    "userName": "johndoe"
  }
  ```
  *(Note: The current controller expects `email` and `password`. `userName` is included in the DTO but is not strictly verified for login).*
- **Response (JSON):** Returns the JWT token along with user details.
  ```json
  {
    "userName": "johndoe",
    "userId": "1",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userEmail": "johndoe@example.com"
  }
  ```

---

## 2. Category Service (`category-service`)

Manages the income/expense categories and their overarching types.

### 2.1 Get All Categories
- **Method:** `GET`
- **Endpoint:** `/api/v1/category/get`
- **Authentication Required:** Yes
- **Query Parameters:** None
- **Response (JSON):** List of categories.
  ```json
  [
    {
      "id": 1,
      "categoryName": "Salary",
      "categoryTypeId": 1,
      "categoryIcon": "money-bill",
      "active": true
    }
  ]
  ```

### 2.2 Get Category Types
- **Method:** `GET`
- **Endpoint:** `/api/v1/category/types`
- **Authentication Required:** Yes
- **Query Parameters:** None
- **Response (JSON):** List of types (Income, Expense, Transfer).
  ```json
  [
    {
      "id": 1,
      "categoryTypeName": "Income"
    },
    {
      "id": 2,
      "categoryTypeName": "Expense"
    }
  ]
  ```

---

## 3. Transaction Service (`transaction-service`)

Handles adding/editing transactions, transaction history logs, and landing page statistics.

### 3.1 Add / Edit Transaction
- **Method:** `POST`
- **Endpoint:** `/api/v1/transaction/addEdit`
- **Authentication Required:** Yes
- **Request Body (JSON):**
  ```json
  {
    "id": null,
    "userId": 1,
    "txnAmount": 500,
    "txnCategoryId": 1,
    "txnDate": "2026-06-28",
    "txnMessage": "Groceries",
    "txnDateInt": 20260628,
    "isModify": 0,
    "modifyCount": 0
  }
  ```
  *(Set `id` and `isModify: 1` when editing an existing transaction).*
- **Response:** String message confirming transaction addition/modification.

### 3.2 Get Transaction History
- **Method:** `GET`
- **Endpoint:** `/api/v1/transaction/history/get`
- **Authentication Required:** Yes
- **Query Parameters:**
  - `userId` (Integer): The ID of the user.
  - `startDate` (String): Start date in `yyyyMMdd` format (e.g. `20260601`).
  - `endDate` (String): End date in `yyyyMMdd` format (e.g. `20260630`).
- **Example URL:** `/api/v1/transaction/history/get?userId=1&startDate=20260601&endDate=20260630`
- **Response (JSON):** Grouped history list and statistics.

### 3.3 Get Landing Page Statistics
- **Method:** `GET`
- **Endpoint:** `/api/v1/transaction/landing/get`
- **Authentication Required:** Yes
- **Query Parameters:**
  - `userId` (Integer): The ID of the user.
  - `startDate` (String): Start date in `yyyyMMdd` format (e.g. `20260601`).
  - `endDate` (String): End date in `yyyyMMdd` format (e.g. `20260630`).
- **Example URL:** `/api/v1/transaction/landing/get?userId=1&startDate=20260601&endDate=20260630`
- **Response (JSON):** Statistical breakdown of income, expenses, and weekly chart data.
  ```json
  {
    "income": 5000,
    "expense": 200,
    "currentBalance": 4800,
    "compareBalanceFromLastMonth": 1000,
    "compareBalanceFromLastMonthPercentage": 20.0,
    "chartData": [
      {
        "week": "Week 1",
        "income": 5000,
        "expense": 0
      }
    ]
  }
  ```
