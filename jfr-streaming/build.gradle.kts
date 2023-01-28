plugins {
  id("otel.java-conventions")
  id("java-test-fixtures")

  id("otel.publish-conventions")
}

// Disable publishing test fixtures
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

dependencies {
  implementation("io.opentelemetry:opentelemetry-api")

  testImplementation(testFixtures(project))

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testFixturesImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
  testFixturesImplementation("io.opentelemetry:opentelemetry-api")
  testFixturesImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testFixturesImplementation("org.awaitility:awaitility")
  testFixturesImplementation("org.assertj:assertj-core:3.24.2")
}

tasks {
  withType(JavaCompile::class) {
    options.release.set(17)
  }

  withType<Javadoc>().configureEach {
    with(options as StandardJavadocDocletOptions) {
      source = "17"
    }
  }

  test {
    useJUnitPlatform()
  }
}

testing {
  suites {

    val serialGcTest by registering(JvmTestSuite::class) {
      dependencies {
        implementation("io.opentelemetry:opentelemetry-sdk-testing")
        implementation(project.dependencies.testFixtures(project))
      }
      targets {
        all {
          testTask {
            jvmArgs = listOf("-XX:+UseSerialGC")
          }
        }
      }
    }
    val parallelGcTest by registering(JvmTestSuite::class) {
      dependencies {
        implementation("io.opentelemetry:opentelemetry-sdk-testing")
        implementation(project.dependencies.testFixtures(project))
      }
      targets {
        all {
          testTask {
            jvmArgs = listOf("-XX:+UseParallelGC")
          }
        }
      }
    }
    val g1GcTest by registering(JvmTestSuite::class) {
      dependencies {
        implementation("io.opentelemetry:opentelemetry-sdk-testing")
        implementation(project.dependencies.testFixtures(project))
      }
      targets {
        all {
          testTask {
            jvmArgs = listOf("-XX:+UseG1GC")
          }
        }
      }
    }
  }
}

tasks {
  check {
    dependsOn(testing.suites)
  }
}
