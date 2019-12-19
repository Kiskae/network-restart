plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdkVersion(29)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(29)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xuse-experimental=kotlin.Experimental"
    }

    sourceSets.configureEach {
        java.srcDir("src/$name/kotlin")
    }
}

dependencies {
    implementation(platform(project(":platform")))
    api(kotlin("stdlib"))
    api("androidx.appcompat:appcompat")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    api("com.jakewharton.timber:timber")
}