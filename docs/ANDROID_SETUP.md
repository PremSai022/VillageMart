# Android setup

## Prerequisites

- Android Studio with Android SDK Platform 37 and Android Build Tools compatible with AGP 9.3.3.
- JDK 17 or newer. The configured Gradle daemon toolchain may use JDK 25; JDK 21 was used for local validation.
- Android emulator or device (API 24+).

## Local debug build

1. Open the repository's `android/` directory in Android Studio or use its Gradle wrapper.
2. Set `android/local.properties` to your SDK path (Android Studio manages this local-only file).
3. Configure the debug API URL through `VILLAGEMART_API_BASE_URL_DEBUG` or a local Gradle property. The emulator
   default is `http://10.0.2.2:8080/`; a physical device needs the reachable host address. The client appends
   `/api/v1/` automatically, so use the API origin (or a base URL already ending in `/api/v1`).
4. Start the backend and MySQL using the instructions in `DEPLOYMENT.md`.
5. Build and run:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Debug builds permit cleartext traffic only through the debug manifest overlay. Release builds reject cleartext.

## Authentication, storage and notifications

The app signs in with the owner username/mobile and password configured on the backend. Access/refresh tokens are
AES-GCM encrypted with an Android Keystore key; DataStore remembers the last village. Room persists idempotent sale
drafts for WorkManager retry. No customer/product fixtures are used in app functionality.

FCM is optional for local use. To enable it, place the Firebase Android configuration at
`android/app/google-services.json` (ignored by Git) and configure the corresponding Firebase Admin service-account
secret on the backend. Without Firebase, dashboard and persisted backend alerts continue to work.

## Release builds

Set `VILLAGEMART_API_BASE_URL` to the production HTTPS API URL before `:app:assembleRelease`. Release signing can be
configured in ignored `android/keystore.properties` or through `VILLAGEMART_KEYSTORE_FILE`,
`VILLAGEMART_KEYSTORE_PASSWORD`, `VILLAGEMART_KEY_ALIAS`, and `VILLAGEMART_KEY_PASSWORD` CI variables. Never check in
signing files or passwords.

The Compose login test is under `app/src/androidTest`; run it on a connected emulator/device using
`.\gradlew.bat :app:connectedDebugAndroidTest`.
