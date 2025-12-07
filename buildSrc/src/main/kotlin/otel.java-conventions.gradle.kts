import io.opentelemetry.gradle.OtelJavaExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  `java-library`
  jacoco

  id("otel.errorprone-conventions")
  id("otel.spotless-conventions")
  id("otel.japicmp-conventions")
  id("org.owasp.dependencycheck")
}

val otelJava = extensions.create<OtelJavaExtension>("otelJava")

group = "io.opentelemetry.contrib"

base.archivesName.set("opentelemetry-${project.name}")

// Version to use to compile code and run tests.
val DEFAULT_JAVA_VERSION = JavaVersion.VERSION_21

java {
  toolchain {
    languageVersion.set(
        otelJava.minJavaVersionSupported.map { JavaLanguageVersion.of(Math.max(it.majorVersion.toInt(), DEFAULT_JAVA_VERSION.majorVersion.toInt())) }
    )
  }

  withJavadocJar()
  withSourcesJar()
}

tasks.named<Jar>("jar") {
  from(rootProject.file("LICENSE")) {
    into("META-INF")
  }
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(otelJava.minJavaVersionSupported.map { it.majorVersion.toInt() })

      if (name!="jmhCompileGeneratedClasses") {
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
        compilerArgs.add("-Xlint:-this-escape")
      }
    }
  }

  withType<Test>().configureEach {
    useJUnitPlatform()

    val maxTestRetries = gradle.startParameter.projectProperties["maxTestRetries"]?.toInt() ?: 0
    develocity.testRetry {
      // You can see tests that were retried by this mechanism in the collected test reports and build scans.
      maxRetries.set(maxTestRetries)
    }

    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
      showStandardStreams = true
    }

    configure<JacocoTaskExtension> {
      // only care about code coverage for code in this repository
      // (in particular avoiding netty classes which sometimes end up
      // causing sporadic CI failures)
      includes = listOf("io/opentelemetry/contrib/**")
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
      addBooleanOption("Xdoclint:all,-missing", true)
    }
  }
}

// Add version information to published artifacts.
plugins.withId("otel.publish-conventions") {
  tasks {
    register("generateVersionResource") {
      val moduleName = otelJava.moduleName
      val propertiesDir = moduleName.map { layout.buildDirectory.file("generated/properties/${it.replace('.', '/')}") }
      val projectVersion = project.version.toString()

      inputs.property("project.version", projectVersion)
      outputs.dir(propertiesDir)

      doLast {
        File(propertiesDir.get().get().asFile, "version.properties").writeText("contrib.version=${projectVersion}")
      }
    }
  }

  sourceSets {
    main {
      output.dir(layout.buildDirectory.dir("generated/properties"), "builtBy" to "generateVersionResource")
    }
  }
}

val dependencyManagement by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = false
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
    useJUnitJupiter("5.14.1")

    dependencies {
      implementation(project())

      implementation(enforcedPlatform("org.testcontainers:testcontainers-bom:2.0.2"))
      implementation(enforcedPlatform("com.google.guava:guava-bom:33.5.0-jre"))
      implementation(enforcedPlatform("com.linecorp.armeria:armeria-bom:1.34.0"))

      compileOnly("com.google.auto.value:auto-value-annotations")
      compileOnly("com.google.errorprone:error_prone_annotations")
      compileOnly("com.google.code.findbugs:jsr305")

      implementation("org.mockito:mockito-core")
      implementation("org.mockito:mockito-junit-jupiter")
      implementation("org.assertj:assertj-core")
      implementation("org.awaitility:awaitility")
      implementation("io.github.netmikey.logunit:logunit-jul")
    }
  }
}

tasks {
  check {
    dependsOn(testing.suites)
  }
}

fun isJavaVersionAllowed(version: JavaVersion): Boolean {
  if (otelJava.minJavaVersionSupported.get() > version) {
    return false
  }
  if (otelJava.maxJavaVersionForTests.isPresent && otelJava.maxJavaVersionForTests.get().compareTo(version) < 0) {
    return false
  }
  return true
}

afterEvaluate {
  val testJavaVersion = gradle.startParameter.projectProperties["testJavaVersion"]?.let(JavaVersion::toVersion)
  val useJ9 = gradle.startParameter.projectProperties["testJavaVM"]?.run { this=="openj9" }
    ?: false
  tasks.withType<Test>().configureEach {
    if (testJavaVersion!=null) {
      javaLauncher.set(
        javaToolchains.launcherFor {
          languageVersion.set(JavaLanguageVersion.of(testJavaVersion.majorVersion))
          implementation.set(if (useJ9) JvmImplementation.J9 else JvmImplementation.VENDOR_SPECIFIC)
        }
      )
      isEnabled = isEnabled && isJavaVersionAllowed(testJavaVersion)
    } else {
      // We default to testing with Java 11 for most tests, but some tests don't support it, where we change
      // the default test task's version so commands like `./gradlew check` can test all projects regardless
      // of Java version.
      if (!isJavaVersionAllowed(DEFAULT_JAVA_VERSION) && otelJava.maxJavaVersionForTests.isPresent) {
        javaLauncher.set(
          javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(otelJava.maxJavaVersionForTests.get().majorVersion))
          }
        )
      }
    }
  }
}

dependencyCheck {
  scanConfigurations = mutableListOf("runtimeClasspath")
  suppressionFile = "buildscripts/dependency-check-suppressions.xml"
  failBuildOnCVSS = 7.0f // fail on high or critical CVE
  nvd.apiKey = System.getenv("NVD_API_KEY")
  nvd.delay = 3500 // until next dependency check release (https://github.com/jeremylong/DependencyCheck/pull/6333)
}
