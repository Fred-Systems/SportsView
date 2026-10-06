import java.net.URL

val downloadLauncherPictures = tasks.register("downloadLauncherPictures") {
    doLast {
        val outDir = file("src/main/res/drawable-nodpi")
        outDir.mkdirs()
        val files = mapOf(
            "sportsview_launcher.png" to "https://raw.githubusercontent.com/Fred-Systems/SportsViewer/main/SportsView_icon_picture.png",
            "calculator_launcher.png" to "https://raw.githubusercontent.com/Fred-Systems/SportsViewer/main/Calculator_icon_picture.png"
        )
        files.forEach { (name, url) ->
            URL(url).openStream().use { input ->
                file(outDir.resolve(name)).outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

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
        versionCode = 10
        versionName = "1.8.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("SPORTSVIEW_KEYSTORE_PASSWORD") ?: error("SPORTSVIEW_KEYSTORE_PASSWORD is missing")
            keyAlias = "sportsview"
            keyPassword = System.getenv("SPORTSVIEW_KEY_PASSWORD") ?: error("SPORTSVIEW_KEY_PASSWORD is missing")
        }
    }

    buildFeatures { buildConfig = true }

    tasks.named("preBuild").configure { dependsOn(downloadLauncherPictures) }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
