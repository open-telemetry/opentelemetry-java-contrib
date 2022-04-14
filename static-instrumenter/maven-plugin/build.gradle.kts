
plugins {
  id("otel.java-conventions")
  id("de.benediktritter.maven-plugin-development") version "0.3.1"
  `maven-publish`
}

description = "Maven3 plugin for static instrumentation of projects code and dependencies"

// setting version is needed for maven plugin to run
//version = "1.0-SNAPSHOT"

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
}

publishing {
  publications {
    create<MavenPublication>("mavenPlugin") {

      from(components["java"])

      versionMapping {
        allVariants {
          fromResolutionResult()
        }
      }

      if (findProperty("otel.stable") != "true") {
        val versionParts = version.split('-').toMutableList()
        versionParts[0] += "-alpha"
        version = versionParts.joinToString("-")
      }

      afterEvaluate {
        val mavenGroupId: String? by project
        if (mavenGroupId != null) {
          groupId = mavenGroupId
        }
        artifactId = base.archivesName.get()

        if (!groupId.startsWith("io.opentelemetry.contrib")) {
          throw GradleException("groupId is not set for this project or its parent ${project.parent}")
        }

        pom.description.set(project.description)
      }

      pom {
        name.set("OpenTelemetry Java Contrib")
        url.set("https://github.com/open-telemetry/opentelemetry-java-contrib")

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
      }
    }
  }
}