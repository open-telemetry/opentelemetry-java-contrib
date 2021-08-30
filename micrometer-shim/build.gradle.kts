plugins {
    id("otel.java-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Metrics Micrometer Shim"

dependencies {
    api("io.opentelemetry:opentelemetry-api")
    api("io.opentelemetry:opentelemetry-api-metrics")
    implementation("io.micrometer:micrometer-core:1.7.3")
}