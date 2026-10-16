plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "To runtime attach the OpenTelemetry Java Instrumentation agent"
otelJava.moduleName.set("io.opentelemetry.contrib.attach")

val agent = configurations.create("agent") {
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
    getByName<JvmTestSuite>("test") {
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

    register<JvmTestSuite>("testAgentDisabledByEnvironmentVariable") {
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

    register<JvmTestSuite>("testAgentDisabledBySystemProperty") {
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
