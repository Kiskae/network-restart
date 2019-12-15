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
    }
}