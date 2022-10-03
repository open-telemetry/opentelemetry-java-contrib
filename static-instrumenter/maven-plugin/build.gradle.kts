import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("com.github.johnrengelman.shadow")

  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Maven3 plugin for static instrumentation of projects code and dependencies"
base.archivesName.set("static-instrumentation-maven-plugin")

dependencies {
  implementation("org.apache.maven:maven-plugin-api:3.5.0") // do not auto-update this version
  implementation("org.apache.maven:maven-project:2.2.1")
  compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.4")
  compileOnly("org.apache.maven:maven-core:3.5.0") // do not auto-update this version
  compileOnly("org.slf4j:slf4j-api")

  testImplementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.4")
  testImplementation("org.apache.maven:maven-core:3.5.0")
  testImplementation("org.slf4j:slf4j-simple")
}

tasks {
  processResources {
    val agentJar = project(":static-instrumenter:agent-instrumenter").tasks.getByName("shadowJar", ShadowJar::class)
    dependsOn(agentJar)
    from(agentJar.archiveFile) {
      rename { "opentelemetry-agent.jar" }
    }
  }

  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
  }
  withType<Javadoc>().configureEach {
    with(options as StandardJavadocDocletOptions) {
      source = "11"
    }
  }
}
