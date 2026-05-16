# Products API

A small Spring Boot REST service that manages a list of products. Prices are stored in EUR and the USD equivalent is computed at creation time via the Croatian National Bank (HNB) public exchange-rate API.

## Tech stack

- Java 17
- Spring Boot 3.4 (Spring MVC, Spring Data JPA, Bean Validation)
- PostgreSQL + Flyway for schema migrations
- Maven
- JUnit 5 + Mockito + H2 (for tests)

## Project layout

```
src/main/java/com/ingemark/products
├── ProductsApplication.java        # entry point
├── config/                         # RestClient + @ConfigurationProperties
├── controller/ProductController    # REST endpoints
├── dto/                            # request/response records
├── entity/Product                  # JPA entity
├── exception/                      # custom exceptions + @RestControllerAdvice
├── mapper/ProductMapper            # entity <-> dto
├── repository/ProductRepository    # Spring Data JPA
└── service/                        # business logic + HNB integration
```

## Prerequisites

- JDK 17+ (`java -version`)
- Maven 3.9+ (`mvn -v`) — or use the bundled `./mvnw` if you generate a wrapper
- PostgreSQL 14+ running locally
- Internet access (the service calls `https://api.hnb.hr` on product creation)

## Local setup

### 1. Create the database

```sql
-- Connect as a superuser, e.g. via psql
CREATE DATABASE ingemark;
-- Optionally create a dedicated user
CREATE USER ingemark WITH PASSWORD 'ingemark';
GRANT ALL PRIVILEGES ON DATABASE ingemark TO ingemark;
```

The default configuration in `application.yml` connects with:

| Property            | Value                                       |
|---------------------|---------------------------------------------|
| URL                 | `jdbc:postgresql://localhost:5432/ingemark` |
| Username (default)  | `postgres`                                  |
| Password (default)  | `postgres`                                  |

Override via environment variables when needed:

```bash
export DB_USERNAME=ingemark
export DB_PASSWORD=ingemark
```

Flyway will create the `products` table on first startup (see `src/main/resources/db/migration/V1__create_products_table.sql`).

### 2. Build and run

```bash
mvn clean package
mvn spring-boot:run
```

The service listens on `http://localhost:8080`.

### 3. Run the tests

```bash
mvn test
```

Tests run against an in-memory H2 database (`application-test.yml`) and mock the HNB client, so no PostgreSQL or internet access is required for the test suite.

## API

Base path: `/api/products`

### Create a product

```http
POST /api/products
Content-Type: application/json

{
  "code": "ABC1234567",
  "name": "Widget",
  "price_eur": 100.00,
  "is_available": true
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "code": "ABC1234567",
  "name": "Widget",
  "price_eur": 100.00,
  "price_usd": 108.50,
  "is_available": true
}
```

The `price_usd` is computed from `price_eur * HNB middle rate (EUR → USD)` and rounded to 2 decimals (HALF_UP).

### Get one product

```http
GET /api/products/{id}
```

### List all products

```http
GET /api/products
```

## Error responses

All errors return a structured JSON body:

```json
{
  "timestamp": "2026-05-16T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "violations": [
    { "field": "code", "message": "code must be exactly 10 characters" }
  ]
}
```

| Situation                              | HTTP status |
|----------------------------------------|-------------|
| Validation failure                     | 400         |
| Product not found                      | 404         |
| Duplicate `code`                       | 409         |
| HNB API unreachable or invalid payload | 503         |
| Unhandled error                        | 500         |

## Configuration reference

| Key                       | Default                          | Notes                                          |
|---------------------------|----------------------------------|------------------------------------------------|
| `hnb.api.base-url`        | `https://api.hnb.hr`             | HNB API base URL                               |
| `hnb.api.currency`        | `USD`                            | Target currency for the EUR → X conversion     |
| `hnb.api.connect-timeout` | `5s`                             | HTTP connect timeout                           |
| `hnb.api.read-timeout`    | `10s`                            | HTTP read timeout                              |
| `server.port`             | `8080`                           |                                                |

## Extending

- **Update / delete endpoints** — add methods to `ProductController` and `ProductService`; the JPA repository already supports them.
- **Pagination** — change `findAll()` to accept `Pageable` and return `Page<ProductResponse>`.
- **Caching the FX rate** — annotate `HnbExchangeRateService#getEurRate` with `@Cacheable` and add `spring-boot-starter-cache`. HNB publishes rates once per day.
- **Additional currencies** — `ExchangeRateService` already takes a currency parameter; expose it on the request DTO or compute multiple prices in the service.
- **Different FX provider** — implement `ExchangeRateService` with a new bean and swap the implementation.
