val stableVersion = "1.49.0"
val alphaVersion = "1.49.0-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
