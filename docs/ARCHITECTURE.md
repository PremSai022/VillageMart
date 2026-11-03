# VillageMart – Architecture

## 1. System overview

```
┌────────────────────┐   HTTPS / JSON (JWT)    ┌──────────────────────────┐   JDBC   ┌──────────┐
│ Android app        │ ──────────────────────▶ │ Spring Boot API (/api/v1) │ ───────▶ │ MySQL 8  │
│ Compose + MVVM     │ ◀────────────────────── │ Java 21                   │          │ Flyway   │
└────────┬───────────┘        FCM push         └────────────┬─────────────┘          └──────────┘
         │ Room (drafts), DataStore, WorkManager             │ Firebase Admin SDK (optional)
```

## 2. Backend

### 2.1 Package layout (`com.villagemart`)
| Package | Responsibility |
|---|---|
| `common` | `BaseEntity`, `ApiError`, exceptions, `GlobalExceptionHandler`, `PageResponse`, money helpers, document number sequences, idempotency records |
| `config` | App properties, clock (Asia/Kolkata), OpenAPI, CORS, async/scheduling |
| `security` | `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `CurrentUser` |
| `auth` | users, refresh tokens, login/refresh/logout, owner bootstrap |
| `settings` | business settings (name, address, GST, invoice footer) |
| `category`, `unit`, `product` | master data |
| `supplier`, `purchase` | wholesale purchases, purchase returns |
| `inventory` | **single entry point for stock changes**, movements, weighted average cost, stock status |
| `customer`, `village` | customers, villages |
| `sales` | billing, invoices, cancellation, sales returns |
| `payment` | payment records, credit collection |
| `expense` | expense categories and expenses |
| `notification` | persisted alerts, device tokens, FCM dispatch + retry |
| `report`, `dashboard` | aggregate queries, PDF/Excel export, invoice PDF |

Feature packages separate entities, repositories, DTOs (Java records), services and controllers. MapStruct is
available for mappings where useful; simple immutable DTO projections are mapped explicitly. Controllers never
return JPA entities.

### 2.2 Transaction & concurrency model
- Service methods that change stock are `@Transactional`.
- `InventoryService` locks inventory rows in ascending product-id order using `PESSIMISTIC_WRITE`
  (MySQL `SELECT … FOR UPDATE`) – deterministic ordering prevents deadlocks between concurrent sales.
- Every quantity change computes previous/new quantity, rejects negatives, recomputes the weighted average cost and
  stock status, and writes a `stock_movements` row.
- Stock-status transitions are handled by `StockAlertService` in the same transaction (persisted notification);
  the FCM push happens **after commit** (`@TransactionalEventListener`) and failed pushes are retried by a scheduler.
- Sale creation: an `idempotency_records` row (unique key + SHA-256 request hash) is inserted first; repeating a key
  with the same payload returns the stored sale, a different payload returns `409 IDEMPOTENCY_CONFLICT`.
- Document numbers (`INV-…`, `PUR-…`, `SR-…`, `PR-…`) come from a locked `document_sequences` row.

### 2.3 Costing
- Purchase landed unit cost = (line total + allocated transport/other charges) ÷ quantity.
- Weighted average is updated on stock inflows (PURCHASE, OPENING_STOCK, positive MANUAL_ADJUSTMENT,
  SALE_RETURN restock, sale CANCELLATION_REVERSAL) and reversed on purchase cancellation/returns.
- Sale item stores `unit_cost` = weighted average at sale time; COGS = Σ qty × unit_cost.

### 2.4 Security
- Stateless Spring Security; `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/actuator/health` are public;
  Swagger UI only when `SWAGGER_ENABLED=true`.
- Access token: HS256 JWT signed with `JWT_SECRET` (≥ 32 bytes), default 30 minutes.
- Refresh token: 256-bit random, SHA-256 hashed in DB, 30 days, rotated on each refresh, revoked on logout.
- All secrets from environment variables; nothing sensitive in the repository.

### 2.5 Error contract
```json
{ "timestamp": "2026-11-03T10:15:30Z", "status": 422, "code": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for Sunflower Oil 1L: available 20, requested 25",
  "path": "/api/v1/sales", "fieldErrors": [] }
```

## 3. Android

### 3.1 Layers (module `app`, package `com.mart.villagemart`)
| Layer | Contents |
|---|---|
| `data` | Serializable API DTOs, Retrofit endpoints, OkHttp auth/refresh, encrypted token storage, preferences and repositories |
| `data` | Room database and DAO for durable sale drafts |
| `ui` | Compose navigation, reusable components, screens and Hilt `ViewModel`s using `StateFlow` |
| `worker` | `PendingSaleWorker` – retries pending sales with the same idempotency key |
| `notification` | Firebase messaging service, notification channels and device-token registration |

### 3.2 Data flow
`Composable → ViewModel (StateFlow) → Repository → Retrofit → Backend`.
Money/quantities use a Kotlin `BigDecimal` wrapper and a custom kotlinx-serialization adapter; no floating-point
conversion is used for transaction amounts.

The Android `TokenStore` encrypts access and refresh tokens with an AES-GCM key held by Android Keystore.
DataStore stores the last selected village separately. Room persists sale JSON and its idempotency key; confirmed,
failed and pending sync states remain visible in the sales workflow.

### 3.3 Sales submission flow
1. Checkout builds the request + UUID idempotency key and persists it as a `PendingSale` (Room).
2. `POST /sales` with `Idempotency-Key`. The owner sees a confirmed invoice only after the backend response.
3. On network failure → draft stays `PENDING`, WorkManager retries with the same key; UI shows “Pending sync”.
4. On 4xx validation error → draft marked `FAILED` with the server message.

## 4. Extensibility
- `stock_locations` + `inventory.location_id` allow multiple vehicles/warehouses.
- `users.role` allows additional users/roles later.
- Reports are query-based services; new reports plug into `ReportService` + exporters.
