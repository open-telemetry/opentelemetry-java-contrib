plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("com.gradleup.shadow")
}

description = "Sampler which makes its decision based on semantic attributes values"
otelJava.moduleName.set("io.opentelemetry.contrib.sampler.cel")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")

  implementation("dev.cel:cel:0.11.1")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator")

  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
}

tasks {
  shadowJar {
    /**
     * Shaded version of this extension is required when using it as a OpenTelemetry Java Agent
     * extension. Shading bundles the dependencies required by this extension in the resulting JAR,
     * ensuring their presence on the classpath at runtime.
     *
     * See http://gradleup.com/shadow/introduction/#introduction for reference.
     */
    archiveClassifier.set("shadow")
  }

  jar {
    /**
     * We need to publish both - shaded and unshaded variants of the dependency
     * Shaded dependency is required for use with the Java agent.
     * Unshaded dependency can be used with OTel Autoconfigure module.
     *
     * Not overriding the classifier to empty results in an implicit classifier 'plain' being
     * used with the standard JAR.
     */
    archiveClassifier.set("")
  }

  assemble {
    dependsOn(shadowJar)
  }
}
