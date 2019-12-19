import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    `java-platform`
}

//region spring dependency plugin inspired DSL.
fun DependencyHandlerScope.mavenBom(coordinates: String) = api(platform(coordinates))

fun DependencyConstraintHandler.dependency(coordinates: Any) = api(coordinates)

fun DependencyConstraintHandler.dependencySet(
        setId: String,
        entries: suspend SequenceScope<String>.() -> Unit
) = dependencySet(setId, sequence(entries))

suspend fun SequenceScope<String>.entry(coordinates: String) = yield(coordinates)

fun DependencyConstraintHandler.dependencySet(setId: String, entries: Sequence<String>) {
    val (groupId, version) = setId.split(':', limit = 2)
    entries.map {
        "$groupId:$it:$version"
    }.forEach { api(it) }
}
//endregion

javaPlatform.allowDependencies()

dependencies {
    mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.3.3")
    mavenBom("org.jetbrains.kotlin:kotlin-bom:${KotlinCompilerVersion.VERSION}")
    mavenBom("org.junit:junit-bom:5.5.2")
    mavenBom("net.serverpeon.androidx:androidx-bom:2019.12.04")

    constraints {
        // https://square.github.io/leakcanary/
        dependencySet("com.squareup.leakcanary:2.0") {
            entry("leakcanary-object-watcher")
            entry("leakcanary-object-watcher-android")
            entry("leakcanary-android")
            entry("leakcanary-android-instrumentation")
            entry("leakcanary-android-process")
        }

        // https://github.com/square/okhttp
        dependencySet("com.squareup.okhttp3:4.2.2") {
            entry("okhttp")
            entry("logging-interceptor")
            entry("mockwebserver")
        }

        // https://github.com/square/retrofit
        dependencySet("com.squareup.retrofit2:2.7.0") {
            entry("retrofit")
            entry("retrofit-mock")
            entry("retrofit-adapter-guava")
            entry("retrofit-adapter-java8")
            entry("retrofit-adapter-rxjava")
            entry("retrofit-adapter-rxjava2")
            entry("retrofit-adapter-scala")
            entry("retrofit-converter-gson")
            entry("retrofit-converter-guava")
            entry("retrofit-converter-jackson")
            entry("retrofit-converter-java8")
            entry("retrofit-converter-jaxb")
            entry("retrofit-converter-moshi")
            entry("retrofit-converter-protobuf")
            entry("retrofit-converter-scalars")
            entry("retrofit-converter-simplexml")
            entry("retrofit-converter-wire")
        }

        dependency("com.jakewharton.timber:timber:4.7.1")
    }
}