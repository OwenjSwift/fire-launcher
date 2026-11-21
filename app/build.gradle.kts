plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// GitHub Actions passes the run number so every new build counts as an update.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.swifty.glasshome"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.swifty.glasshome"
        minSdk = 25
        targetSdk = 30
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    signingConfigs {
        // A fixed key, kept in the repo, so each new APK installs over the old one.
        // This app is only for sideloading onto your own Fire TV, so that's fine.
        create("glass") {
            storeFile = file("glasshome.keystore")
            storePassword = "glasshome"
            keyAlias = "glasshome"
            keyPassword = "glasshome"
        }
    }

    buildTypes {
        release {
            // Shrinks and optimises the code. Makes Compose apps noticeably smoother.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("glass")
        }
        debug {
            signingConfig = signingConfigs.getByName("glass")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint { checkReleaseBuilds = false }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}
