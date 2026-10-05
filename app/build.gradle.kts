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
    buildFeatures { buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
