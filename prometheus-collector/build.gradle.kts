plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Prometheus Collector - exposes OTel metrics to a Prometheus registry"

dependencies {
  api("io.opentelemetry:opentelemetry-sdk-metrics")
  implementation("io.prometheus:simpleclient_httpserver")

  testImplementation("com.google.guava:guava")
}
