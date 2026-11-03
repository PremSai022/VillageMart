# Testing

## Backend

Run the backend test suite with JDK 21:

```powershell
cd backend
.\mvnw.cmd test
```

Tests run against an isolated H2 database in MySQL mode and apply Flyway migrations. Coverage includes login/refresh,
product CRUD, purchases, stock transitions, concurrency/idempotency, sales/cancellation/returns, expenses, dashboard,
reports, PDF/Excel exports and invoice PDF generation. Testcontainers MySQL dependencies are present for Docker-enabled
CI; Docker was unavailable on the development workstation.

The report integration test validates the calculated case with opening quantity 100 at weighted cost 10, sale of 10
at 15, a saleable return of 2 and a 20 expense: net revenue 120, COGS 80, gross profit 40 and net profit 20.

## Android

```powershell
cd android
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:connectedDebugAndroidTest
```

Unit tests cover exact decimal JSON encoding/decoding and sale ViewModel stock validation, owner confirmation for price
overrides, and credit-customer validation. The Compose login screen test is compiled as an instrumentation test and
requires an emulator/device to execute. The debug APK build was verified locally.

## Acceptance workflows

Backend acceptance tests exercise the ten business scenarios in the master requirements: stock receipt/sale,
threshold transitions, insufficient-stock rejection, idempotency, cancellation, damaged returns and weighted-average
profit. Before production release also verify owner bootstrap, MySQL migrations, Android sign-in, purchase completion,
direct/customer sales, invoice sharing, push notification delivery and backup restoration in a staging environment.
