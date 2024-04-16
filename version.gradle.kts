val stableVersion = "1.36.0-SNAPSHOT"
val alphaVersion = "1.36.0-alpha-SNAPSHOT"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
