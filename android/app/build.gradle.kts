import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Firebase is optional: the plugin is applied only when the (git-ignored) google-services.json is present.
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun config(name: String, envName: String, default: String): String =
    (localProps.getProperty(name) ?: System.getenv(envName) ?: providers.gradleProperty(name).orNull ?: default)

val debugApiUrl = config("villagemart.apiBaseUrl.debug", "VILLAGEMART_API_BASE_URL_DEBUG", "http://10.0.2.2:8080/")
val releaseApiUrl = config("villagemart.apiBaseUrl.release", "VILLAGEMART_API_BASE_URL", "")
val appVersionCode = config("villagemart.versionCode", "VILLAGEMART_VERSION_CODE", "1").toInt()
val appVersionName = config("villagemart.versionName", "VILLAGEMART_VERSION_NAME", "1.0.0")

// Release signing comes from keystore.properties (git-ignored) or CI environment variables. Never commit keys.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val releaseStoreFile = keystoreProps.getProperty("storeFile") ?: System.getenv("VILLAGEMART_KEYSTORE_FILE")

android {
    namespace = "com.mart.villagemart"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mart.villagemart"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "com.mart.villagemart.HiltTestRunner"
        buildConfigField("boolean", "FIREBASE_CONFIGURED", hasFirebaseConfig.toString())
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = keystoreProps.getProperty("storePassword") ?: System.getenv("VILLAGEMART_KEYSTORE_PASSWORD")
                keyAlias = keystoreProps.getProperty("keyAlias") ?: System.getenv("VILLAGEMART_KEY_ALIAS")
                keyPassword = keystoreProps.getProperty("keyPassword") ?: System.getenv("VILLAGEMART_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "DEFAULT_API_BASE_URL", "\"$debugApiUrl\"")
        }
        release {
            optimization {
                enable = true
            }
            buildConfigField("String", "DEFAULT_API_BASE_URL", "\"$releaseApiUrl\"")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
    }
}

val requestedTasks = gradle.startParameter.taskNames
val releaseBuildRequested = requestedTasks.any { taskName ->
    taskName.contains("release", ignoreCase = true) ||
        taskName.substringAfterLast(":").lowercase() in setOf("assemble", "bundle", "build")
}
if (releaseBuildRequested && (releaseApiUrl.isBlank() || !releaseApiUrl.startsWith("https://"))) {
    throw GradleException("Set VILLAGEMART_API_BASE_URL to the production HTTPS API URL before building a release.")
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
