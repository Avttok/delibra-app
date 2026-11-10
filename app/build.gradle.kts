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

    // Assinatura digital de produção (Delibra). A chave NÃO fica no repositório: o GitHub Actions
    // monta o arquivo a partir dos secrets DELIBRA_KEYSTORE_B64 / DELIBRA_KEYSTORE_SENHA / DELIBRA_KEY_ALIAS
    // e passa o caminho em DELIBRA_KEYSTORE. Sem ela (build local), o release usa a chave de desenvolvimento.
    val chaveProducao = System.getenv("DELIBRA_KEYSTORE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        // Chave fixa de desenvolvimento (Android Studio e testes locais).
        getByName("debug") {
            storeFile = rootProject.file("keystore/pwex-debug.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (chaveProducao != null) create("producao") {
            storeFile = chaveProducao
            storePassword = System.getenv("DELIBRA_KEYSTORE_SENHA")
            keyAlias = System.getenv("DELIBRA_KEY_ALIAS") ?: "delibra"
            keyPassword = System.getenv("DELIBRA_KEY_SENHA") ?: System.getenv("DELIBRA_KEYSTORE_SENHA")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = false
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (chaveProducao != null) signingConfigs.getByName("producao") else signingConfigs.getByName("debug")
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
