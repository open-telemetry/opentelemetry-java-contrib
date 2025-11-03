plugins {
  application
  id("com.gradleup.shadow")

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

  // for jmxmp protocol support
  runtimeOnly("org.terracotta:jmxremote_optional-tc:1.0.8")

  implementation("io.opentelemetry.instrumentation:opentelemetry-jmx-metrics")
  implementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

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
        implementation("io.opentelemetry.proto:opentelemetry-proto:1.8.0-alpha")
        implementation("org.bouncycastle:bcprov-jdk18on:1.82")
        implementation("org.bouncycastle:bcpkix-jdk18on:1.82")
      }
    }
  }
}

tasks {
  shadowJar {
    mergeServiceFiles()

    duplicatesStrategy = DuplicatesStrategy.INCLUDE // required for mergeServiceFiles()

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
    inputs.files(layout.files(shadowJar))
    systemProperty("shadow.jar.path", shadowJar.get().archiveFile.get().asFile.absolutePath)

    val testAppTask = project("test-app").tasks.named<Jar>("jar")
    dependsOn(testAppTask)
    inputs.files(layout.files(testAppTask))
    systemProperty("app.jar.path", testAppTask.get().archiveFile.get().asFile.absolutePath)

    val testWarTask = project("test-webapp").tasks.named<Jar>("war")
    dependsOn(testWarTask)
    inputs.files(layout.files(testWarTask))
    systemProperty("app.war.path", testWarTask.get().archiveFile.get().asFile.absolutePath)

    systemProperty("gradle.project.version", "${project.version}")

    develocity.testRetry {
      // TODO (trask) fix flaky tests and remove this workaround
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

//
// task that run weaver within gradle;
tasks.register("runWeaver", Exec::class) {
  standardOutput = System.out
  executable = "docker"

  val WEAVER_CONTAINER = "otel/weaver@sha256:5425ade81dc22ddd840902b0638b4b6a9186fb654c5b50c1d1ccd31299437390"
  val projectRoot = project.layout.projectDirectory.asFile.absolutePath
  val modelPath = project.layout.projectDirectory.dir("model").asFile.absolutePath
  val templatePath = project.layout.projectDirectory.dir("templates").asFile.absolutePath
  val outputPath = project.layout.projectDirectory.file("src/main/resources").asFile.absolutePath

  val file_args = if (org.gradle.internal.os.OperatingSystem.current().isWindows())
    // Don't need to worry about file system permissions in docker.
    listOf()
  else {
    // Make sure we run as local file user
    val unix = com.sun.security.auth.module.UnixSystem()
    val uid = unix.getUid() // $(id -u $USERNAME)
    val gid = unix.getGid() // $(id -g $USERNAME)
    listOf("-u", "$uid:$gid")
  }

  val weaver_args = listOf(
    "--rm",
    "--platform=linux/x86_64",
    "--mount", "type=bind,source=${modelPath},target=/home/weaver/source,readonly",
    "--mount", "type=bind,source=${templatePath},target=/home/weaver/templates,readonly",
    "--mount", "type=bind,source=${outputPath},target=/home/weaver/target",
    "--mount", "type=bind,source=${projectRoot},target=/home/weaver",
    "${WEAVER_CONTAINER}",
    "registry", "generate",
    "--registry=/home/weaver/source",
    "--templates=/home/weaver/templates",
    "rules",
    "/home/weaver/target/")

  setArgs(listOf("run") + file_args + weaver_args)
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
