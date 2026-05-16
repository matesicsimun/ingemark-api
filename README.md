# Products API

A small Spring Boot REST service that manages a list of products. Prices are stored in EUR and the USD equivalent is fetched at creation time from the [Croatian National Bank (HNB) public exchange-rate API](https://api.hnb.hr/).

## Tech stack

- Java 17
- Spring Boot 3.4 (Spring MVC, Spring Data JPA, Bean Validation)
- PostgreSQL 16 + Flyway migrations
- Maven
- JUnit 5 + Mockito + H2 (tests only)

## Quick start

**Prerequisites:** JDK 17+, Maven 3.9+, Docker (for the database). Internet access is required at runtime since product creation calls the HNB API.

```bash
# 1. Start PostgreSQL in the background
docker compose up -d

# 2. Wait for it to become healthy (a few seconds)
docker compose ps

# 3. Run the application
mvn spring-boot:run
```

The service listens on **http://localhost:8080**. Flyway creates the `products` table automatically on first startup, so the schema is ready as soon as the app comes up.

To stop everything:

```bash
# Stop the app: Ctrl+C in the terminal running mvn

# Stop Postgres (keeps data):
docker compose down

# Stop Postgres AND delete the data volume:
docker compose down -v
```

## Try it out

Once the app is running, exercise it with curl. The examples below show the full create → get → list flow.

### 1. Create a product

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ABC1234567",
    "name": "Widget",
    "price_eur": 100.00,
    "is_available": true
  }'
```

Expected response — `201 Created`, with `Location: /api/products/1`:

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

The `price_usd` value comes from `price_eur × HNB middle rate (EUR → USD)`, rounded to USD's two fraction digits.

### 2. Get one product

```bash
curl http://localhost:8080/api/products/1
```

### 3. List all products

```bash
curl http://localhost:8080/api/products
```

### 4. See validation in action

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"code":"SHORT","name":"X","price_eur":-5,"is_available":true}'
```

Expected response — `400 Bad Request` with a `violations` array describing each invalid field.

### 5. See conflict handling

Send the first create request twice — the second one returns `409 Conflict` because `code` is unique.

## Running the tests

```bash
mvn test
```

Tests run against an in-memory H2 database (`src/test/resources/application-test.yml`) and mock the HNB client, so **no Postgres or internet access is required for the test suite**.

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

## API reference

Base path: `/api/products`

| Method | Path                | Description                  | Success         | Failure modes              |
|--------|---------------------|------------------------------|-----------------|----------------------------|
| POST   | `/api/products`     | Create a product             | `201 Created`   | 400, 409, 503              |
| GET    | `/api/products/{id}`| Fetch a product by id        | `200 OK`        | 404                        |
| GET    | `/api/products`     | List all products            | `200 OK`        | —                          |

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

Defaults match `docker-compose.yml`, so the out-of-the-box setup works without any overrides. To point at a different Postgres:

```bash
export DB_USERNAME=myuser
export DB_PASSWORD=mypassword
# then start the app
```

| Property                  | Default                                     | Notes                                      |
|---------------------------|---------------------------------------------|--------------------------------------------|
| `spring.datasource.url`   | `jdbc:postgresql://localhost:5432/ingemark` | Override in `application.yml` if needed    |
| `DB_USERNAME` (env)       | `postgres`                                  | Mapped to `spring.datasource.username`     |
| `DB_PASSWORD` (env)       | `postgres`                                  | Mapped to `spring.datasource.password`     |
| `hnb.api.base-url`        | `https://api.hnb.hr`                        | HNB API base URL                           |
| `hnb.api.connect-timeout` | `5s`                                        | HTTP connect timeout to HNB                |
| `hnb.api.read-timeout`    | `10s`                                       | HTTP read timeout to HNB                   |
| `server.port`             | `8080`                                      |                                            |

## Extending

- **Update / delete endpoints** — add methods to `ProductController` and `ProductService`; the JPA repository already supports them.
- **Pagination** — change `findAll()` to accept `Pageable` and return `Page<ProductResponse>`.
- **Caching the FX rate** — annotate `HnbExchangeRateService#convertFromEur` with `@Cacheable` and add `spring-boot-starter-cache`. HNB publishes rates once per day.
- **Additional currencies** — `ExchangeRateService#convertFromEur(BigDecimal, Currency)` already takes a target currency; expose it on the request DTO or compute multiple prices in the service.
- **Different FX provider** — implement `ExchangeRateService` with a new bean and swap the implementation.

## Troubleshooting

| Symptom                                                          | Likely cause                                       | Fix                                                       |
|------------------------------------------------------------------|----------------------------------------------------|-----------------------------------------------------------|
| App fails to start with `Connection refused: localhost:5432`     | Postgres container isn't running yet               | `docker compose up -d` and wait for `healthy`             |
| App fails with `password authentication failed`                  | Stale data volume from a previous setup            | `docker compose down -v` then `docker compose up -d`      |
| `503 Service Unavailable` on POST                                 | HNB API unreachable                                | Check internet connectivity; HNB occasionally rate-limits |
| Flyway migration error on startup                                 | Schema drift vs. what Flyway expects               | `docker compose down -v` to reset, then start again       |
