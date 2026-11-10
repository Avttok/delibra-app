import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/** Lê a configuração: variável de ambiente (CI) > gradle.properties. */
fun pwexConfig(property: String, env: String): String =
    System.getenv(env)?.takeIf { it.isNotBlank() }?.trim()
        ?: providers.gradleProperty(property).orNull?.trim()
        ?: ""

android {
    namespace = "com.pwexpress.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "SUPABASE_URL", "\"${pwexConfig("pwex.supabaseUrl", "PWEX_SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${pwexConfig("pwex.supabaseKey", "PWEX_SUPABASE_KEY")}\"")
        buildConfigField("String", "SITE_URL", "\"${pwexConfig("pwex.siteUrl", "PWEX_SITE_URL")}\"")
        buildConfigField("String", "GITHUB_REPO", "\"${pwexConfig("pwex.githubRepo", "PWEX_GITHUB_REPO")}\"")
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
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.core.ktx)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    api(libs.maplibre.android)
    api(libs.coil.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
