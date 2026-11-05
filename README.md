# VillageMart

VillageMart is an Android inventory and mobile retail management system for a village-travelling grocery and
household-goods business. It provides owner authentication, product/catalog management, wholesale purchases,
trolley inventory, low-stock alerts, point-of-sale billing, customers, villages, expenses, reports, and invoice PDFs.

## Technology

- **Backend:** Java 21, Spring Boot 3.5, Spring Security/JWT, Spring Data JPA, MySQL 8, Flyway, OpenAPI.
- **Android:** Kotlin, Jetpack Compose Material 3, MVVM/StateFlow, Hilt, Retrofit/OkHttp, kotlinx.serialization,
  Room, DataStore, WorkManager, Coil and optional Firebase Cloud Messaging.
- **Reports:** PDF and Excel exports; weighted-average cost for inventory valuation and profit.

## Repository

```text
backend/       Spring Boot API and Flyway migrations
android/       Android Studio project
docs/          Requirements, architecture, API, database, setup and deployment
docker-compose.yml
.env.example
```

## Run locally

1. Copy `.env.example` to `.env`; replace the example database/JWT/owner passwords with strong local-only values.
2. Start the local MySQL + backend stack from the repository root:

   ```powershell
   docker compose up --build
   ```

3. Open `android/` in Android Studio and run the `app` configuration. It uses the production API
   `https://villagemart-production.up.railway.app/` by default; set `VILLAGEMART_API_BASE_URL_DEBUG` (e.g.
   `http://10.0.2.2:8080/`) to use a local backend.
4. Sign in with the initial owner account configured in `.env`.

Backend manual build/test:

```powershell
cd backend
.\mvnw.cmd test
```

Android validation:

```powershell
cd android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The debug APK (`android/app/build/outputs/apk/debug/app-debug.apk`) installs as `com.mart.villagemart.debug` with the
launcher label **VillageMart (Debug)**. Uninstall any older `com.mart.villagemart` template build
(`adb uninstall com.mart.villagemart`) so you don't launch a stale "Hello Android!" app. If an old build shows template
code, delete `android/app/build` and rebuild. The app starts on a branded splash screen, then shows Owner login, then the Dashboard.

See [deployment](docs/DEPLOYMENT.md), [Android setup](docs/ANDROID_SETUP.md), [API reference](docs/API_DOCUMENTATION.md),
[database design](docs/DATABASE.md), [architecture](docs/ARCHITECTURE.md), [requirements](docs/REQUIREMENTS.md), and
[testing](docs/TESTING.md) for details.

## Security

Never commit `.env`, production credentials, Firebase service-account files, `google-services.json`, signing keys or
signing passwords. Production release builds require an HTTPS API base URL. Configure owner bootstrap credentials
securely before deploying; public registration is not available.
