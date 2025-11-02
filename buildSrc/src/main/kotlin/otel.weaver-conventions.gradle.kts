import io.opentelemetry.gradle.OtelJavaExtension
import org.gradle.api.provider.Property
import java.io.File

// Weaver code generation convention plugin for OpenTelemetry
// Apply this plugin to modules that have a model/ directory with weaver model files
// It will generate Java code, documentation, and YAML configs using the OpenTelemetry Weaver tool

/**
 * Extension for configuring Weaver code generation.
 */
interface OtelWeaverExtension {
  /**
   * The Java package path where generated code will be placed.
   * Path should use forward slashes (e.g., "io/opentelemetry/contrib/mymodule/metrics").
   *
   * Defaults to deriving the path from otelJava.moduleName by:
   * 1. Removing "io.opentelemetry.contrib." prefix
   * 2. Converting dots and hyphens to forward slashes
   * 3. Prepending "io/opentelemetry/"
   *
   * Example: "io.opentelemetry.contrib.ibm-mq-metrics" → "io/opentelemetry/ibm/mq/metrics"
   */
  val javaOutputPackage: Property<String>
}

/**
 * Derives a Java package path from the otelJava.moduleName.
 * Converts module name format to filesystem path format.
 */
fun derivePackagePathFromModuleName(): String {
  val otelJava = extensions.findByType(OtelJavaExtension::class.java)
  if (otelJava != null && otelJava.moduleName.isPresent) {
    val moduleName = otelJava.moduleName.get()

    // Remove common prefix and convert to path
    return moduleName
      .removePrefix("io.opentelemetry.contrib.")
      .removePrefix("io.opentelemetry.")
      .replace('.', '/')
      .replace('-', '/')
      .let { "io/opentelemetry/$it" }
  }

  // Fallback if otelJava extension not found
  return "io/opentelemetry/metrics"
}

fun configureWeaverTask(task: Exec) {
  task.group = "weaver"
  task.standardOutput = System.out
  task.errorOutput = System.err
  task.executable = "docker"
}

// Create the weaver extension for configuration
val weaverExtension = extensions.create("otelWeaver", OtelWeaverExtension::class.java)

// Check if this module has a model directory
val modelDir = File(project.projectDir, "model")
val hasWeaverModel = modelDir.exists() && modelDir.isDirectory

if (hasWeaverModel) {
  val templatesDir = File(project.projectDir, "templates")
  val docsDir = File(project.projectDir, "docs")
  val weaverContainer = "otel/weaver:v0.18.0@sha256:5425ade81dc22ddd840902b0638b4b6a9186fb654c5b50c1d1ccd31299437390"

  // Configure default after project is evaluated so otelJava.moduleName is available
  afterEvaluate {
    if (!weaverExtension.javaOutputPackage.isPresent) {
      weaverExtension.javaOutputPackage.set(derivePackagePathFromModuleName())
    }
  }

  logger.lifecycle("Weaver model found in ${project.name}")
  logger.lifecycle("  Model directory: ${modelDir.absolutePath}")
  logger.lifecycle("  Templates directory: ${templatesDir.absolutePath}")
  logger.lifecycle("  Container: $weaverContainer")


  tasks.register<Exec>("weaverCheck") {
    configureWeaverTask(this)
    description = "Check the weaver model for errors"

    val modelDirPath = modelDir.absolutePath
    val templatesDirPath = templatesDir.absolutePath
    val hasTemplates = templatesDir.exists() && templatesDir.isDirectory

    inputs.dir(modelDir)
    if (hasTemplates) {
      inputs.dir(templatesDir)
    }

    val tempDir = layout.buildDirectory.dir("weaver-check").get().asFile

    doFirst {
      tempDir.mkdirs()

      val mountArgs = mutableListOf(
        "--mount", "type=bind,source=$modelDirPath,target=/home/weaver/model,readonly"
      )
      if (hasTemplates) {
        mountArgs.addAll(listOf(
          "--mount", "type=bind,source=$templatesDirPath,target=/home/weaver/templates,readonly"
        ))
      }
      mountArgs.addAll(listOf(
        "--mount", "type=bind,source=${tempDir.absolutePath},target=/home/weaver/target"
      ))

      args = listOf("run", "--rm", "--platform=linux/x86_64") +
        mountArgs +
        listOf(weaverContainer, "registry", "check", "--registry=/home/weaver/model")
    }
  }

  tasks.register<Exec>("weaverGenerateDocs") {
    configureWeaverTask(this)
    description = "Generate markdown documentation from weaver model"

    val modelDirPath = modelDir.absolutePath
    val templatesDirPath = templatesDir.absolutePath
    val docsDirPath = docsDir.absolutePath
    val hasTemplates = templatesDir.exists() && templatesDir.isDirectory

    inputs.dir(modelDir)
    if (hasTemplates) {
      inputs.dir(templatesDir)
    }
    outputs.dir(docsDir)

    doFirst {
      docsDir.mkdirs()

      val mountArgs = mutableListOf(
        "--mount", "type=bind,source=$modelDirPath,target=/home/weaver/model,readonly"
      )
      if (hasTemplates) {
        mountArgs.addAll(listOf(
          "--mount", "type=bind,source=$templatesDirPath,target=/home/weaver/templates,readonly"
        ))
      }
      mountArgs.addAll(listOf(
        "--mount", "type=bind,source=$docsDirPath,target=/home/weaver/target"
      ))

      args = listOf("run", "--rm", "--platform=linux/x86_64") +
        mountArgs +
        listOf(weaverContainer, "registry", "generate", "--registry=/home/weaver/model", "markdown", "--future", "/home/weaver/target")
    }
  }

  val weaverGenerateJavaTask = tasks.register<Exec>("weaverGenerateJava") {
    configureWeaverTask(this)
    description = "Generate Java code from weaver model"

    val modelDirPath = modelDir.absolutePath
    val templatesDirPath = templatesDir.absolutePath
    val hasTemplates = templatesDir.exists() && templatesDir.isDirectory

    inputs.dir(modelDir)
    if (hasTemplates) {
      inputs.dir(templatesDir)
    }

    // Register outputs lazily using provider
    val javaOutputDirProvider = weaverExtension.javaOutputPackage.map {
      File(project.projectDir, "src/main/java/$it")
    }
    outputs.dir(javaOutputDirProvider)

    doFirst {
      val outputDir = javaOutputDirProvider.get()
      logger.lifecycle("  Java output: ${outputDir.absolutePath}")
      outputDir.mkdirs()

      val mountArgs = mutableListOf(
        "--mount", "type=bind,source=$modelDirPath,target=/home/weaver/model,readonly"
      )
      if (hasTemplates) {
        mountArgs.addAll(listOf(
          "--mount", "type=bind,source=$templatesDirPath,target=/home/weaver/templates,readonly"
        ))
      }
      mountArgs.addAll(listOf(
        "--mount", "type=bind,source=${outputDir.absolutePath},target=/home/weaver/target"
      ))

      // Build base docker run command
      val baseDockerArgs = mutableListOf("run", "--rm", "--platform=linux/x86_64")

      // Add user mapping for Linux (not needed on macOS with Docker Desktop)
      val os = System.getProperty("os.name").lowercase()
      if (os.contains("linux")) {
        try {
          val userId = ProcessBuilder("id", "-u").start().inputStream.bufferedReader().readText().trim()
          val groupId = ProcessBuilder("id", "-g").start().inputStream.bufferedReader().readText().trim()
          baseDockerArgs.addAll(listOf("-u", "$userId:$groupId"))
        } catch (e: Exception) {
          logger.warn("Failed to get user/group ID, generated files may be owned by root: ${e.message}")
        }
      }

      args = baseDockerArgs + mountArgs +
        listOf(weaverContainer, "registry", "generate", "--registry=/home/weaver/model", "java", "--future", "/home/weaver/target")
    }
  }

  // Make spotless tasks depend on weaver generation
  tasks.matching { it.name == "spotlessJava" || it.name == "spotlessJavaApply" || it.name == "spotlessApply" }.configureEach {
    mustRunAfter(weaverGenerateJavaTask)
  }

  // Make weaverGenerateJava automatically format generated code
  weaverGenerateJavaTask.configure {
    finalizedBy("spotlessJavaApply")
  }

  tasks.register<Exec>("weaverGenerateYaml") {
    configureWeaverTask(this)
    description = "Generate YAML configuration from weaver model"

    val modelDirPath = modelDir.absolutePath
    val templatesDirPath = templatesDir.absolutePath
    val projectDirPath = project.projectDir.absolutePath
    val hasTemplates = templatesDir.exists() && templatesDir.isDirectory

    inputs.dir(modelDir)
    if (hasTemplates) {
      inputs.dir(templatesDir)
    }
    outputs.file(File(project.projectDir, "config.yml"))

    doFirst {
      val mountArgs = mutableListOf(
        "--mount", "type=bind,source=$modelDirPath,target=/home/weaver/model,readonly"
      )
      if (hasTemplates) {
        mountArgs.addAll(listOf(
          "--mount", "type=bind,source=$templatesDirPath,target=/home/weaver/templates,readonly"
        ))
      }
      mountArgs.addAll(listOf(
        "--mount", "type=bind,source=$projectDirPath,target=/home/weaver/target"
      ))

      args = listOf("run", "--rm", "--platform=linux/x86_64") +
        mountArgs +
        listOf(weaverContainer, "registry", "generate", "--registry=/home/weaver/model", "yaml", "--future", "/home/weaver/target")
    }
  }

  tasks.register("weaverGenerate") {
    description = "Generate all outputs (Java, docs, YAML) from weaver model"
    group = "weaver"
    dependsOn("weaverGenerateJava", "weaverGenerateDocs", "weaverGenerateYaml")
  }

  tasks.named("compileJava") {
    dependsOn("weaverGenerateJava")
  }
} else {
  logger.debug("No weaver model directory found in ${project.name}, skipping weaver task registration")
}
