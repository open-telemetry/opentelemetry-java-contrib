import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  `java-platform`

  id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

// this line is managed by .github/scripts/update-sdk-version.sh
val otelVersion = "1.18.0"

val DEPENDENCY_BOMS = listOf(
  "com.fasterxml.jackson:jackson-bom:2.13.4",
  "com.google.guava:guava-bom:31.1-jre",
  "com.linecorp.armeria:armeria-bom:1.20.0",
  "org.junit:junit-bom:5.9.1",
  "io.grpc:grpc-bom:1.49.2",
  "io.opentelemetry:opentelemetry-bom:$otelVersion",
  "io.opentelemetry:opentelemetry-bom-alpha:${otelVersion}-alpha",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${otelVersion}-alpha",
  "org.testcontainers:testcontainers-bom:1.17.5"
)

val CORE_DEPENDENCIES = listOf(
  "com.google.auto.service:auto-service:1.0.1",
  "com.google.auto.service:auto-service-annotations:1.0.1",
  "com.google.auto.value:auto-value:1.9",
  "com.google.auto.value:auto-value-annotations:1.10",
  "com.google.errorprone:error_prone_annotations:2.15.0",
  "com.google.errorprone:error_prone_core:2.15.0",
  "io.prometheus:simpleclient:0.16.0",
  "io.prometheus:simpleclient_common:0.16.0",
  "io.prometheus:simpleclient_httpserver:0.16.0",
  "org.mockito:mockito-core:4.8.0",
  "org.mockito:mockito-junit-jupiter:4.8.0",
  "org.slf4j:slf4j-api:2.0.3",
  "org.slf4j:slf4j-simple:2.0.3",
  "org.slf4j:log4j-over-slf4j:2.0.3",
  "org.slf4j:jcl-over-slf4j:2.0.3",
  "org.slf4j:jul-to-slf4j:2.0.3"
)

val DEPENDENCIES = listOf(
  "io.opentelemetry.javaagent:opentelemetry-javaagent:$otelVersion",
  "com.google.code.findbugs:annotations:3.0.1u2",
  "com.google.code.findbugs:jsr305:3.0.2",
  "com.squareup.okhttp3:okhttp:4.10.0",
  "com.uber.nullaway:nullaway:0.10.2",
  "org.assertj:assertj-core:3.23.1",
  "org.awaitility:awaitility:4.2.0",
  "org.bouncycastle:bcpkix-jdk15on:1.70",
  "org.junit-pioneer:junit-pioneer:1.7.1",
  "org.skyscreamer:jsonassert:1.5.1"
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
    for (dependency in CORE_DEPENDENCIES) {
      api(dependency)
      val split = dependency.split(':')
      dependencyVersions[split[0]] = split[2]
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
