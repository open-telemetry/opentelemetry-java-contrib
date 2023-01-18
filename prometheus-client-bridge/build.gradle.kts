plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Prometheus client bridge"

dependencies {
  api("io.opentelemetry:opentelemetry-sdk-metrics")
  implementation("io.prometheus:simpleclient")

  testImplementation("com.google.guava:guava")
  testImplementation("io.prometheus:simpleclient_httpserver")
}