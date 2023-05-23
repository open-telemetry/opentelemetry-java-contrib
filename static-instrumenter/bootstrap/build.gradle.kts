plugins {
  id("otel.java-conventions")
}

description = "Bootstrap classes for static agent"

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_11)
}
