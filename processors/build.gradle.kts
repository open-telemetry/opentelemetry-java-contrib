plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Tools to intercept and process signals globally."
otelJava.moduleName.set("io.opentelemetry.contrib.processors")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  // TODO: revert versions before merging
  api("io.opentelemetry:opentelemetry-sdk:1.48.0-SNAPSHOT")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi:1.48.0-SNAPSHOT")
  compileOnly("io.opentelemetry:opentelemetry-api-incubator:1.48.0-alpha-SNAPSHOT")

  // For EventToSpanEventBridge
  implementation("io.opentelemetry:opentelemetry-exporter-otlp-common:1.48.0-SNAPSHOT")
  implementation("com.fasterxml.jackson.core:jackson-core")

  testImplementation("io.opentelemetry:opentelemetry-api-incubator:1.48.0-alpha-SNAPSHOT")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.48.0-SNAPSHOT")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.48.0-SNAPSHOT")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator:1.48.0-alpha-SNAPSHOT")
}
