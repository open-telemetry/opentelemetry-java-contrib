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
  implementation("io.opentelemetry:opentelemetry-sdk-testing")

  implementation("io.opentelemetry.instrumentation:opentelemetry-jmx-metrics")

  testImplementation("org.junit-pioneer:junit-pioneer")
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
    systemProperty("gradle.project.version", "${project.version}")
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
