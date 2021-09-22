import groovy.util.Node
import groovy.util.NodeList

plugins {
  id("java")
  id("com.github.johnrengelman.shadow")
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

// NOTE
// `META-INF/plexus/components.xml` is manually handled under src/main/resources because there is no Gradle
// equivalent to the Maven plugin `plexus-component-metadata:generate-metadata`

description = "Maven extension to observe Maven builds with distributed traces using OpenTelemetry SDK"

dependencies {
  implementation("org.codehaus.plexus:plexus-component-annotations:2.1.0")

  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-sdk-trace")
  implementation("io.opentelemetry:opentelemetry-semconv")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp-trace")

  implementation("io.grpc:grpc-netty-shaded")

  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")

  compileOnly("org.apache.maven:maven-core:3.5.0")
  compileOnly("org.slf4j:slf4j-api")
  compileOnly("org.sonatype.aether:aether-api:1.13.1")


  testImplementation("org.apache.maven:maven-core:3.5.0")
  testImplementation("org.slf4j:slf4j-simple")
}

tasks {
  shadowJar {
    archiveClassifier.set("")
  }

  assemble {
    dependsOn(shadowJar)
  }
  publishing {
    (publications) {
      "maven"(MavenPublication::class) {
            pom {
              withXml {
                val pomNode = asNode()
                val dependenciesNode = (pomNode.get("dependencies") as NodeList)[0] as Node
                val dependencyNodes = dependenciesNode.children() as NodeList
                // the jar dependencies bundled in the uber-jar by the shadow plugin are wrongly added as
                // 'runtime' dependencies in the generated pom.xml instead of being absent this pom.xml.
                // Remove those runtime dependencies from the pom.xml:
                dependencyNodes.removeIf {
                  val dependencyNode = it as Node
                  val scope: String = ((dependencyNode.get("scope") as NodeList)[0] as Node).text()
                  val removeDependency = (scope == "runtime" || scope == "compile")
                  logger.debug("pom.xml remove: $removeDependency dependency $dependencyNode")
                  removeDependency
                }
              }
            }
        }
    }
  }
}

