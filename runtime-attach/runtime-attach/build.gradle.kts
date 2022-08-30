plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "To runtime attach the OpenTelemetry Java Instrumentation agent"

val agent: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  implementation(project(":runtime-attach:runtime-attach-core"))
  agent("io.opentelemetry.javaagent:opentelemetry-javaagent")

  // Used by byte-buddy but not brought in as a transitive dependency.
  compileOnly("com.google.code.findbugs:annotations")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit-pioneer:junit-pioneer")
  testImplementation("org.assertj:assertj-core")
}

tasks.test {
  useJUnitPlatform()
  setForkEvery(1) // One JVM by test class to avoid a test class launching a runtime attachment influences the behavior of another test class
}

tasks {
  jar {
    inputs.files(agent)
    from({
      agent.singleFile
    })
    rename { "otel-agent.jar" }
  }
}
