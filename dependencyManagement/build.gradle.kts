import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    `java-platform`

    id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

val DEPENDENCY_BOMS = listOf(
    "com.fasterxml.jackson:jackson-bom:2.12.3",
    "com.google.guava:guava-bom:30.1.1-jre",
    "org.junit:junit-bom:5.7.2",
    "com.linecorp.armeria:armeria-bom:1.9.1",
    "io.grpc:grpc-bom:1.39.0",
    "io.opentelemetry:opentelemetry-bom:1.4.1",
    "io.opentelemetry:opentelemetry-bom-alpha:1.4.1-alpha",
    "org.testcontainers:testcontainers-bom:1.16.0"
)

val DEPENDENCY_SETS = listOf(
    DependencySet(
        "com.google.auto.value",
        "1.8.1",
        listOf("auto-value", "auto-value-annotations")
    ),
    DependencySet(
        "io.prometheus",
        "0.11.0",
        listOf("simpleclient", "simpleclient_common", "simpleclient_httpserver")
    ),
    DependencySet(
        "org.slf4j",
        "1.7.30",
        listOf("slf4j-api", "slf4j-simple", "log4j-over-slf4j", "jcl-over-slf4j", "jul-to-slf4j")
    )
)

val DEPENDENCIES = listOf(
    "org.assertj:assertj-core:3.20.2",
    "org.awaitility:awaitility:4.1.0",
    "org.checkerframework:checker-qual:3.15.0",
    "org.skyscreamer:jsonassert:1.5.0",
    "org.spockframework:spock-core:1.3-groovy-2.5"
)

javaPlatform {
    allowDependencies()
}

dependencies {
    for (bom in DEPENDENCY_BOMS) {
        api(enforcedPlatform(bom))
        val split = bom.split(':')
        dependencyVersions[split[0]] = split[2]
    }
    constraints {
        for (set in DEPENDENCY_SETS) {
            for (module in set.modules) {
                api("${set.group}:${module}:${set.version}")
                dependencyVersions[set.group] = set.version
            }
        }
        for (dependency in DEPENDENCIES) {
            api(dependency)
            val split = dependency.split(':')
            dependencyVersions[split[0]] = split[2]
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isGuava = version.endsWith("-jre")
    val isStable = stableKeyword || regex.matches(version) || isGuava
    return isStable.not()
}

tasks {
    named<DependencyUpdatesTask>("dependencyUpdates") {
        revision = "release"
        checkConstraints = true

        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}
