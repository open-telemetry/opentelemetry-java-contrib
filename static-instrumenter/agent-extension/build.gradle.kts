plugins {
  id("otel.java-conventions")
}

description = "Extension for OpenTelemetry Java Agent"

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_11)
}

// can't use bom since that will cause conflicts when updating to the latest SDK version
// and before updating to the latest instrumentation version
val otelInstrumentationVersion = "2.10.0"
val otelInstrumentationAlphaVersion = "2.10.0-alpha"

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:$otelInstrumentationVersion")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-muzzle:$otelInstrumentationAlphaVersion")

  compileOnly(project(":static-instrumenter:bootstrap"))
}
