plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler and exporter implementations for consistent sampling"
otelJava.moduleName.set("io.opentelemetry.contrib.sampler")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk-trace")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  api("io.opentelemetry:opentelemetry-sdk-extension-incubator")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

tasks {
  withType<Test>().configureEach {
    develocity.testRetry {
      // TODO (trask) fix flaky tests and remove this workaround
      if (System.getenv().containsKey("CI")) {
        maxRetries.set(5)
      }
    }
  }
}
