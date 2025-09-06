import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("otel.animalsniffer-conventions")
  id("com.squareup.wire") version "5.3.11"
}

description = "Client implementation of the OpAMP spec."
otelJava.moduleName.set("io.opentelemetry.contrib.opamp.client")

dependencies {
  implementation("com.squareup.okhttp3:okhttp")
  implementation("com.github.f4b6a3:uuid-creator")
  implementation("io.opentelemetry:opentelemetry-api")
  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("com.google.protobuf:protobuf-java-util")
  testImplementation("com.squareup.okhttp3:mockwebserver3")
  testImplementation("com.squareup.okhttp3:mockwebserver3-junit5")
}

val opampProtos = tasks.register<DownloadAndExtractOpampProtos>("opampProtoDownload") {
  group = "opamp"
  outputProtosDir.set(project.layout.buildDirectory.dir("opamp/protos"))
  downloadedZipFile.set(project.layout.buildDirectory.file("intermediate/opampProtoDownload/release.zip"))
  zipUrl.set("https://github.com/open-telemetry/opamp-spec/zipball/v0.14.0")
}

wire {
  java {}
  sourcePath {
    srcDir(opampProtos)
  }
}

abstract class DownloadAndExtractOpampProtos @Inject constructor(
  private val archiveOps: ArchiveOperations,
  private val fileOps: FileSystemOperations,
) : DefaultTask() {

  @get:OutputDirectory
  abstract val outputProtosDir: DirectoryProperty

  @get:Internal
  abstract val downloadedZipFile: RegularFileProperty

  @get:Input
  abstract val zipUrl: Property<String>

  @TaskAction
  fun execute() {
    val url = URL(zipUrl.get())
    downloadedZipFile.get().asFile.parentFile.mkdirs()

    url.openStream().use { input: InputStream ->
      downloadedZipFile.get().asFile.outputStream().use { output: FileOutputStream ->
        input.copyTo(output)
      }
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
