# OpenTelemetry Java static instrumenter

## Structure

### Agent instrumenter

Module enhancing OpenTelemetry Java Agent for static instrumentation. The modified agent is capable of instrumenting and saving a new JAR with all relevant instrumentations applied and necessary helper class-code included.

In order to statically instrument a JAR, modified agent needs to be both attached (`-javaagent:`) and run as the main method (`io.opentelemetry.contrib.staticinstrumenter.Main` class).

### Gradle plugin

Gradle plugin running static instrumentation agent during the `assemble` lifecycle task. The assembled archive contains statically instrumened class code.

### Maven3 plugin

Maven3 plugin running the static instrumentation agent during the `package` phase. Packaged archive contains statically instrumened class code.

## Component owners

- [Jakub Wach](https://github.com/kubawach), Splunk
- [Anna Nosek](https://github.com/Enkelian), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
