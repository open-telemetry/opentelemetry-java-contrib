plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Extended Tracer"
otelJava.moduleName.set("io.opentelemetry.contrib.extended-tracer")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
}
