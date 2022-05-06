plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry AWS X-Ray Support"

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk-trace")

  compileOnly("io.micrometer:micrometer-core:1.1.0")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")

  testImplementation("com.linecorp.armeria:armeria-junit5")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("com.google.guava:guava")
  testImplementation("org.slf4j:slf4j-simple")
  testImplementation("org.skyscreamer:jsonassert")
}
