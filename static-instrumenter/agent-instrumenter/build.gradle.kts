import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("otel.java-conventions")
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("org.unbroken-dome.test-sets") version "4.0.0"
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

  // TODO: remove local jar once agent with hook is released
  javaagent(files("libs/opentelemetry-javaagent.jar"))
  // javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent")

  bootstrapLibs(project(":static-instrumenter:bootstrap"))
  javaagentLibs(project(":static-instrumenter:agent-extension"))
}

// TODO: migrate to JVM test suite plugin
testSets {
  create("integrationTest")
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

  // TODO: the resulting jar contains both inst and unpacked inst,
  //  but we need only the unpacked inst here
  val createNoInstAgent by registering(ShadowJar::class) {

    configurations = listOf(javaagent)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveClassifier.set("no-inst")

    inputs.files.forEach {
      if (it.name.endsWith(".jar")) {
        from(zipTree(it)) {
          include("inst/")
          eachFile {
            // drop inst/
            relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
            rename("(.*)\\.classdata\$", "\$1.class")
          }
        }
      }
    }
  }

  assemble {
    dependsOn(shadowJar, createNoInstAgent)
  }

  val integrationTest by existing(Test::class) {
    dependsOn(assemble)
    jvmArgumentProviders.add(AgentJarsProvider(shadowJar.flatMap { it.archiveFile }, createNoInstAgent.flatMap { it.archiveFile }))
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

tasks.withType<ShadowJar>().configureEach {
  // we depend on opentelemetry-javaagent-instrumentation-api in agent-extension so we need to relocate its usage
  relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation")
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
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
