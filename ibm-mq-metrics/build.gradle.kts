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

val ibmClientJar: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  api("com.google.auto.value:auto-value-annotations:1.11.1")
  api("com.google.code.findbugs:jsr305:3.0.2")
  api("io.swagger:swagger-annotations:1.6.16")
  api("org.jetbrains:annotations:26.1.0")
  api("com.ibm.mq:com.ibm.mq.allclient:10.0.0.0")
  api("org.snakeyaml:snakeyaml-engine:2.10")
  api("com.fasterxml.jackson.core:jackson-databind:2.22.0")
  api("io.opentelemetry:opentelemetry-sdk")
  api("io.opentelemetry:opentelemetry-exporter-otlp")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  api("org.slf4j:slf4j-api:2.0.18")
  implementation("org.slf4j:slf4j-simple:2.0.18")
  testImplementation("com.google.guava:guava")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  annotationProcessor("com.google.auto.value:auto-value:1.11.1")
  ibmClientJar("com.ibm.mq:com.ibm.mq.allclient:10.0.0.0") {
    artifact {
      name = "com.ibm.mq.allclient"
      extension = "jar"
    }
    isTransitive = false
  }
}

testing {
  suites {
    val integrationTest by registering(JvmTestSuite::class) {
      dependencies {
        implementation("org.assertj:assertj-core")
        implementation("io.opentelemetry:opentelemetry-sdk-testing")
        implementation("com.ibm.mq:com.ibm.mq.jakarta.client:9.4.5.1")
        implementation("jakarta.jms:jakarta.jms-api:3.1.0")
      }

      targets {
        all {
          testTask.configure {
            // Jakarta JMS requires Java 11+
            val testJavaVersion: String? by project
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
  dependencies {
    exclude(dependency("com.ibm.mq:com.ibm.mq.allclient"))
  }
}
