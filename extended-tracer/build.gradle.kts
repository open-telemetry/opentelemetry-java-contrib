plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Tracing Utilities"
otelJava.moduleName.set("io.opentelemetry.contrib.extended-tracer")

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
}
