val stableVersion = "1.63.0-SNAPSHOT"
val alphaVersion = "1.63.0-alpha-SNAPSHOT"
val apidiffBaselineVersion = "1.62.0"
extra["tagVersion"] = "v$stableVersion"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
  extra["apidiffBaselineVersion"] = apidiffBaselineVersion
}
