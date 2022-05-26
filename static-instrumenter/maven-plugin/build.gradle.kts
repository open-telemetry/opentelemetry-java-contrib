plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Maven3 plugin for static instrumentation of projects code and dependencies"
extra["mavenArtifactId"] = "static-instrumentation-maven-plugin"

dependencies {
  implementation("org.apache.maven:maven-plugin-api:3.6.3")
  implementation("org.apache.maven:maven-project:2.2.1")
  compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.0")
  compileOnly("org.apache.maven:maven-core:3.5.0")
  compileOnly("org.slf4j:slf4j-api")

  testImplementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.0")
  testImplementation("org.apache.maven:maven-core:3.5.0")
  testImplementation("org.slf4j:slf4j-simple")
}

tasks {
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
