plugins {
  application
  id("com.gradleup.shadow")
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("otel.weaver-conventions")
}

description = "IBM-MQ metrics"
otelJava.moduleName.set("io.opentelemetry.contrib.ibm-mq-metrics")
application.mainClass.set("io.opentelemetry.ibm.mq.opentelemetry.Main")

otelWeaver {
  javaOutputPackage.set("io/opentelemetry/ibm/mq/metrics")
}

val ibmClientJar = configurations.create("ibmClientJar") {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  api("com.google.auto.value:auto-value-annotations:1.11.1")
  api("com.google.code.findbugs:jsr305:3.0.2")
  api("io.swagger:swagger-annotations:1.6.16")
  api("org.jetbrains:annotations:26.1.0")
  api("com.ibm.mq:com.ibm.mq.allclient:10.0.1.0")
  api("org.snakeyaml:snakeyaml-engine:2.10")
  api("com.fasterxml.jackson.core:jackson-databind")
  api("io.opentelemetry:opentelemetry-sdk")
  api("io.opentelemetry:opentelemetry-exporter-otlp")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  api("org.slf4j:slf4j-api:2.0.20")
  implementation("org.slf4j:slf4j-simple:2.0.20")
  testImplementation("com.google.guava:guava")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  annotationProcessor("com.google.auto.value:auto-value:1.11.1")
  ibmClientJar("com.ibm.mq:com.ibm.mq.allclient:10.0.1.0") {
    artifact {
      name = "com.ibm.mq.allclient"
      extension = "jar"
    }
    isTransitive = false
  }
}

testing {
  suites {
    register<JvmTestSuite>("integrationTest") {
      dependencies {
        implementation("org.assertj:assertj-core")
        implementation("io.opentelemetry:opentelemetry-sdk-testing")
        implementation("com.ibm.mq:com.ibm.mq.jakarta.client:10.0.1.0")
        implementation("jakarta.jms:jakarta.jms-api:3.1.0")
      }

      targets {
        all {
          testTask.configure {
            // Jakarta JMS requires Java 11+
            val testJavaVersion = project.findProperty("testJavaVersion") as String?
            if (testJavaVersion == "8") {
              enabled = false
            }
          }
        }
      }
    }
  }
}

tasks.shadowJar {
  // avoid warning about duplicate kotlin module files being silently dropped
  filesMatching("META-INF/*.kotlin_module") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
  dependencies {
    exclude(dependency("com.ibm.mq:com.ibm.mq.allclient"))
  }
}
