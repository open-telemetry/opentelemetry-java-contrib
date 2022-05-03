plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Utility to attach OpenTelemetry Java Instrumentation agent from classpath"

dependencies {
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent:1.6.0")
  implementation("net.bytebuddy:byte-buddy-agent:1.11.18")

  // Used by byte-buddy but not brought in as a transitive dependency.
  compileOnly("com.google.code.findbugs:annotations")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent:1.6.0")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit-pioneer:junit-pioneer")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.apache.httpcomponents:httpclient:4.5.13")
}
