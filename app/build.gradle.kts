import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.knigdelioglu.seyir"
    compileSdk = 37

    val localProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }
    val footballApiKey: String = (
        localProperties.getProperty("FOOTBALL_API_KEY")
            ?: localProperties.getProperty("API_FOOTBALL_KEY")
            ?: localProperties.getProperty("API_SPORTS_KEY")
            ?: System.getenv("FOOTBALL_API_KEY")
            ?: System.getenv("API_FOOTBALL_KEY")
            ?: ""
    ).trim()

    defaultConfig {
        applicationId = "io.github.knigdelioglu.seyir"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-dev"

        buildConfigField("String", "FOOTBALL_API_KEY", "\"$footballApiKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")

    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.tv:tv-material:1.1.0")

    testImplementation("junit:junit:4.13.2")
    // Android's org.json classes are framework stubs in local JVM tests. Keep
    // the real parser test-only so production has no additional dependency.
    testImplementation("org.json:json:20260814")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
