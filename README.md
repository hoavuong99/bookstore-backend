# Bookstore Backend

Spring Boot 3 backend for a decoupled Online Bookstore Management System.

## Standard Project Structure
```text
bookstore-backend/
├── pom.xml
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/com/example/bookstore/
│   │   │   ├── auth/          # auth feature (controller, service, dto)
│   │   │   ├── book/          # book feature (controller, service, dto)
│   │   │   ├── order/         # order feature (controller, service, dto)
│   │   │   ├── domain/        # entities, repositories, enums
│   │   │   ├── security/      # JWT filter/service, principal, user details
│   │   │   ├── config/        # security/jpa/properties config
│   │   │   └── common/        # ApiResponse, global exceptions
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
└── .github/
```

## Tech Stack
- Java 21
- Spring Boot 3 (Web, Data JPA, Security, Validation)
- MySQL 8
- JWT Bearer Authentication (stateless)

## Implemented API Modules
- Auth:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
- Orders:
  - `POST /api/v1/users/{userId}/orders/checkout` (JWT required)

All API responses use a common envelope:
- `ApiResponse<T>` with `success`, `message`, `data`, `errors`, `timestamp`.

## Quick Start
### 1) Prepare MySQL
Create database (or let JDBC URL auto-create):
- DB: `bookstore_db`
- User: `root`
- Password: `root`

### 2) Configure Environment Variables (optional)
Defaults are already in `src/main/resources/application.yml`.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (use a strong 32+ character secret)
- `CORS_ALLOWED_ORIGINS` (comma-separated, e.g. `http://localhost:5173`)

### 3) Run Backend
```bash
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

## Frontend Integration Notes
- Add `Authorization: Bearer <accessToken>` for protected endpoints.
- Default allowed origins include:
  - `http://localhost:5173`
  - `http://127.0.0.1:5173`
- Validation and business errors are returned in `ApiResponse.errors` with proper HTTP status.

## Example Auth Requests
### Register
```json
{
  "email": "alice@example.com",
  "password": "StrongPass123",
  "fullName": "Alice",
  "phone": "+84901234567",
  "address": "HCM City"
}
```

### Login
```json
{
  "email": "alice@example.com",
  "password": "StrongPass123"
}
```

## Example Checkout Request
`POST /api/v1/users/{userId}/orders/checkout`

```json
{
  "couponCode": "WELCOME10",
  "shippingAddress": "123 Street",
  "recipientName": "Alice",
  "recipientPhone": "+84901234567",
  "paymentMethod": "COD"
}
```

## Current Scope
This repository currently includes the core auth + checkout integration path. You can now plug in frontend login and checkout flows with JWT.
