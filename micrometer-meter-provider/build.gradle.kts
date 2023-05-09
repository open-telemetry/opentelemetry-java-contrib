plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry Micrometer MeterProvider"
otelJava.moduleName.set("io.opentelemetry.contrib.metrics.micrometer")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk-metrics")

  compileOnly("io.micrometer:micrometer-core:1.1.0") // do not auto-update this version
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")

  testImplementation("io.micrometer:micrometer-core:1.9.5")
}

testing {
  suites {
    val integrationTest by registering(JvmTestSuite::class) {
      dependencies {
        implementation("io.micrometer:micrometer-registry-prometheus:1.11.0")
      }
    }
  }
}
