package io.opentelemetry.gradle

import java.io.IOException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Docker run is external and side-effectful")
abstract class WeaverTasks @Inject constructor(
  private val execOps: ExecOperations
) : DefaultTask() {

  companion object {
    private const val WEAVER_MODEL_PATH = "/home/weaver/model"
    private const val WEAVER_TEMPLATES_PATH = "/home/weaver/templates"
    private const val WEAVER_TARGET_PATH = "/home/weaver/target"
  }

  @get:Input
  abstract val dockerExecutable: Property<String>
  @get:Input
  abstract val platform: Property<String>
  @get:Input
  abstract val image: Property<String>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val modelDir: DirectoryProperty

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val templatesDir: DirectoryProperty

  // Choose ONE of these per task
  @get:OutputDirectory
  @get:Optional
  abstract val outputDir: DirectoryProperty
  @get:OutputFile
  @get:Optional
  abstract val outputFile: RegularFileProperty

  // e.g., ["registry","check","--registry=/home/weaver/model"]
  @get:Input
  abstract val toolArgs: ListProperty<String>

  @TaskAction
  fun runWeaver() {
    // Validate Docker is available
    validateDockerAvailable()

    val mounts = mutableListOf(
      "--mount", "type=bind,source=${modelDir.get().asFile.absolutePath},target=$WEAVER_MODEL_PATH,readonly"
    )

    val templates = templatesDir.orNull
    if (templates != null) {
      when {
        templates.asFile.isDirectory -> {
          mounts += listOf("--mount", "type=bind,source=${templates.asFile.absolutePath},target=$WEAVER_TEMPLATES_PATH,readonly")
        }
        templates.asFile.exists() -> {
          logger.warn("templatesDir exists but is not a directory: ${templates.asFile.absolutePath}. Skipping templates mount.")
        }
      }
    }

    val targetMount = when {
      outputDir.isPresent -> {
        outputDir.get().asFile.mkdirs()
        listOf("--mount", "type=bind,source=${outputDir.get().asFile.absolutePath},target=$WEAVER_TARGET_PATH")
      }

      outputFile.isPresent -> {
        // Mount parent directory and ensure weaver writes to the correct filename
        val outputFileObj = outputFile.get().asFile
        val parent = outputFileObj.parentFile.also { it.mkdirs() }
        logger.info("Mounting ${parent.absolutePath} for output file: ${outputFileObj.name}")
        listOf("--mount", "type=bind,source=${parent.absolutePath},target=$WEAVER_TARGET_PATH")
      }

      else -> error("Either outputDir or outputFile must be set")
    }
    mounts += targetMount

    val base = mutableListOf("run", "--rm", "--platform=${platform.get()}")
    val os = System.getProperty("os.name").lowercase()
    if (os.contains("linux")) {
      try {
        val uid = ProcessBuilder("id", "-u").start().inputStream.bufferedReader().readText().trim()
        val gid = ProcessBuilder("id", "-g").start().inputStream.bufferedReader().readText().trim()
        base += listOf("-u", "$uid:$gid")
      } catch (e: IOException) {
        logger.warn("Could not determine uid/gid: ${e.message}. Generated files may be owned by root")
      }
    }

    execOps.exec {
      executable = dockerExecutable.get()
      args = base + mounts + listOf(image.get()) + toolArgs.get()
      standardOutput = System.out
      errorOutput = System.err
      isIgnoreExitValue = false
    }
  }

  private fun validateDockerAvailable() {
    try {
      val process = ProcessBuilder(dockerExecutable.get(), "--version")
        .redirectErrorStream(true)
        .start()
      val exitCode = process.waitFor()
      if (exitCode != 0) {
        throw GradleException("Docker is not available or not functioning correctly. Please ensure Docker is installed and running.")
      }
    } catch (e: IOException) {
      throw GradleException("Docker is required but could not be executed. Please install and start Docker. Error: ${e.message}", e)
    }
  }
}

