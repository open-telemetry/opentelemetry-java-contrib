plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
  id("maven-publish")
}

description = "OpenTelemetry GCP Resources Support"
otelJava.moduleName.set("io.opentelemetry.contrib.gcp.resource")

// enable publishing to maven local
java {
  withSourcesJar()
}

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk")

  // Provides GCP resource detection support
//  implementation("com.google.cloud.opentelemetry:detector-resources-support:0.27.0")

  implementation("io.opentelemetry.semconv:opentelemetry-semconv")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.squareup.okhttp3:okhttp")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

//  testImplementation("org.mockito:mockito-core")
  testImplementation("com.google.guava:guava")

  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.assertj:assertj-core")
  testImplementation("com.linecorp.armeria:armeria-junit5")
}
