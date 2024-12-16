plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "To help in create an OpenTelemetry distro able to runtime attach an OpenTelemetry Java Instrumentation agent"
otelJava.moduleName.set("io.opentelemetry.contrib.attach.core")

dependencies {
  implementation("net.bytebuddy:byte-buddy-agent:1.15.11")

  // Used by byte-buddy but not brought in as a transitive dependency.
  compileOnly("com.google.code.findbugs:annotations")
}
