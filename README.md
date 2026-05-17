# Products API

Small Spring Boot REST service for managing products. Prices are stored in EUR; the USD equivalent is fetched from the [HNB exchange-rate API](https://api.hnb.hr/) at creation time.

## Quick start

Requires JDK 17+, Maven 3.9+, Docker. Internet access is needed at runtime for the HNB call on create.

```bash
docker compose up -d --wait
mvn spring-boot:run
```

Listens on `http://localhost:8080`. Stop with Ctrl+C; `docker compose down -v` to wipe the DB.

## API

Base path: `/api/products`

| Method | Path                  | Description |
|--------|-----------------------|-------------|
| POST   | `/api/products`       | Create      |
| GET    | `/api/products/{id}`  | Get one     |
| GET    | `/api/products`       | List all    |

Create:

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"code":"ABC1234567","name":"Widget","price_eur":100.00,"is_available":true}'
```

Returns `201 Created` with `Location: /api/products/{id}` and the created product (request body plus `id` and `price_usd`).

Errors return a structured body:

```json
{
  "timestamp": "2026-05-16T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "violations": [{ "field": "code", "message": "code must be exactly 10 characters" }]
}
```

| Situation                              | Status |
|----------------------------------------|--------|
| Validation failure / malformed request | 400    |
| Product not found                      | 404    |
| Method not allowed                     | 405    |
| Duplicate `code`                       | 409    |
| Unsupported media type                 | 415    |
| HNB unreachable or invalid payload     | 503    |

## Configuration

Defaults match `docker-compose.yml`. To point at a different Postgres, set `DB_USERNAME` and `DB_PASSWORD`.

| Property                  | Default                                     |
|---------------------------|---------------------------------------------|
| `spring.datasource.url`   | `jdbc:postgresql://localhost:5432/ingemark` |
| `DB_USERNAME` (env)       | `postgres`                                  |
| `DB_PASSWORD` (env)       | `postgres`                                  |
| `hnb.api.base-url`        | `https://api.hnb.hr`                        |
| `hnb.api.connect-timeout` | `5s`                                        |
| `hnb.api.read-timeout`    | `10s`                                       |

## Tests

```bash
mvn test
```

Uses H2 in-memory plus a mocked HNB client. No Postgres or internet required.

## Design notes

- **USD price is computed at create time and stored.** HNB publishes once per day; recomputing on read would mean a cache or scheduled job, which isn't worth the cost at this scope. The stored value drifts after the next HNB update.
- **No pagination on `findAll`.** Out of scope at this size — `Pageable` + `Page<ProductResponse>` is the obvious extension.
- **HNB call sits inside the create transaction.** A slow HNB response holds a JDBC connection for the HTTP duration. Fine here, would move out of the transaction under real load.
