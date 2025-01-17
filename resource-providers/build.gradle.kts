plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Contributed ResourceProviders"
otelJava.moduleName.set("io.opentelemetry.contrib.resourceproviders")

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")

  compileOnly("com.google.auto.service:auto-service")
  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("com.google.auto.service:auto-service")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
