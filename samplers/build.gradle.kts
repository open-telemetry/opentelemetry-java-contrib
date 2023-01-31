plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler which makes its decision based on semantic attributes values"
otelJava.moduleName.set("io.opentelemetry.contrib.samplers")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  api("io.opentelemetry:opentelemetry-semconv")
}
