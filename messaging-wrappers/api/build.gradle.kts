plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry Messaging Wrappers"
otelJava.moduleName.set("io.opentelemetry.contrib.messaging.wrappers")

dependencies {
  api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
  api("io.opentelemetry.semconv:opentelemetry-semconv")
  api("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-api-incubator")

  testImplementation("com.google.guava:guava:33.4.8-jre")
  testImplementation(project(":messaging-wrappers:testing"))
}

tasks {
  withType<Test>().configureEach {
    jvmArgs("-Dotel.java.global-autoconfigure.enabled=true")
    jvmArgs("-Dotel.traces.exporter=logging")
    jvmArgs("-Dotel.metrics.exporter=logging")
    jvmArgs("-Dotel.logs.exporter=logging")
  }
}