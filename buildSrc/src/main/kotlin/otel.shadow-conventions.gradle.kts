import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.TextReportRenderer
import com.github.jk1.license.task.CheckLicensePreparationTask
import com.github.jk1.license.task.CheckLicenseTask
import com.github.jk1.license.task.ReportTask

plugins {
  id("com.gradleup.shadow")
  id("org.cyclonedx.bom")
  id("com.github.jk1.dependency-license-report")
}

// Temporary workaround until the license plugin supports configuration cache
tasks.withType<ReportTask>().configureEach {
  notCompatibleWithConfigurationCache("Unsupported")
}
tasks.withType<CheckLicensePreparationTask>().configureEach {
  notCompatibleWithConfigurationCache("Unsupported")
}
tasks.withType<CheckLicenseTask>().configureEach {
  notCompatibleWithConfigurationCache("Unsupported")
}

licenseReport {
  allowedLicensesFile = file(rootProject.file("config/dependency-license/allowed-licenses.json"))
  excludes = arrayOf("opentelemetry-java-contrib:dependencyManagement")
  filters = arrayOf(
    LicenseBundleNormalizer(
      rootProject.file("config/dependency-license/license-normalizer-bundle.json").absolutePath,
      false
    )
  )
  renderers = arrayOf<ReportRenderer>(
    TextReportRenderer()
  )
}

tasks.cyclonedxDirectBom {
  xmlOutput.unsetConvention()
}

val copyLegalDocs = tasks.register<Copy>("copyLegalDocs") {
  group = "documentation"
  description = "Copies legal files and generated checkLicense/SBOM reports into resources."

  from(layout.buildDirectory.file("reports/dependency-license/THIRD-PARTY-NOTICES.txt"))
  from(layout.buildDirectory.file("reports/cyclonedx-direct/bom.json"))

  into(layout.buildDirectory.dir("generated/legal-docs"))

  rename("bom.json", "SBOM.json")
}

copyLegalDocs.configure {
  tasks.findByName("cyclonedxDirectBom")?.let { dependsOn(it) }
  tasks.findByName("checkLicense")?.let { dependsOn(it) }
}

tasks.named<ShadowJar>("shadowJar") {
  dependsOn(copyLegalDocs)

  transform<com.github.jengelman.gradle.plugins.shadow.transformers.ApacheNoticeResourceTransformer>()

  manifest {
    attributes["Implementation-Version"] = project.version
  }

  // Prevent dependency-provided LICENSE/NOTICE files from being copied into the
  // distribution (they often duplicate or conflict). We still include the
  // project"s own `LICENSE` explicitly below.
  exclude(
    "DISCLAIMER",
    "license.header",
    "licenses/**",
    "META-INF/DEPENDENCIES",
    "META-INF/NOTICE.md",
    "META-INF/NOTICE",
    "META-INF/NOTICE.md",
    "META-INF/licenses/**",
    "META-INF/LICENSE*",
    "**/NOTICE*"
  )

  from(layout.buildDirectory.dir("generated/legal-docs")) {
    into("META-INF/licenses")
  }

  from(rootProject.file("LICENSE")) {
    into("META-INF")
  }
}

tasks.named<Task>("assemble") {
  dependsOn("shadowJar")
}
