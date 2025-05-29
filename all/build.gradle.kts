plugins {
  `jacoco-report-aggregation`

  id("otel.java-conventions")
}

description = "OpenTelemetry Contrib All"

dependencies {
   rootProject.subprojects.forEach { subproject ->
    // Generate aggregate coverage report for published modules that enable jacoco.
    subproject.plugins.withId("jacoco") {
      subproject.plugins.withId("maven-publish") {
        // TODO(anuraaga): Figure out how to avoid transitive dependencies being pulled into jacoco due to the use
        // of shadow plugin.
        if (subproject.name != "jmx-metrics") {
          implementation(project(subproject.path)) {
            isTransitive = false
          }
        }
      }
    }
  }
}

tasks {
  // We don't compile anything here. This project is mostly for
  // aggregating jacoco reports and it doesn't work if this isn't at least as high as the
  // highest supported Java version in any of our projects. Most of our projects target
  // Java 8, but some target Java 11 or 17.
  withType(JavaCompile::class) {
    options.release.set(17)
  }
}

afterEvaluate {
  tasks {
    testCodeCoverageReport {
      classDirectories.setFrom(
        classDirectories.files.map {
          zipTree(it).filter {
            // Exclude mrjar (jacoco complains), shaded, and generated code
            !it.absolutePath.contains("META-INF/versions/") &&
              !it.absolutePath.contains("AutoValue_")
          }
        },
      )

      reports {
        // xml is usually used to integrate code coverage with
        // other tools like SonarQube, Coveralls or Codecov
        xml.required.set(true)

        // HTML reports can be used to see code coverage
        // without any external tools
        html.required.set(true)
      }
    }
  }
}

dependencyCheck {
  skip = true
}
