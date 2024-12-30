plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("com.github.johnrengelman.shadow")
}

description = "OpenTelemetry Java Agent Extension that enables authentication support for OTLP exporters"
otelJava.moduleName.set("io.opentelemetry.contrib.gcp.auth")

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  // We use `compileOnly` dependency because during runtime all necessary classes are provided by
  // javaagent itself.
  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-exporter-otlp")

  // Only dependencies added to `implementation` configuration will be picked up by Shadow plugin
  implementation("com.google.auth:google-auth-library-oauth2-http:1.30.1")

  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
  useJUnitPlatform()
}
