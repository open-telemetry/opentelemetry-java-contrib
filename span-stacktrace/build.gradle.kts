plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry Java span stacktrace capture module"
otelJava.moduleName.set("io.opentelemetry.contrib.stacktrace")

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  api("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")

  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")

  testAnnotationProcessor("com.google.auto.service:auto-service")
  testCompileOnly("com.google.auto.service:auto-service-annotations")

  testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
}

// todo remove when https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/14497 is merged
configurations.all {
  resolutionStrategy {
    force("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator:2.20.0-alpha-SNAPSHOT")
    force("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:2.19.0")
  }
}
