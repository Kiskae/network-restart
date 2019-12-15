import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.0.0-alpha06")
        classpath(kotlin("gradle-plugin", version = "1.3.61"))
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.27.0"
}

subprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }

    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}