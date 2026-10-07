plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val copyLauncherPictures = tasks.register<Copy>("copyLauncherPictures") {
    from(project.layout.projectDirectory) {
        include("SportsView_icon_picture.png")
        include("Calculator_icon_picture.png")
    }
    into(project.layout.projectDirectory.dir("app/src/main/res/drawable-nodpi"))
    rename("SportsView_icon_picture.png", "sportsview_launcher.png")
    rename("Calculator_icon_picture.png", "calculator_launcher.png")
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

    tasks.named("preBuild").configure { dependsOn(copyLauncherPictures) }
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
