plugins {
  id("otel.java-conventions")
}

description = "Maven3 plugin for static instrumentation of projects code and dependencies"

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  implementation("org.apache.maven:maven-plugin-api:3.6.3")
  implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.0")
  implementation("org.apache.maven:maven-project:2.2.1")
  implementation("org.apache.maven:maven-core:3.3.9")
  implementation("org.apache.maven.resolver:maven-resolver:1.6.2")
  implementation("org.apache.maven.resolver:maven-resolver-spi:1.6.2")
  implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.6.2")
  implementation("org.apache.maven.resolver:maven-resolver-transport-file:1.6.2")
  implementation("org.apache.maven.resolver:maven-resolver-transport-http:1.6.2")
  implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:1.12.0-alpha")
  implementation("org.apache.commons:commons-lang3:3.12.0")
  testImplementation("org.slf4j:slf4j-simple")
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
  }
}
