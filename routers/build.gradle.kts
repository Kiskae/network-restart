plugins {
    kotlin("jvm")
}

dependencies {
    implementation(platform(project(":platform")))

    api(kotlin("stdlib"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    api("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    api("org.jsoup:jsoup:1.12.1")

    implementation("com.squareup.retrofit2:retrofit")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("com.squareup.okhttp3:mockwebserver")
}