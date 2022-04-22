import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("otel.java-conventions")
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("org.unbroken-dome.test-sets") version "4.0.0"
}
// TODO: update readme and PR!
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

  bootstrapLibs(project(":static-instrumenter:common"))
  javaagentLibs(project(":static-instrumenter:agent-extension"))
}

testSets {
  create("integrationTest")
}

tasks {

  val relocateJavaagentLibs by registering(ShadowJar::class) {
    configurations = listOf(javaagentLibs)
    duplicatesStrategy = DuplicatesStrategy.FAIL
    archiveFileName.set("javaagentLibs-relocated.jar")
  }

  val isolateJavaagentLibs by registering(Copy::class) {
    dependsOn(relocateJavaagentLibs)
    isolateClasses(relocateJavaagentLibs.get().outputs.files)
    into("$buildDir/isolated/javaagentLibs")
  }

  shadowJar {
    configurations = listOf(javaagent, bootstrapLibs)

    dependsOn(isolateJavaagentLibs)
    from(isolateJavaagentLibs.get().outputs)
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

  val mainShadowJar by registering(Jar::class) {
    archiveFileName.set("opentelemetry-static-agent.jar")
    from(zipTree(shadowJar.get().archiveFile))

    manifest {
      attributes(shadowJar.get().manifest.attributes)
    }
  }

  // TODO: the resulting jar contains both inst and unpacked inst,
  //  but we need only the unpacked inst here
  val createNoInstAgent by registering(ShadowJar::class) {

    configurations = listOf(javaagent)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

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
    archiveFileName.set("opentelemetry-no-inst-agent.jar")
  }

  assemble {
    dependsOn(shadowJar, mainShadowJar, createNoInstAgent)
  }

  val copyJarsToTestRes by registering(Copy::class) {
    dependsOn(assemble)
    from("$buildDir/libs/opentelemetry-no-inst-agent.jar")
    from("$buildDir/libs/opentelemetry-static-agent.jar")
    destinationDir = File("src/integrationTest/resources")
  }

  val integrationTest by existing {
    dependsOn(copyJarsToTestRes)
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
  relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation")
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
  }
}
