# OpenTelemetry Java static instrumenter

## Structure

### Agent instrumenter

Module enhancing OpenTelemetry Java Agent for static instrumentation. The modified agent is capable of instrumenting and saving a new JAR with all relevant instrumentations applied and necessary helper class-code included.

In order to statically instrument a JAR, modified agent needs to be both attached (`-javaagent:`) and run as the main method (`io.opentelemetry.contrib.staticinstrumenter.agent.main.Main` class).
:

To instrument you app use `opentelemetry-static-agent.jar` and pass the main class of the agent:

`java -javaagent:opentelemetry-static-agent.jar -cp your-app.jar io.opentelemetry.contrib.staticinstrumenter.agent.main.Main output-folder`

To run an instrumented app pass the `-Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.contextStorageProvider=default` option and add `no-inst-agent.jar` to the classpath:

`java -Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.contextStorageProvider=default -cp output-folder/your-app.jar:no-inst-agent.jar org.example.YourMainClass`

### Gradle plugin

Gradle plugin running static instrumentation agent during the `assemble` lifecycle task. The assembled archive contains statically instrumened class code.

### Maven3 plugin

Maven3 plugin running the static instrumentation agent during the `package` phase. Packaged archive contains statically instrumened class code.

## Component owners

- [Jakub Wach](https://github.com/kubawach), Splunk
- [Anna Nosek](https://github.com/anosek-an), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
