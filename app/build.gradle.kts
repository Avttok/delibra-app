import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services) apply false
}

// Firebase (avisos push): só liga se o google-services.json já tiver o app com.delibra.app.
// Sem ele o app funciona igual, só sem push (o servidor avisa pelo app aberto).
val temFirebase = file("google-services.json").takeIf { it.exists() }?.readText()?.contains("\"com.delibra.app\"") == true
if (temFirebase) apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.pwexpress.motoboy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.delibra.app"
        minSdk = 26
        targetSdk = 36
        // No GitHub Actions cada build ganha um número maior, assim o APK novo instala por cima do antigo.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "0.9.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
        // aparelhos reais (ARM) + emulador (x86_64): APK menor sem as outras arquiteturas do mapa
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

    signingConfigs {
        // Chave fixa de desenvolvimento (só para o piloto via sideload).
        // Garante que o APK do Android Studio e o do GitHub tenham a mesma assinatura.
        getByName("debug") {
            storeFile = rootProject.file("keystore/pwex-debug.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug") // trocar por chave de produção antes da Play Store
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:tracking"))
    implementation(project(":feature:corridas"))
    implementation(project(":feature:perfil"))
    implementation(project(":feature:gestor"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
