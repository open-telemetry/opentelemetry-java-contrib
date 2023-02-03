import io.opentelemetry.gradle.OtelJavaExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  `java-library`
  jacoco

  id("otel.errorprone-conventions")
  id("otel.spotless-conventions")
}

val otelJava = extensions.create<OtelJavaExtension>("otelJava")

group = "io.opentelemetry.contrib"

base.archivesName.set("opentelemetry-${project.name}")

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }

  withJavadocJar()
  withSourcesJar()
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(8)

      if (name != "jmhCompileGeneratedClasses") {
        compilerArgs.addAll(
          listOf(
            "-Xlint:all",
            // We suppress the "try" warning because it disallows managing an auto-closeable with
            // try-with-resources without referencing the auto-closeable within the try block.
            "-Xlint:-try",
            // We suppress the "processing" warning as suggested in
            // https://groups.google.com/forum/#!topic/bazel-discuss/_R3A9TJSoPM
            "-Xlint:-processing",
            // We suppress the "options" warning because it prevents compilation on modern JDKs
            "-Xlint:-options",

            // Fail build on any warning
            "-Werror",
          ),
        )
      }

      encoding = "UTF-8"

      if (name.contains("Test")) {
        // serialVersionUI is basically guaranteed to be useless in tests
        compilerArgs.add("-Xlint:-serial")
      }
    }
  }

  withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
      showExceptions = true
      showCauses = true
      showStackTraces = true
    }
  }

  withType<Javadoc>().configureEach {
    exclude("io/opentelemetry/**/internal/**")

    with(options as StandardJavadocDocletOptions) {
      source = "8"
      encoding = "UTF-8"
      docEncoding = "UTF-8"
      breakIterator(true)

      addBooleanOption("html5", true)

      // TODO (trask) revisit to see if url is fixed
      // currently broken because https://docs.oracle.com/javase/8/docs/api/element-list is missing
      // and redirects
      // links("https://docs.oracle.com/javase/8/docs/api/")

      addBooleanOption("Xdoclint:all,-missing", true)
    }
  }
}

// Add version information to published artifacts.
plugins.withId("otel.publish-conventions") {
  tasks {
    register("generateVersionResource") {
      val moduleName = otelJava.moduleName
      val propertiesDir = moduleName.map { File(buildDir, "generated/properties/${it.replace('.', '/')}") }

      inputs.property("project.version", project.version.toString())
      outputs.dir(propertiesDir)

      doLast {
        File(propertiesDir.get(), "version.properties").writeText("contrib.version=${project.version}")
      }
    }
  }

  sourceSets {
    main {
      output.dir("$buildDir/generated/properties", "builtBy" to "generateVersionResource")
    }
  }
}

val dependencyManagement by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = false
  isVisible = false
}

dependencies {
  dependencyManagement(platform(project(":dependencyManagement")))
  afterEvaluate {
    configurations.configureEach {
      if (isCanBeResolved && !isCanBeConsumed) {
        extendsFrom(dependencyManagement)
      }
    }
  }

  compileOnly("com.google.code.findbugs:jsr305")
  compileOnly("com.google.errorprone:error_prone_annotations")
}

testing {
  suites.withType(JvmTestSuite::class).configureEach {
    dependencies {
      implementation(project(project.path))

      compileOnly("com.google.auto.value:auto-value-annotations")
      compileOnly("com.google.errorprone:error_prone_annotations")
      compileOnly("com.google.code.findbugs:jsr305")

      implementation("org.junit.jupiter:junit-jupiter-api")
      implementation("org.junit.jupiter:junit-jupiter-params")
      implementation("org.mockito:mockito-core")
      implementation("org.mockito:mockito-junit-jupiter")
      implementation("org.assertj:assertj-core")
      implementation("org.awaitility:awaitility")
      implementation("io.github.netmikey.logunit:logunit-jul")

      runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
    }
  }
}
