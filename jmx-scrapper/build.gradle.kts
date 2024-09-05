plugins {
  application
  id("com.github.johnrengelman.shadow")

  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "JMX metrics scrapper"
otelJava.moduleName.set("io.opentelemetry.contrib.jmxscrapper")

application.mainClass.set("io.opentelemetry.contrib.jmxscrapper.JmxMetrics")
