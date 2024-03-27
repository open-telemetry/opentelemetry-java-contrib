val stableVersion = "1.34.0"
val alphaVersion = "1.34.0-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
