import de.undercouch.gradle.tasks.download.DownloadExtension

plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("otel.animalsniffer-conventions")
  id("de.undercouch.download") version "5.6.0"
  id("com.squareup.wire") version "5.4.0"
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
    val zipUrl = "https://github.com/open-telemetry/opamp-spec/zipball/v0.14.0"

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
