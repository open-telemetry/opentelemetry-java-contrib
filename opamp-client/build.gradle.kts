import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.DownloadExtension
import groovy.json.JsonSlurper

plugins {
  id("otel.java-conventions")
  id("de.undercouch.download") version "5.6.0"
  id("com.squareup.wire") version "5.3.1"
}

description = "Client implementation of the OpAMP spec."
otelJava.moduleName.set("io.opentelemetry.contrib.opamp.client")

dependencies {
  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")
}

val opampReleaseInfo = tasks.register<Download>("opampLastReleaseInfo") {
  group = "opamp"
  src("https://api.github.com/repos/open-telemetry/opamp-spec/releases/latest")
  val token = System.getenv("GH_TOKEN")
  if (token.isNullOrBlank()) {
    logger.warn("No GitHub token found in environment variable GH_TOKEN. Rate limits may apply.")
  } else {
    header("Authorization", "Bearer $token")
    header("X-GitHub-Api-Version", "2022-11-28")
  }
  dest(project.layout.buildDirectory.file("opamp/release.json"))
}

val opampProtos = tasks.register<DownloadOpampProtos>("opampProtoDownload", download)
opampProtos.configure {
  group = "opamp"
  dependsOn(opampReleaseInfo)
  lastReleaseInfoJson.set {
    opampReleaseInfo.get().dest
  }
  outputProtosDir.set(project.layout.buildDirectory.dir("opamp/protos"))
  downloadedZipFile.set(project.layout.buildDirectory.file("intermediate/$name/release.zip"))
}

wire {
  java {}
  sourcePath {
    srcDir(opampProtos)
  }
}

abstract class DownloadOpampProtos @Inject constructor(
  private val download: DownloadExtension,
  private val archiveOps: ArchiveOperations,
  private val fileOps: FileSystemOperations,
) : DefaultTask() {

  @get:InputFile
  abstract val lastReleaseInfoJson: RegularFileProperty

  @get:OutputDirectory
  abstract val outputProtosDir: DirectoryProperty

  @get:Internal
  abstract val downloadedZipFile: RegularFileProperty

  @Suppress("UNCHECKED_CAST")
  @TaskAction
  fun execute() {
    val releaseInfo = JsonSlurper().parse(lastReleaseInfoJson.get().asFile) as Map<String, String>
    val zipUrl = releaseInfo["zipball_url"]
    download.run {
      src(zipUrl)
      dest(downloadedZipFile)
    }
    val protos = archiveOps.zipTree(downloadedZipFile).matching {
      setIncludes(listOf("**/*.proto"))
    }
    fileOps.sync {
      from(protos.files)
      into(outputProtosDir)
    }
  }
}
