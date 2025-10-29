plugins {
  `maven-publish`
  signing
}

val tagVersion: String by rootProject.extra

publishing {
  publications {
    register<MavenPublication>("maven") {
      plugins.withId("java-platform") {
        from(components["javaPlatform"])
      }
      plugins.withId("java-library") {
        from(components["java"])
      }

      versionMapping {
        allVariants {
          fromResolutionResult()
        }
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
          tag.set(tagVersion)
          url.set("https://github.com/open-telemetry/opentelemetry-java-contrib/tree/${tagVersion}")
        }

        issueManagement {
          system.set("GitHub Issues")
          url.set("https://github.com/open-telemetry/opentelemetry-java-contrib/issues")
        }

        ciManagement {
          system.set("GitHub Actions")
          url.set("https://github.com/open-telemetry/opentelemetry-java-contrib/actions")
        }

        withXml {
          // Since 5.0 okhttp uses gradle metadata to choose either okhttp-jvm or okhttp-android.
          // This does not work for maven builds that don't understand gradle metadata. They end up
          // using the okhttp artifact that is an empty jar. Here we replace usages of okhttp with
          // okhttp-jvm so that maven could get the actual okhttp dependency instead of the empty jar.
          var result = asString()
          var modified = result.toString().replace(">okhttp<", ">okhttp-jvm<")
          result.clear()
          result.append(modified)
        }
      }
    }
  }
}

// Sign only if we have a key to do so
val signingKey: String? = System.getenv("GPG_PRIVATE_KEY")
// Stub out entire signing block off of CI since Gradle provides no way of lazy configuration of
// signing tasks.
if (System.getenv("CI") != null && signingKey != null) {
  signing {
    useInMemoryPgpKeys(signingKey, System.getenv("GPG_PASSWORD"))
    sign(publishing.publications["maven"])
  }
}
