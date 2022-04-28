import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    `java-platform`

    id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

val DEPENDENCY_BOMS = listOf(
    "com.fasterxml.jackson:jackson-bom:2.13.2.20220328",
    "com.google.guava:guava-bom:31.0.1-jre",
    "com.linecorp.armeria:armeria-bom:1.14.0",
    "org.junit:junit-bom:5.8.2",
    "com.linecorp.armeria:armeria-bom:1.9.1",
    "io.grpc:grpc-bom:1.42.1",
    "io.opentelemetry:opentelemetry-bom:1.13.0",
    "io.opentelemetry:opentelemetry-bom-alpha:1.13.0-alpha",
    "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:1.14.0-alpha-SNAPSHOT",
    "org.testcontainers:testcontainers-bom:1.16.3"
)

val DEPENDENCY_SETS = listOf(
    DependencySet(
        "com.google.auto.service",
        "1.0.1",
        listOf("auto-service", "auto-service-annotations")
    ),
    DependencySet(
        "com.google.auto.value",
        "1.9",
        listOf("auto-value", "auto-value-annotations")
    ),
    DependencySet(
        "com.google.errorprone",
        "2.12.1",
        listOf("error_prone_annotations", "error_prone_core")
    ),
    DependencySet(
        "io.prometheus",
        "0.12.0",
        listOf("simpleclient", "simpleclient_common", "simpleclient_httpserver")
    ),
    DependencySet(
        "org.mockito",
        "4.3.1",
        listOf("mockito-core", "mockito-junit-jupiter")
    ),
    DependencySet(
        "org.slf4j",
        "1.7.36",
        listOf("slf4j-api", "slf4j-simple", "log4j-over-slf4j", "jcl-over-slf4j", "jul-to-slf4j")
    )
)

val DEPENDENCIES = listOf(
    "com.google.code.findbugs:annotations:3.0.1u2",
    "com.google.code.findbugs:jsr305:3.0.2",
    "com.squareup.okhttp3:okhttp:4.9.3",
    "com.uber.nullaway:nullaway:0.9.5",
    "org.assertj:assertj-core:3.22.0",
    "org.awaitility:awaitility:4.1.1",
    "org.junit-pioneer:junit-pioneer:1.5.0",
    "org.skyscreamer:jsonassert:1.5.0"
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
