plugins {
  id("otel.java-conventions")
}

dependencies {
  implementation("io.opentelemetry:opentelemetry-api")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
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
