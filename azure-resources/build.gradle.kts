plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
  id("maven-publish")
}

description = "OpenTelemetry Azure Resources Support"
otelJava.moduleName.set("io.opentelemetry.contrib.azure.resource")

// enable publishing to maven local
java {
  withSourcesJar()
}

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-api-incubator")
  api("io.opentelemetry:opentelemetry-sdk")

  implementation("io.opentelemetry.semconv:opentelemetry-semconv")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.squareup.okhttp3:okhttp")

  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-api-incubator")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")

  testImplementation("com.google.guava:guava")

  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.assertj:assertj-core")
  testImplementation("com.linecorp.armeria:armeria-junit5")
}

tasks {
  withType<Test>().configureEach {
    environment(
      "WEBSITE_SITE_NAME" to "my-function",
      "FUNCTIONS_EXTENSION_VERSION" to "1.2.3"
    )
    jvmArgs("-Dotel.experimental.config.file=${project.projectDir.resolve("src/test/resources/declarative-config.yaml")}")
  }
}
