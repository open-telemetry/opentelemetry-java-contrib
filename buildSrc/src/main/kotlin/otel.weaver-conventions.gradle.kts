import io.opentelemetry.gradle.WeaverTasks
import org.gradle.api.GradleException
import org.gradle.api.provider.Property

// Weaver code generation convention plugin for OpenTelemetry
// Apply this plugin to modules that have a model/ directory with weaver model files
// It will generate Java code, documentation, and YAML configs using the OpenTelemetry Weaver tool

val weaverContainer =
  "otel/weaver:v0.18.0@sha256:5425ade81dc22ddd840902b0638b4b6a9186fb654c5b50c1d1ccd31299437390"

// Auto-detect platform for Docker, with fallback to x86_64
val dockerPlatform = System.getProperty("os.arch").let { arch ->
  when {
    arch == "aarch64" || arch == "arm64" -> "linux/arm64"
    else -> "linux/x86_64"
  }
}

interface OtelWeaverExtension {
  /**
   * REQUIRED: The Java package path where generated code will be placed. Path should use forward
   * slashes (e.g., "io/opentelemetry/ibm/mq/metrics").
   *
   * Example configuration in build.gradle.kts:
   * ```kotlin
   * otelWeaver {
   *   javaOutputPackage.set("io/opentelemetry/ibm/mq/metrics")
   * }
   * ```
   */
  val javaOutputPackage: Property<String>
}

val weaverExtension = extensions.create("otelWeaver", OtelWeaverExtension::class.java)

val projectModelDir = layout.projectDirectory.dir("model")
val hasWeaverModel = projectModelDir.asFile.exists() && projectModelDir.asFile.isDirectory

if (hasWeaverModel) {
  val projectTemplatesDir = layout.projectDirectory.dir("templates")
  val projectDocsDir = layout.projectDirectory.dir("docs")

  logger.lifecycle("Weaver model found in ${project.name}")
  logger.lifecycle("  Model directory: ${projectModelDir.asFile.absolutePath}")
  logger.lifecycle("  Templates directory: ${projectTemplatesDir.asFile.absolutePath}")
  logger.lifecycle("  Container: $weaverContainer")

  tasks.register<WeaverTasks>("weaverCheck") {
    group = "weaver"
    description = "Check the weaver model for errors"

    dockerExecutable.set("docker")
    platform.set(dockerPlatform)
    image.set(weaverContainer)

    modelDir.set(projectModelDir)
    templatesDir.set(projectTemplatesDir)
    outputDir.set(layout.buildDirectory.dir("weaver-check"))

    toolArgs.set(listOf("registry", "check", "--registry=/home/weaver/model"))

    // Always run check task to ensure model validity, even if inputs haven't changed.
    // This is intentional as validation should always run when explicitly requested.
    outputs.upToDateWhen { false }
  }

  tasks.register<WeaverTasks>("weaverGenerateDocs") {
    group = "weaver"
    description = "Generate markdown documentation from weaver model"

    dockerExecutable.set("docker")
    platform.set(dockerPlatform)
    image.set(weaverContainer)

    modelDir.set(projectModelDir)
    templatesDir.set(projectTemplatesDir)
    outputDir.set(projectDocsDir)

    toolArgs.set(
      listOf(
        "registry",
        "generate",
        "--registry=/home/weaver/model",
        "markdown",
        "--future",
        "/home/weaver/target"
      )
    )
  }

  val weaverGenerateJavaTask =
    tasks.register<WeaverTasks>("weaverGenerateJava") {
      group = "weaver"
      description = "Generate Java code from weaver model"

      dockerExecutable.set("docker")
      platform.set(dockerPlatform)
      image.set(weaverContainer)

      modelDir.set(projectModelDir)
      templatesDir.set(projectTemplatesDir)

      // Map the javaOutputPackage to the output directory
      // Finalize the value to ensure it's set at configuration time and avoid capturing the extension
      val javaPackage = weaverExtension.javaOutputPackage
      javaPackage.finalizeValueOnRead()
      outputDir.set(javaPackage.map { layout.projectDirectory.dir("src/main/java/$it") })

      toolArgs.set(
        listOf(
          "registry",
          "generate",
          "--registry=/home/weaver/model",
          "java",
          "--future",
          "/home/weaver/target"
        )
      )

      doFirst { logger.lifecycle("  Java output: ${outputDir.get().asFile.absolutePath}") }
    }

  // Validate the required configuration at configuration time (not execution time)
  afterEvaluate {
    if (weaverExtension.javaOutputPackage.orNull == null) {
      throw GradleException(
        """
        otelWeaver.javaOutputPackage must be configured in project '${project.name}'.

        Add this to your build.gradle.kts:
        otelWeaver {
          javaOutputPackage.set("io/opentelemetry/your/package")
        }
        """.trimIndent()
      )
    }
  }

  // Make spotless tasks depend on weaver generation
  tasks
    .matching {
      it.name == "spotlessJava" || it.name == "spotlessJavaApply" || it.name == "spotlessApply"
    }
    .configureEach { mustRunAfter(weaverGenerateJavaTask) }

  // Make weaverGenerateJava automatically format generated code
  weaverGenerateJavaTask.configure { finalizedBy("spotlessJavaApply") }

  tasks.register<WeaverTasks>("weaverGenerateYaml") {
    group = "weaver"
    description = "Generate YAML configuration from weaver model"

    dockerExecutable.set("docker")
    platform.set(dockerPlatform)
    image.set(weaverContainer)

    modelDir.set(projectModelDir)
    templatesDir.set(projectTemplatesDir)
    outputFile.set(layout.projectDirectory.file("config.yml"))

    toolArgs.set(
      listOf(
        "registry",
        "generate",
        "--registry=/home/weaver/model",
        "yaml",
        "--future",
        "/home/weaver/target"
      )
    )
  }

  tasks.register("weaverGenerate") {
    description = "Generate all outputs (Java, docs, YAML) from weaver model"
    group = "weaver"
    dependsOn("weaverGenerateJava", "weaverGenerateDocs", "weaverGenerateYaml")
  }

  tasks.named("compileJava") { dependsOn("weaverGenerateJava") }
} else {
  logger.debug(
    "No weaver model directory found in ${project.name}, skipping weaver task registration"
  )
}
