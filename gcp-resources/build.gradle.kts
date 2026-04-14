plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry GCP Resources Support"
otelJava.moduleName.set("io.opentelemetry.contrib.gcp.resource")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-api-incubator")
  api("io.opentelemetry:opentelemetry-sdk")

  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  implementation("com.fasterxml.jackson.core:jackson-core")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-api-incubator")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("org.mockito:mockito-core")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("com.google.guava:guava")

  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.2")
}
