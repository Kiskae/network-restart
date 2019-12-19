plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdkVersion(29)

    defaultConfig {
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

    sourceSets.configureEach {
        java.srcDir("src/$name/kotlin")
    }
}

dependencies {
    implementation(platform(project(":platform")))
    implementation(kotlin("stdlib"))

    implementation(project(":context-lib"))

    implementation("androidx.appcompat:appcompat")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")

    debugImplementation("com.squareup.leakcanary:leakcanary-android")

    implementation("androidx.ui:ui-tooling")
    implementation("androidx.ui:ui-layout")
    implementation("androidx.ui:ui-material")
    implementation("androidx.ui:ui-text")
    implementation("androidx.ui:ui-foundation")
    implementation("androidx.ui:ui-framework")
}