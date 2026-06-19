import ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer

plugins {
  id("otel.java-conventions")
  id("ru.vyarus.animalsniffer")
}

dependencies {
  signature("com.toasttab.android:gummy-bears-api-23:0.14.0:coreLib2@signature")
}

animalsniffer {
  sourceSets = listOf(java.sourceSets.main.get())
}

// Always having declared output makes this task properly participate in tasks up-to-date checks
tasks.withType<AnimalSniffer> {
  reports.text.required.set(true)
}
