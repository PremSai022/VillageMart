# VillageMart – Development Plan and Status

## Phase 0 – Repository audit — complete

- The repository initially contained only the Android Studio Compose template.
- Preserved it under `android/` and retained package/application ID `com.mart.villagemart`.
- No existing backend/database or usable Git history was present at audit time.
- Added requirements, architecture and phase plan; added the Spring Boot backend foundation and Flyway V1/V2.

## Phase 1 – Backend foundation — complete

- Java 21 / Spring Boot 3.5 / Maven wrapper; MySQL configuration uses environment variables and Flyway.
- BCrypt owner bootstrap, JWT access/refresh, rotation/revocation, protected APIs and consistent error response.
- Categories, units, products, validation, search and pagination.
- Auth and product integration tests pass.

## Phase 2 – Inventory and purchases — complete

- Supplier/purchase APIs, purchase completion/cancellation/returns, landed costs.
- Central inventory service with locking, movement history, weighted-average costing and low-stock transitions.
- Persisted notifications, optional FCM dispatch/retry.
- Acceptance, purchase, concurrency and alert tests pass.

## Phase 3 – Sales — complete

- Villages, customers, sales, payments/credit collection, idempotency, cancellation and controlled returns.
- Expenses and profit reporting support.
- Ten business acceptance scenarios and module integration tests pass.

## Phase 4 – Android foundation — implemented

- [x] Kotlin/Compose Material 3, Hilt/KSP, Retrofit/OkHttp, kotlinx.serialization, Room, DataStore, WorkManager,
  Navigation Compose, Coil and optional Firebase Messaging configured.
- [x] AES-GCM Android Keystore token storage, JWT interceptor and refresh authenticator.
- [x] Login, session restore, bottom navigation, premium green business theme, dashboard and error/loading states.
- [x] Android build and unit/instrumentation test compilation pass.

## Phase 5 – Android operations — core workflows implemented; remaining screens need completion

- [x] Product search/details/create/edit/activation, inventory status, stock adjustments, supplier/customer/village/
  expense/category creation, purchase intake, sales cart/checkout, cash/UPI/credit/split payment selection, dashboard,
  alerts, notifications, reports, PDF/Excel report export and invoice sharing.
- [x] Room-backed sale idempotency and WorkManager retry status; recently selected village preference.
- [x] Credit collection against customer balances and controlled sales returns from invoice item history.
- [ ] Complete supplier/customer/village/category edit flows, purchase returns, customer/supplier history, full stock
  movement detail UI, invoice lifecycle, settings editing and expanded report filters.
- [ ] Execute Compose instrumentation tests on emulator/device and end-to-end Android↔backend workflow.

## Phase 6 – Reports and integration — backend complete; Android integration in progress

- [x] Daily/monthly/product/category/village/purchase/stock/profit/expense/outstanding/payment reports with JSON/PDF/XLSX.
- [x] Backend dashboard and professional invoice PDF.
- [x] Report calculation, export content-type and invoice PDF integration tests.
- [x] Android report export/share and dashboard API integration.
- [ ] Exercise FCM from a configured Firebase project and verify live Android notification receipt.

## Phase 7 – QA and release — partially complete

- [x] 46 backend tests pass.
- [x] Android unit tests pass; Android instrumentation test compiles.
- [x] Android debug APK builds successfully.
- [x] Release variant builds with a reserved HTTPS test URL; release builds reject a missing/non-HTTPS API URL.
- [x] MySQL Compose setup, backend Dockerfile, Railway config, environment sample and operational docs added.
- [ ] Build/test against a live MySQL 8 instance (Docker not available in the current environment).
- [ ] Run instrumentation and full device workflow on a connected emulator.
- [ ] Build a signed release with the production HTTPS API URL and verify deployment.
- [ ] Initialize Git repository if this workspace is intended to become the source repository.
