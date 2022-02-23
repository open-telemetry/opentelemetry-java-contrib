plugins {
  id("otel.java-conventions")
}

description = "OpenTelemetry Java Static Instrumentation Agent"

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation("org.slf4j:slf4j-api")
  implementation("org.slf4j:slf4j-simple")
  testImplementation("org.mockito:mockito-core:3.+")
}
