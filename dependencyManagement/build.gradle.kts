plugins {
  `java-platform`
}

val otelInstrumentationVersion = "2.17.0-alpha"
val semconvVersion = "1.34.0"

javaPlatform {
  allowDependencies()
}

dependencies {
  // boms that are only used by tests should be added in otel.java-conventions.gradle.kts
  // under JvmTestSuite so they don't show up as runtime dependencies in license and vulnerability scans
  // (the constraints section below doesn't have this issue, and will only show up
  // as runtime dependencies if they are actually used as runtime dependencies)
  api(enforcedPlatform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${otelInstrumentationVersion}"))
  api(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.19.1"))

  constraints {
    api("io.opentelemetry.semconv:opentelemetry-semconv:${semconvVersion}")
    api("io.opentelemetry.semconv:opentelemetry-semconv-incubating:${semconvVersion}")

    api("com.google.auto.service:auto-service:1.1.1")
    api("com.google.auto.service:auto-service-annotations:1.1.1")
    api("com.google.auto.value:auto-value:1.11.0")
    api("com.google.auto.value:auto-value-annotations:1.11.0")
    api("com.google.errorprone:error_prone_annotations:2.39.0")
    api("com.google.errorprone:error_prone_core:2.39.0")
    api("io.github.netmikey.logunit:logunit-jul:2.0.0")
    api("io.opentelemetry.proto:opentelemetry-proto:1.7.0-alpha")
    api("io.prometheus:simpleclient:0.16.0")
    api("io.prometheus:simpleclient_common:0.16.0")
    api("io.prometheus:simpleclient_httpserver:0.16.0")
    api("org.mockito:mockito-core:4.11.0")
    api("org.mockito:mockito-inline:4.11.0")
    api("org.mockito:mockito-junit-jupiter:4.11.0")
    api("org.slf4j:slf4j-api:2.0.17")
    api("org.slf4j:slf4j-simple:2.0.17")
    api("org.slf4j:log4j-over-slf4j:2.0.17")
    api("org.slf4j:jcl-over-slf4j:2.0.17")
    api("org.slf4j:jul-to-slf4j:2.0.17")

    api("com.google.code.findbugs:annotations:3.0.1u2")
    api("com.google.code.findbugs:jsr305:3.0.2")
    api("com.squareup.okhttp3:okhttp:5.0.0")
    api("com.uber.nullaway:nullaway:0.12.7")
    api("org.assertj:assertj-core:3.27.3")
    api("org.awaitility:awaitility:4.3.0")
    api("org.bouncycastle:bcpkix-jdk15on:1.70")
    api("org.junit-pioneer:junit-pioneer:1.9.1")
    api("org.skyscreamer:jsonassert:1.5.3")
    api("org.apache.kafka:kafka-clients:4.0.0")
    api("org.testcontainers:kafka:1.21.3")
    api("com.lmax:disruptor:3.4.4")
    api("org.jctools:jctools-core:4.0.5")
    api("tools.profiler:async-profiler:4.0")
    api("com.blogspot.mydailyjava:weak-lock-free:0.18")
    api("org.agrona:agrona:1.22.0")
  }
}
