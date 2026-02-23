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
  api("com.google.code.findbugs:jsr305:3.0.2")
  api("io.swagger:swagger-annotations:1.6.16")
  api("org.jetbrains:annotations:26.0.2-1")
  api("com.ibm.mq:com.ibm.mq.allclient:9.4.5.0")
  api("org.yaml:snakeyaml:2.5")
  api("com.fasterxml.jackson.core:jackson-databind:2.21.1")
  api("io.opentelemetry:opentelemetry-sdk")
  api("io.opentelemetry:opentelemetry-exporter-otlp")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  api("org.slf4j:slf4j-api:2.0.17")
  implementation("org.slf4j:slf4j-simple:2.0.17")
  testImplementation("com.google.guava:guava")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  ibmClientJar("com.ibm.mq:com.ibm.mq.allclient:9.4.5.0") {
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
        implementation("com.ibm.mq:com.ibm.mq.jakarta.client:9.4.5.0")
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
