# Spring Boot E-Commerce Backend

Small e-commerce REST API built with Spring Boot, PostgreSQL, Gradle, and Docker Compose.

## What it includes

- Seeded users and product catalog
- One active cart per user
- Cart item add, update, and remove operations
- Checkout with stock validation
- Order history and order details
- Swagger UI

This is the first MVP. It does not include authentication, RBAC, Redis, Kafka, Sagas, AWS deployment, rate limiting, optimistic locking, or automated tests.

## Requirements

- Java 21
- Docker Desktop

The Gradle Wrapper downloads Gradle automatically when `./gradlew` is first run.

## Start PostgreSQL

```bash
cp .env.example .env
docker compose up -d postgres
docker compose ps
```

PostgreSQL is available at `localhost:5432`. On first startup, Docker runs `database/schema.sql` and `database/data.sql`.

## Start the application

```bash
./gradlew bootRun
```

The API runs on `http://localhost:8080`.

## Swagger UI

Open this URL in a browser:

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger UI shows every endpoint and lets you send requests from the browser. The OpenAPI document is available at `http://localhost:8080/v3/api-docs`.

## Demo commands

List products:

```bash
curl http://localhost:8080/api/products
```

Add product `1` to user `1`'s cart:

```bash
curl -X POST http://localhost:8080/api/users/1/cart/items \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'
```

View the cart:

```bash
curl http://localhost:8080/api/users/1/cart
```

Update the quantity:

```bash
curl -X PATCH http://localhost:8080/api/users/1/cart/items/1 \
  -H "Content-Type: application/json" \
  -d '{"quantity":3}'
```

Checkout:

```bash
curl -X POST http://localhost:8080/api/users/1/cart/checkout
```

View user orders:

```bash
curl http://localhost:8080/api/users/1/orders
```

Product `5` is out of stock. Add it to a new cart and checkout to see a `409 Conflict` response:

```bash
curl -X POST http://localhost:8080/api/users/1/cart/items \
  -H "Content-Type: application/json" \
  -d '{"productId":5,"quantity":1}'

curl -X POST http://localhost:8080/api/users/1/cart/checkout
```

## Resetting the database

Seed files run only when PostgreSQL creates a new data volume. To reload them, stop the stack and intentionally remove the Compose volume:

```bash
docker compose down -v
docker compose up -d postgres
```

## Next steps

1. Add optimistic locking and concurrent checkout checks.
2. Add JWT authentication and RBAC.
3. Add Redis caching and rate limiting.
4. Add Kafka event handling.
5. Add saga compensation.
6. Add CI and AWS deployment.
