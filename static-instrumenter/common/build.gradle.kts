plugins {
  id("otel.java-conventions")
}

description = "Extension for OpenTelemetry Java Agent"

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
  }
}
