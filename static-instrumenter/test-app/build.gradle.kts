plugins {
  id("otel.java-conventions")
  id("com.github.johnrengelman.shadow")
}

description = "OpenTelemetry Java Static Instrumentation Test Application"

dependencies {
  implementation("org.apache.httpcomponents:httpclient:4.5.14")
  implementation("org.slf4j:slf4j-api")
  runtimeOnly("org.slf4j:slf4j-simple")
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
  }

  jar {
    manifest {
      attributes("Main-Class" to "io.opentelemetry.contrib.staticinstrumenter.test.HttpClientTest")
    }
  }

  assemble {
    dependsOn(shadowJar)
  }
}
