/*
 * Desarrollado por RelajoSoft · https://relajosoft.com
 * Autor: Yoel Enrique Estevez Gonzalez
 * Configuración del módulo Android de la aplicación.
 */
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.relajosoft.floatingscanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.relajosoft.floatingscanner"
        minSdk = 26
        targetSdk = 35
        // Cada entrega aumenta versionCode para permitir actualizaciones en las PDA.
        versionCode = 12
        versionName = "0.8.0"
    }

    buildFeatures {
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX: compatibilidad, ciclo de vida y servicio en primer plano.
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")

    // CameraX: vista previa y análisis local de la cámara.
    val cameraX = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    // ML Kit se incluye en el APK y procesa los códigos localmente, sin Internet.
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}
