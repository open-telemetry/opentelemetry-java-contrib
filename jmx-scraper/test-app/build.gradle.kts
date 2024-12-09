plugins {
  id("otel.java-conventions")

  application
}

description = "JMX metrics scraper - test application"

tasks {
  jar {
    manifest {
      attributes["Main-Class"] = "io.opentelemetry.contrib.jmxscraper.testapp.TestApp"
    }
  }
}
