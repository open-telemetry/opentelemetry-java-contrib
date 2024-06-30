plugins {
  application
  id("com.github.johnrengelman.shadow")

  id("otel.groovy-conventions")
  id("otel.publish-conventions")
}

description = "JMX metrics gathering Groovy script runner"
otelJava.moduleName.set("io.opentelemetry.contrib.jmxmetrics")

application.mainClass.set("io.opentelemetry.contrib.jmxmetrics.JmxMetrics")

repositories {
  mavenCentral()
  maven {
    setUrl("https://repo.terracotta.org/maven2")
    content {
      includeGroupByRegex("""org\.terracotta.*""")
    }
  }
}

val groovyVersion = "3.0.22"

dependencies {
  api(platform("org.codehaus.groovy:groovy-bom:$groovyVersion"))

  implementation("io.grpc:grpc-netty-shaded")
  implementation("org.codehaus.groovy:groovy-jmx")
  implementation("org.codehaus.groovy:groovy")
  implementation("io.prometheus:simpleclient")
  implementation("io.prometheus:simpleclient_httpserver")
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-sdk-metrics")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  implementation("io.opentelemetry:opentelemetry-sdk-testing")
  implementation("io.opentelemetry:opentelemetry-exporter-logging")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
  implementation("io.opentelemetry:opentelemetry-exporter-prometheus")

  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")

  runtimeOnly("org.terracotta:jmxremote_optional-tc:1.0.8")

  testImplementation("org.slf4j:slf4j-api")
  testImplementation("org.slf4j:slf4j-simple")
  testImplementation("org.junit-pioneer:junit-pioneer")
  testImplementation("org.awaitility:awaitility")
}

testing {
  suites {
    val integrationTest by registering(JvmTestSuite::class) {
      dependencies {
        implementation("com.linecorp.armeria:armeria-grpc")
        implementation("com.linecorp.armeria:armeria-junit5")
        implementation("io.opentelemetry.proto:opentelemetry-proto:0.20.0-alpha")
        implementation("org.testcontainers:junit-jupiter")
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
