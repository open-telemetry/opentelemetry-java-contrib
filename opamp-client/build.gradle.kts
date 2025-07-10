import de.undercouch.gradle.tasks.download.DownloadExtension
import java.net.HttpURLConnection
import java.net.URL

plugins {
  id("otel.java-conventions")
  id("de.undercouch.download") version "5.6.0"
  id("com.squareup.wire") version "5.3.5"
}

description = "Client implementation of the OpAMP spec."
otelJava.moduleName.set("io.opentelemetry.contrib.opamp.client")

dependencies {
  implementation("com.squareup.okhttp3:okhttp")
  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")
  testImplementation("org.mockito:mockito-inline")
}

val opampProtos = tasks.register<DownloadOpampProtos>("opampProtoDownload", download)
opampProtos.configure {
  group = "opamp"
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

  @get:OutputDirectory
  abstract val outputProtosDir: DirectoryProperty

  @get:Internal
  abstract val downloadedZipFile: RegularFileProperty

  @TaskAction
  fun execute() {
    // Get the latest release tag by following the redirect from GitHub's latest release URL
    val latestReleaseUrl = "https://github.com/open-telemetry/opamp-spec/releases/latest"
    val connection = URL(latestReleaseUrl).openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false
    connection.requestMethod = "HEAD"

    val redirectLocation = connection.getHeaderField("Location")
    connection.disconnect()

    // Extract tag from URL like: https://github.com/open-telemetry/opamp-spec/releases/tag/v0.12.0
    val latestTag = redirectLocation.substringAfterLast("/")
    // Download the source code for the latest release
    val zipUrl = "https://github.com/open-telemetry/opamp-spec/zipball/$latestTag"

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
