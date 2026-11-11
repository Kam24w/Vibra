import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

/**
 * Firma de release compartida por WanderAI (teléfono) y Vibra (reloj): la Data Layer
 * solo entrega mensajes entre apps con el mismo applicationId Y el mismo certificado.
 * Los secretos van en local.properties (no versionado) o en variables de entorno:
 * VIBRA_RELEASE_STORE_FILE, VIBRA_RELEASE_STORE_PASSWORD, VIBRA_RELEASE_KEY_ALIAS,
 * VIBRA_RELEASE_KEY_PASSWORD. Sin ellos, la build release queda sin firmar.
 */
fun releaseSecret(name: String): String? =
    (localProperties.getProperty(name) ?: System.getenv(name))?.takeIf { it.isNotBlank() }
val releaseStoreFile = releaseSecret("VIBRA_RELEASE_STORE_FILE")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vibra.navigation"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Debe coincidir con WanderAI (teléfono): la Data Layer solo entrega mensajes y
        // DataItems entre apps con el mismo applicationId y la misma firma.
        applicationId = "com.vibra.wanderai"
        minSdk = 30
        targetSdk = 37
        // Mismo applicationId que el teléfono: en Play cada APK necesita su propio
        // versionCode, así que el reloj usa la serie 1000+.
        versionCode = 1001
        versionName = "1.0"

    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseSecret("VIBRA_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSecret("VIBRA_RELEASE_KEY_ALIAS")
                keyPassword = releaseSecret("VIBRA_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (releaseStoreFile != null) signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.ui.tooling)
    implementation(libs.core.splashscreen)
    implementation(libs.wear.ongoing)
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout)
    implementation(libs.guava.android)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.wear.tooling.preview)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.ui.tooling)
}