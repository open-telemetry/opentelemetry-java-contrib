plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry Baggage Span Processor"
otelJava.moduleName.set("io.opentelemetry.contrib.baggage.processor")

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")
  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-common")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator")

  testAnnotationProcessor("com.google.auto.service:auto-service")
  testCompileOnly("com.google.auto.service:auto-service-annotations")
  testImplementation("io.opentelemetry:opentelemetry-sdk-common")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("com.google.guava:guava")
  testImplementation("org.awaitility:awaitility")
}

configurations.all {
  // todo remove version number before merging, should be provided by the bom
  resolutionStrategy {
    force("io.opentelemetry:opentelemetry-sdk-common:1.52.0")
    force("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.52.0")
  }
}
