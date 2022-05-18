import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("otel.java-conventions")
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("jvm-test-suite")
}

description = "OpenTelemetry Java Static Instrumentation Agent"

val javaagentLibs: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

val bootstrapLibs: Configuration by configurations.creating
configurations.getByName("implementation").extendsFrom(bootstrapLibs)

val javaagent: Configuration by configurations.creating
configurations.getByName("implementation").extendsFrom(javaagent)

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  implementation("org.slf4j:slf4j-api")
  runtimeOnly("org.slf4j:slf4j-simple")

  // TODO: remove snapshot once new agent is released
  javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.14.0-SNAPSHOT")

  bootstrapLibs(project(":static-instrumenter:bootstrap"))
  javaagentLibs(project(":static-instrumenter:agent-extension"))
}

tasks {

  val relocateJavaagentLibs by registering(ShadowJar::class) {
    configurations = listOf(javaagentLibs)
    duplicatesStrategy = DuplicatesStrategy.FAIL
    archiveFileName.set("javaagentLibs-relocated.jar")
  }

  shadowJar {
    configurations = listOf(javaagent, bootstrapLibs)

    dependsOn(relocateJavaagentLibs)
    isolateClasses(relocateJavaagentLibs.get().outputs.files)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
      attributes.put("Main-Class", "io.opentelemetry.contrib.staticinstrumenter.agent.main.Main")
      attributes.put("Agent-Class", "io.opentelemetry.contrib.staticinstrumenter.agent.OpenTelemetryStaticAgent")
      attributes.put("Premain-Class", "io.opentelemetry.contrib.staticinstrumenter.agent.OpenTelemetryStaticAgent")
      attributes.put("Can-Redefine-Classes", "true")
      attributes.put("Can-Retransform-Classes", "true")
      attributes.put("Implementation-Vendor", "OpenTelemetry")
      attributes.put("Implementation-Version", "demo-${project.version}")
    }
  }

  val createNoInstAgent by registering(Jar::class) {
    archiveClassifier.set("no-inst")
    // source jar has no timestamps don't add any to the destination
    isPreserveFileTimestamps = false

    from(zipTree(shadowJar.get().archiveFile)) {
      // renaming inst/ leaves behind empty directories
      includeEmptyDirs = false
      // skip to avoid duplicate entry error
      exclude("inst/META-INF/MANIFEST.MF")
      eachFile {
        if (path.startsWith("inst/")) {
          path = path.substring("inst/".length)
          if (path.endsWith(".classdata")) {
            path = path.substring(0, path.length - 4)
          }
        }
      }
    }

    manifest {
      attributes(shadowJar.get().manifest.attributes)
    }
  }

  withType<ShadowJar>().configureEach {
    // we depend on opentelemetry-instrumentation-api in agent-extension, so we need to relocate its usage
    relocate("io.opentelemetry.instrumentation.api", "io.opentelemetry.javaagent.shaded.instrumentation.api")
  }

  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
  }

  assemble {
    dependsOn(shadowJar, createNoInstAgent)
  }
}

testing {
  suites {
    val integrationTest by registering(JvmTestSuite::class) {
      targets.all {
        testTask.configure {
          jvmArgumentProviders.add(
            AgentJarsProvider(
              tasks.shadowJar.flatMap { it.archiveFile },
              tasks.named<Jar>("createNoInstAgent").flatMap { it.archiveFile }
            )
          )
        }
      }
    }
  }
}

fun CopySpec.isolateClasses(jars: Iterable<File>) {
  jars.forEach {
    from(zipTree(it)) {
      into("inst")
      rename("^(.*)\\.class\$", "\$1.classdata")
      // Rename LICENSE file since it clashes with license dir on non-case sensitive FSs (i.e. Mac)
      rename("""^LICENSE$""", "LICENSE.renamed")
      exclude("META-INF/INDEX.LIST")
      exclude("META-INF/*.DSA")
      exclude("META-INF/*.SF")
    }
  }
}

class AgentJarsProvider(
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val agentJar: Provider<RegularFile>,
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val noInstAgentJar: Provider<RegularFile>
) : CommandLineArgumentProvider {
  override fun asArguments(): Iterable<String> = listOf("-Dagent=${file(agentJar).path}", "-Dno.inst.agent=${file(noInstAgentJar).path}")
}
