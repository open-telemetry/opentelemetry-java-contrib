plugins {
  war
}

description = "JMX metrics scraper - test web application"

dependencies {
  providedCompile("jakarta.servlet:jakarta.servlet-api:5.0.0")
}

java {
  // keeping java 8 compatibility to allow running this sample app in most containers
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}
