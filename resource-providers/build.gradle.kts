plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Contributed ResourceProviders"

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")

  compileOnly("com.google.auto.service:auto-service")
  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-semconv")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  testImplementation("io.opentelemetry:opentelemetry-semconv")
  testImplementation("com.google.auto.service:auto-service")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling") {
    exclude("io.opentelemetry.javaagent", "opentelemetry-javaagent-tooling-java9")
  }
}
