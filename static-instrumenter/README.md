# OpenTelemetry Java static instrumenter

## Structure

### Agent instrumenter

Module enhancing OpenTelemetry Java Agent for static instrumentation. The modified agent is capable of instrumenting and saving a new JAR with all relevant instrumentations applied and necessary helper class-code included.

To instrument the agent, first build agent-instrumenter using:

`./gradlew :static-instrumenter:agent-instrumenter:assemble`

Executable agent-instrumenter will be located under `build/libs/opentelemetry-agent-instrumenter.jar`

Then, instrument the agent (supported versions 1.10+):

`java -jar opentelemetry-agent-instrumenter.jar [path-to-agent] [output-dir]`

Output consists of two jars:

* `opentelemetry-javaagent.jar` - agent capable of static instrumentation
* `classpath-agent.jar` - agent's classes to be placed on instrumented application's classpath

To in order to statically instrument a JAR, run (using modified agent):

`java -Dota.static.instrumenter=true -javaagent:opentelemetry-javaagent.jar -cp your-app.jar io.opentelemetry.contrib.staticinstrumenter.internals.Main [output-dir]`

Finally, run the instrumented application:

`java -Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.contextStorageProvider=default -cp your-instrumented-app.jar:classpath-agent.jar org.example.MainClass`

### Gradle plugin

Gradle plugin running static instrumentation agent during the `assemble` lifecycle task. The assembled archive contains statically instrumened class code.

### Maven3 plugin

Maven3 plugin running the static instrumentation agent during the `package` phase. Packaged archive contains statically instrumened class code.

## Component owners

- [Jakub Wach](https://github.com/kubawach), Splunk
- [Anna Nosek](https://github.com/Enkelian), Splunk

Learn more about component owners in [component-owners.yml](../.github/workflows/component-owners.yml).
