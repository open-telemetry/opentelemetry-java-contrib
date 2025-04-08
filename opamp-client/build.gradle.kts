import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.DownloadExtension
import groovy.json.JsonSlurper

plugins {
  id("otel.java-conventions")
  id("de.undercouch.download") version "5.6.0"
  id("com.google.protobuf") version "0.9.4"
}

description = "Client implementation of the OpAMP spec."
otelJava.moduleName.set("io.opentelemetry.contrib.opamp.client")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
  main {
    proto {
      srcDir("opamp-spec/proto")
    }
  }
}

tasks.withType<JavaCompile>().configureEach {
  with(options) {
    // Suppressing warnings about the usage of deprecated methods.
    // This is needed because the Protobuf plugin (com.google.protobuf) generates code that uses deprecated methods.
    compilerArgs.add("-Xlint:-deprecation")
  }
}

val protobufVersion = "4.28.2"

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:$protobufVersion"
  }
}

dependencies {
  implementation("com.google.protobuf:protobuf-java:$protobufVersion")
  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")
}

val opampReleaseInfo = tasks.register<Download>("opampLastReleaseInfo") {
  group = "opamp"
  src("https://api.github.com/repos/open-telemetry/opamp-spec/releases/latest")
  dest(project.layout.buildDirectory.file("opamp/release.json"))
}

tasks.register<DownloadOpampProtos>("opampProtoDownload", download).configure {
  group = "opamp"
  dependsOn(opampReleaseInfo)
  lastReleaseInfoJson.set {
    opampReleaseInfo.get().dest
  }
  outputProtosDir.set(project.layout.buildDirectory.dir("opamp/protos"))
  downloadedZipFile.set(project.layout.buildDirectory.file("intermediate/$name/release.zip"))
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
