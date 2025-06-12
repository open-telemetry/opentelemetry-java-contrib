plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry GCP Resources Support"
otelJava.moduleName.set("io.opentelemetry.contrib.gcp.resource")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk")

  // Provides GCP resource detection support
  implementation("com.google.cloud.opentelemetry:detector-resources-support:0.35.0")

  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  implementation("com.fasterxml.jackson.core:jackson-core")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("org.mockito:mockito-core")
  testImplementation("com.google.guava:guava")

  testImplementation("org.junit.jupiter:junit-jupiter-api")
}
