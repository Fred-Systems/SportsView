plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fredsystems.sportsviewer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fredsystems.sportsviewer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        val ytKey = System.getenv("YOUTUBE_API_KEY") ?: ""
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$ytKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures { buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
