plugins {
  application
  id("com.github.johnrengelman.shadow")

  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "JMX metrics scraper"
otelJava.moduleName.set("io.opentelemetry.contrib.jmxscraper")

application.mainClass.set("io.opentelemetry.contrib.jmxscraper.JmxScraper")

dependencies {
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-sdk-metrics")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")
  runtimeOnly("io.opentelemetry:opentelemetry-exporter-logging")

  implementation("io.opentelemetry.instrumentation:opentelemetry-jmx-metrics")

  testImplementation("org.junit-pioneer:junit-pioneer")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("org.awaitility:awaitility")
}

testing {
  suites {
    val integrationTest by registering(JvmTestSuite::class) {
      dependencies {
        implementation("org.testcontainers:junit-jupiter")
        implementation("org.slf4j:slf4j-simple")
        implementation("com.linecorp.armeria:armeria-junit5")
        implementation("com.linecorp.armeria:armeria-grpc")
        implementation("io.opentelemetry.proto:opentelemetry-proto:1.4.0-alpha")
      }
    }
  }
}

tasks {
  shadowJar {
    mergeServiceFiles()

    manifest {
      attributes["Implementation-Version"] = project.version
    }
    // This should always be standalone, so remove "-all" to prevent unnecessary artifact.
    archiveClassifier.set("")
  }

  jar {
    archiveClassifier.set("noshadow")
  }

  withType<Test>().configureEach {
    dependsOn(shadowJar)
    systemProperty("shadow.jar.path", shadowJar.get().archiveFile.get().asFile.absolutePath)

    val testAppTask = project("test-app").tasks.named<Jar>("jar")
    dependsOn(testAppTask)
    systemProperty("app.jar.path", testAppTask.get().archiveFile.get().asFile.absolutePath)

    val testWarTask = project("test-webapp").tasks.named<Jar>("war")
    dependsOn(testWarTask)
    systemProperty("app.war.path", testWarTask.get().archiveFile.get().asFile.absolutePath)

    systemProperty("gradle.project.version", "${project.version}")

    develocity.testRetry {
      // You can see tests that were retried by this mechanism in the collected test reports and build scans.
      if (System.getenv().containsKey("CI")) {
        maxRetries.set(5)
      }
    }
  }

  // Because we reconfigure publishing to only include the shadow jar, the Gradle metadata is not correct.
  // Since we are fully bundled and have no dependencies, Gradle metadata wouldn't provide any advantage over
  // the POM anyways so in practice we shouldn't be losing anything.
  withType<GenerateModuleMetadata>().configureEach {
    enabled = false
  }
}

// Don't publish non-shadowed jar (shadowJar is in shadowRuntimeElements)
with(components["java"] as AdhocComponentWithVariants) {
  configurations.forEach {
    withVariantsFromConfiguration(configurations["apiElements"]) {
      skip()
    }
    withVariantsFromConfiguration(configurations["runtimeElements"]) {
      skip()
    }
  }
}
