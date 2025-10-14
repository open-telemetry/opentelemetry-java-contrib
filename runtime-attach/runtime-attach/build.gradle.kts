plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "To runtime attach the OpenTelemetry Java Instrumentation agent"
otelJava.moduleName.set("io.opentelemetry.contrib.attach")

val agent: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  implementation(project(":runtime-attach:runtime-attach-core"))
  agent("io.opentelemetry.javaagent:opentelemetry-javaagent")

  // Used by byte-buddy but not brought in as a transitive dependency.
  compileOnly("com.google.code.findbugs:annotations")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.assertj:assertj-core")
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      targets.all {
        testTask.configure {
          setForkEvery(1) // One JVM by test class to avoid a test class launching a runtime attachment influences the behavior of another test class
          filter {
            excludeTestsMatching("AgentDisabledByEnvironmentVariableTest")
            excludeTestsMatching("AgentDisabledBySystemPropertyTest")
          }
        }
      }
    }

    val testAgentDisabledByEnvironmentVariable by registering(JvmTestSuite::class) {
      targets.all {
        testTask.configure {
          setForkEvery(1)
          filter {
            includeTestsMatching("AgentDisabledByEnvironmentVariableTest")
          }
          include("**/AgentDisabledByEnvironmentVariableTest.*")
          environment("OTEL_JAVAAGENT_ENABLED", "false")
        }
      }
    }

    val testAgentDisabledBySystemProperty by registering(JvmTestSuite::class) {
      targets.all {
        testTask.configure {
          setForkEvery(1)
          filter {
            includeTestsMatching("AgentDisabledBySystemPropertyTest")
          }
          include("**/AgentDisabledBySystemPropertyTest.*")
          jvmArgs("-Dotel.javaagent.enabled=false")
        }
      }
    }
  }
}

tasks {
  jar {
    inputs.files(agent)
    from({
      agent.singleFile
    })
    rename("^(.*)\\.jar\$", "otel-agent.jar")
  }
}
