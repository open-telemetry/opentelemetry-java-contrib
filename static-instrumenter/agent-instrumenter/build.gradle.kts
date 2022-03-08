plugins {
  id("otel.java-conventions")
}

description = "OpenTelemetry Java Static Instrumentation Agent"

dependencies {
  implementation("org.slf4j:slf4j-api")
  runtimeOnly("org.slf4j:slf4j-simple")
}
