# Spring Boot E-Commerce Backend

Backend service for a small e-commerce workflow: products, carts, checkout, and order history.

## Stack

- Java 21 and Spring Boot
- PostgreSQL 16
- Spring Data JPA
- Gradle
- Docker Compose
- Springdoc OpenAPI

## Features

- Product catalog with seeded electronics and clothing items
- One active cart per user
- Add, update, and remove cart items
- Transactional checkout with stock validation
- Order history and order details
- Consistent JSON error responses
- Swagger UI for API exploration

## API overview

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/products` | List products |
| GET | `/api/products/{productId}` | Get a product |
| GET | `/api/users/{userId}/cart` | Get active cart |
| POST | `/api/users/{userId}/cart/items` | Add a cart item |
| PATCH | `/api/users/{userId}/cart/items/{productId}` | Change item quantity |
| DELETE | `/api/users/{userId}/cart/items/{productId}` | Remove a cart item |
| POST | `/api/users/{userId}/cart/checkout` | Create an order |
| GET | `/api/users/{userId}/orders` | List user orders |
| GET | `/api/orders/{orderId}` | Get order details |

## Local run

Requirements: Java 21 and Docker.

```bash
cp .env.example .env
docker compose up -d postgres
docker compose ps
chmod +x gradlew
./gradlew bootRun
```

PostgreSQL listens on `localhost:5432`. The API listens on `http://localhost:8080`.

Database tables and seed records are defined in `database/schema.sql` and `database/data.sql`. Docker loads them when the PostgreSQL volume is created.

## API documentation

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Example flow

```bash
curl http://localhost:8080/api/products

curl -X POST http://localhost:8080/api/users/1/cart/items \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

curl http://localhost:8080/api/users/1/cart

curl -X PATCH http://localhost:8080/api/users/1/cart/items/1 \
  -H "Content-Type: application/json" \
  -d '{"quantity":3}'

curl -X POST http://localhost:8080/api/users/1/cart/checkout

curl http://localhost:8080/api/users/1/orders
```

Product `5` has zero stock and can be used to check the `409 Conflict` checkout response.

```bash
curl -X POST http://localhost:8080/api/users/1/cart/items \
  -H "Content-Type: application/json" \
  -d '{"productId":5,"quantity":1}'

curl -X POST http://localhost:8080/api/users/1/cart/checkout
```

## Reset local data

```bash
docker compose down -v
docker compose up -d postgres
```

## Scope

The current version focuses on the checkout workflow and basic stock checks. Authentication, RBAC, optimistic locking, Redis, Kafka, saga handling, CI, and AWS deployment are planned extensions.
