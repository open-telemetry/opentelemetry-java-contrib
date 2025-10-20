plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "SpanExporter based on Kafka"

otelJava {
  moduleName.set("io.opentelemetry.contrib.kafka")
  // kafka 4 requires java 11
  minJavaVersionSupported.set(JavaVersion.VERSION_11)
}

dependencies {
  api("io.opentelemetry:opentelemetry-sdk-trace")
  api("io.opentelemetry:opentelemetry-sdk-common")
  api("io.opentelemetry.proto:opentelemetry-proto:1.8.0-alpha")
  api("org.apache.kafka:kafka-clients")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("com.google.auto.service:auto-service-annotations")
  compileOnly("com.google.auto.value:auto-value-annotations")
  compileOnly("org.slf4j:slf4j-api")

  runtimeOnly("com.fasterxml.jackson.core:jackson-core")
  runtimeOnly("com.fasterxml.jackson.core:jackson-databind")

  implementation("io.opentelemetry:opentelemetry-exporter-otlp-common")
  implementation("com.google.protobuf:protobuf-java")

  testImplementation("io.opentelemetry:opentelemetry-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("com.google.guava:guava")
  testImplementation("org.testcontainers:testcontainers-junit-jupiter")
  testImplementation("org.testcontainers:testcontainers-kafka")
  testImplementation("org.rnorth.duct-tape:duct-tape")
  testImplementation("org.testcontainers:testcontainers")

  testRuntimeOnly("org.slf4j:slf4j-simple")
}
