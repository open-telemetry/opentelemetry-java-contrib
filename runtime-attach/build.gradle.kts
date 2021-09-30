plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Utility to attach OpenTelemetry Java Instrumentation agent from classpath"

dependencies {
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent:1.6.0")
  implementation("net.bytebuddy:byte-buddy-agent:1.11.18")
}
