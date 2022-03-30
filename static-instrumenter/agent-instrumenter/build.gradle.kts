import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("otel.java-conventions")
  id("com.github.johnrengelman.shadow")
  application
}

description = "OpenTelemetry Java Static Instrumentation Agent"

dependencies {
  implementation("net.bytebuddy:byte-buddy-dep:1.12.8")
  implementation("org.slf4j:slf4j-api")
  runtimeOnly("org.slf4j:slf4j-simple")

  // Used by byte-buddy but not brought in as a transitive dependency.
  compileOnly("com.google.code.findbugs:annotations")
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
  }
}

// this is necessary to be able to use logging tools already present in the agent
tasks.withType<ShadowJar>().configureEach {
  relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
  archiveFileName.set("opentelemetry-agent-instrumenter.jar")
}

application.mainClass.set("io.opentelemetry.contrib.staticinstrumenter.AgentModificationMain")
