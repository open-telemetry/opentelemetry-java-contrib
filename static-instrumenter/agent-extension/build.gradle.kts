plugins {
  id("otel.java-conventions")
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

description = "Extension for OpenTelemetry Java Agent"

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-instrumentation-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  // TODO: replace with the following file once new javaagent is released
  compileOnly(files("libs/opentelemetry-muzzle.jar"))
//  compileOnly("io.opentelemetry.javaagent:opentelemetry-muzzle")

  compileOnly(project(":static-instrumenter:bootstrap"))
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
  }
}
