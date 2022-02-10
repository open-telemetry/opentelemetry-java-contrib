plugins {
  `jacoco-report-aggregation`

  id("otel.java-conventions")
}

description = "OpenTelemetry Contrib Testing"

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
        }
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
