plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdkVersion(29)

    defaultConfig {
        // 21 -> 24 because weird leakcanary icon issue?
        minSdkVersion(21)
        targetSdkVersion(29)
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(platform(project(":platform")))

    debugImplementation("com.squareup.leakcanary:leakcanary-android")
}