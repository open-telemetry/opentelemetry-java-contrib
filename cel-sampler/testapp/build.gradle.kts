plugins {
  java
  application
}

application {
  mainClass.set("io.opentelemetry.contrib.sampler.cel.testapp.SimpleServer")
}

tasks.jar {
  manifest {
    attributes["Main-Class"] = "io.opentelemetry.contrib.sampler.cel.testapp.SimpleServer"
  }
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}