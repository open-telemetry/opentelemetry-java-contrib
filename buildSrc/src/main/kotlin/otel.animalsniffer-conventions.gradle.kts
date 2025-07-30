import ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer

plugins {
  id("otel.java-conventions")
  id("ru.vyarus.animalsniffer")
}

dependencies {
  signature("com.toasttab.android:gummy-bears-api-21:0.12.0:coreLib@signature")
}

animalsniffer {
  sourceSets = listOf(java.sourceSets.main.get())
}

// Always having declared output makes this task properly participate in tasks up-to-date checks
tasks.withType<AnimalSniffer> {
  reports.text.required.set(true)
}

// Attaching animalsniffer check to the compilation process.
tasks.named("classes").configure {
  finalizedBy("animalsnifferMain")
}
