plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry AWS X-Ray Support"
otelJava.moduleName.set("io.opentelemetry.contrib.awsxray")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk-trace")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  implementation("com.squareup.okhttp3:okhttp")
  implementation("io.opentelemetry:opentelemetry-semconv")

  annotationProcessor("com.google.auto.service:auto-service")
  testImplementation("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")

  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.fasterxml.jackson.core:jackson-databind")

  testImplementation("com.linecorp.armeria:armeria-junit5")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("com.google.guava:guava")
  testImplementation("org.slf4j:slf4j-simple")
  testImplementation("org.skyscreamer:jsonassert")
}

testing {
  suites {
    val awsTest by registering(JvmTestSuite::class) {
      dependencies {
        implementation("io.opentelemetry:opentelemetry-exporter-otlp")
        implementation("io.opentelemetry:opentelemetry-sdk-testing")
        implementation("org.testcontainers:junit-jupiter")
        runtimeOnly("org.slf4j:slf4j-simple")
      }
    }
  }
}
