plugins {
  application
}

description = "JMX metrics scraper - test application"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(8)
  }
}

tasks {
  jar {
    manifest {
      attributes["Main-Class"] = "io.opentelemetry.contrib.jmxscraper.testapp.TestApp"
    }
  }
}
