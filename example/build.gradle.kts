plugins {
  id("otel.java-conventions")
}

description = "An example OpenTelemetry Java Contrib library"

tasks {
  jar {
    manifest {
      attributes["Main-Class"] = "io.opentelemetry.contrib.example.Library"
    }
  }
}