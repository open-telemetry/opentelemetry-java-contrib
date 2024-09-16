plugins {
  `java-platform`
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

val otelInstrumentationVersion = "2.8.0-alpha"

val DEPENDENCY_BOMS = listOf(
  "com.fasterxml.jackson:jackson-bom:2.17.2",
  "com.google.guava:guava-bom:33.3.0-jre",
  "com.linecorp.armeria:armeria-bom:1.30.1",
  "org.junit:junit-bom:5.11.0",
  "io.grpc:grpc-bom:1.66.0",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${otelInstrumentationVersion}",
  "org.testcontainers:testcontainers-bom:1.20.1"
)

val autoServiceVersion = "1.1.1"
val autoValueVersion = "1.11.0"
val errorProneVersion = "2.32.0"
val prometheusVersion = "0.16.0"
val mockitoVersion = "4.11.0"
val slf4jVersion = "2.0.16"
val semConvVersion = "1.27.0-alpha"

val CORE_DEPENDENCIES = listOf(
  "com.google.auto.service:auto-service:${autoServiceVersion}",
  "com.google.auto.service:auto-service-annotations:${autoServiceVersion}",
  "com.google.auto.value:auto-value:${autoValueVersion}",
  "com.google.auto.value:auto-value-annotations:${autoValueVersion}",
  "com.google.errorprone:error_prone_annotations:${errorProneVersion}",
  "com.google.errorprone:error_prone_core:${errorProneVersion}",
  "io.github.netmikey.logunit:logunit-jul:2.0.0",
  "io.opentelemetry.proto:opentelemetry-proto:1.0.0-alpha",
  "io.opentelemetry.semconv:opentelemetry-semconv:${semConvVersion}",
  "io.opentelemetry.semconv:opentelemetry-semconv-incubating:${semConvVersion}",
  "io.prometheus:simpleclient:${prometheusVersion}",
  "io.prometheus:simpleclient_common:${prometheusVersion}",
  "io.prometheus:simpleclient_httpserver:${prometheusVersion}",
  "org.mockito:mockito-core:${mockitoVersion}",
  "org.mockito:mockito-inline:${mockitoVersion}",
  "org.mockito:mockito-junit-jupiter:${mockitoVersion}",
  "org.slf4j:slf4j-api:${slf4jVersion}",
  "org.slf4j:slf4j-simple:${slf4jVersion}",
  "org.slf4j:log4j-over-slf4j:${slf4jVersion}",
  "org.slf4j:jcl-over-slf4j:${slf4jVersion}",
  "org.slf4j:jul-to-slf4j:${slf4jVersion}"
)

val DEPENDENCIES = listOf(
  "com.google.code.findbugs:annotations:3.0.1u2",
  "com.google.code.findbugs:jsr305:3.0.2",
  "com.squareup.okhttp3:okhttp:4.12.0",
  "com.uber.nullaway:nullaway:0.11.2",
  "org.assertj:assertj-core:3.26.3",
  "org.awaitility:awaitility:4.2.2",
  "org.bouncycastle:bcpkix-jdk15on:1.70",
  "org.junit-pioneer:junit-pioneer:1.9.1",
  "org.skyscreamer:jsonassert:1.5.3",
  "org.apache.kafka:kafka-clients:3.8.0",
  "org.testcontainers:kafka:1.20.1",
  "com.lmax:disruptor:3.4.4",
  "org.jctools:jctools-core:4.0.5",
  "tools.profiler:async-profiler:3.0",
  "com.blogspot.mydailyjava:weak-lock-free:0.18",
  "org.agrona:agrona:1.22.0"
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
