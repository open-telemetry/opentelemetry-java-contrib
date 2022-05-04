import groovy.util.Node

plugins {
  id("otel.java-conventions")
  id("de.benediktritter.maven-plugin-development") version "0.3.1"
  `maven-publish`
}

description = "Maven3 plugin for static instrumentation of projects code and dependencies"

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

publishing {
  publications {
    create<MavenPublication>("mavenPlugin") {

      from(components["java"])

      pom {
        name.set("OpenTelemetry Java Contrib")
        url.set("https://github.com/open-telemetry/opentelemetry-java-contrib")
        artifactId = "static-instrumentation-maven-plugin"
        description.set(project.description)

        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }

        developers {
          developer {
            id.set("opentelemetry")
            name.set("OpenTelemetry")
            url.set("https://github.com/open-telemetry/opentelemetry-java-contrib/discussions")
          }
        }

        scm {
          connection.set("scm:git:git@github.com:open-telemetry/opentelemetry-java-contrib.git")
          developerConnection.set("scm:git:git@github.com:open-telemetry/opentelemetry-java-contrib.git")
          url.set("git@github.com:open-telemetry/opentelemetry-java-contrib.git")
        }

        // remove dependencyManagement tag causing issues
        withXml {
          val dependencyManagement = asNode().get("dependencyManagement") as groovy.util.NodeList
          asNode().remove(dependencyManagement[0] as Node?)
        }
      }
    }
  }
}

// sets artifactId of the plugin descriptor
mavenPlugin {
  artifactId.set("static-instrumentation-maven-plugin")
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
