plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Maven3 plugin for static instrumentation of projects code and dependencies"
base.archivesName.set("static-instrumentation-maven-plugin")

val instrumentedAgent by configurations.creating

dependencies {
  implementation("org.apache.maven:maven-plugin-api:3.6.3")
  implementation("org.apache.maven:maven-project:2.2.1")
  compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.0")
  compileOnly("org.apache.maven:maven-core:3.5.0")
  compileOnly("org.slf4j:slf4j-api")

  testImplementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.0")
  testImplementation("org.apache.maven:maven-core:3.5.0")
  testImplementation("org.slf4j:slf4j-simple")

  instrumentedAgent(project(":static-instrumenter:agent-instrumenter", "shadow"))
}

task<Copy>("copyAgent") {
  into("$buildDir/resources/main")
  from(configurations.getByName("instrumentedAgent")) {
    rename { "opentelemetry-agent.jar" }
  }
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
    dependsOn("copyAgent")
  }
  withType<Javadoc>().configureEach {
    with(options as StandardJavadocDocletOptions) {
      source = "11"
    }
  }
}
