plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry Micrometer MeterProvider"
otelJava.moduleName.set("io.opentelemetry.contrib.metrics.micrometer")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk-metrics")
  api("io.opentelemetry:opentelemetry-api-incubator")

  compileOnly("io.micrometer:micrometer-core:1.5.0") // do not auto-update this version
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")

  testImplementation("io.micrometer:micrometer-core:1.13.4")
}

testing {
  suites {
    val integrationTest by registering(JvmTestSuite::class) {
      dependencies {
        implementation("io.micrometer:micrometer-registry-prometheus:1.13.4")
      }
    }
  }
}
