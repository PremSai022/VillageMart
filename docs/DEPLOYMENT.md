# Deployment

## Local Docker Compose

Copy `.env.example` to `.env`, replace all sample values, and run from the repository root:

```powershell
docker compose up --build
```

Compose starts MySQL 8.4 with a persistent named volume and waits for its health check before launching the backend.
Flyway applies migrations automatically. Health endpoint: `http://localhost:8080/actuator/health`.
Swagger is disabled by default; enable only in controlled environments with `SWAGGER_ENABLED=true`.

Generate a strong JWT key (at least 32 random bytes) and provide strong database/owner passwords. The example values
are placeholders, not credentials to use in production. Keep `.env` out of source control.

## Railway backend

1. Provision a Railway MySQL service and a backend service connected to this repository.
2. Set the backend service root directory to `backend` (or use the repository root with Dockerfile path
   `backend/Dockerfile`); `backend/railway.json` configures the Dockerfile and `/actuator/health` check.
3. Configure secrets/environment variables in Railway:
   - Database: reference the Railway MySQL variables `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`,
     `MYSQLPASSWORD` (e.g. `MYSQLHOST=${{MySQL.MYSQLHOST}}`); the JDBC URL is built from them automatically.
     Alternatively set `DB_URL` (must start with `jdbc:mysql://`, never Railway's `mysql://` `MYSQL_URL`),
     `DB_USERNAME` and `DB_PASSWORD`. Never commit database credentials to `application.yml`.
   - `JWT_SECRET`: unique cryptographically random secret with at least 32 bytes.
   - Optional `OWNER_USERNAME`, `OWNER_PASSWORD`, `OWNER_MOBILE`, `OWNER_NAME` to pre-create the owner. If omitted,
     the owner creates the account from the app's **Create Account** screen; registration closes permanently
     (server-enforced) once one owner exists.
   - Password recovery: temporarily set `OWNER_RESET_PASSWORD` (8+ chars, a letter and a digit), redeploy, sign in,
     then remove the variable. All existing sessions are revoked when the reset is applied.
   - `BUSINESS_NAME`, `BUSINESS_ADDRESS`, `BUSINESS_MOBILE` as desired.
   - `CORS_ALLOWED_ORIGINS` only for trusted origins; the Android app does not require browser CORS.
   - Optional FCM `FIREBASE_CREDENTIALS_BASE64`; do not commit Firebase service-account JSON.
4. Set `SWAGGER_ENABLED=false` in production. Configure Railway health checks, resource sizing, and restart policy.
5. Deploy and confirm `https://villagemart-production.up.railway.app/actuator/health`, then sign in through the
   Android client.

The backend listens on Railway's injected `PORT`; Flyway executes before the application serves traffic. Use Railway
MySQL backups/restore points and test restoration regularly. Do not expose MySQL publicly when private networking is
available.

## Android release

The production API is `https://villagemart-production.up.railway.app/` and is the default for release (and debug)
builds. Override with `VILLAGEMART_API_BASE_URL` (HTTPS origin with a trailing slash) if needed; the release Gradle
task fails when the URL is not HTTPS. Supply signing credentials through ignored local properties or CI secrets. Verify
the release APK/AAB against the production API before publishing.

## Operational safety

- Rotate JWT, DB and Firebase credentials if exposed.
- Keep secrets out of logs, source control, build artifacts and issue reports.
- Monitor Flyway migration logs, health status, database capacity and scheduled push retries.
- Back up MySQL before schema migrations and verify backup recovery.
