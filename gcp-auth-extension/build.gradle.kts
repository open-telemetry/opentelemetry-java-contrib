plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry Java Agent Extension that enables authentication support for OTLP exporters"
otelJava.moduleName.set("io.opentelemetry.contrib.gcp.auth")

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
  useJUnitPlatform()
}
