plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry Baggage Span Processor"
otelJava.moduleName.set("io.opentelemetry.contrib.baggage.processor")

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")

  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk")
  compileOnly("com.google.auto.service:auto-service")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")

  testImplementation("com.google.auto.service:auto-service")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("com.google.guava:guava")
  testImplementation("org.awaitility:awaitility")
}
