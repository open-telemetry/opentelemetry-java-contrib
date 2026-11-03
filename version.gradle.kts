val stableVersion = "1.62.0"
val alphaVersion = "1.62.0-alpha"
val apidiffBaselineVersion = "1.61.0"
extra["tagVersion"] = "v$stableVersion"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
  extra["apidiffBaselineVersion"] = apidiffBaselineVersion
}
