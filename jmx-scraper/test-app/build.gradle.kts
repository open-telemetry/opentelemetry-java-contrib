plugins {
  application
}

description = "JMX metrics scraper - test application"

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
  jar {
    manifest {
      attributes["Main-Class"] = "io.opentelemetry.contrib.jmxscraper.testapp.TestApp"
    }
  }
}
