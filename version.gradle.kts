val stableVersion = "1.40.0"
val alphaVersion = "1.40.0-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
