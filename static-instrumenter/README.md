# OpenTelemetry Java static instrumenter

## Structure

### Agent instrumenter

Module enhancing OpenTelemetry Java Agent for static instrumentation. The modified agent can instrument and save a new JAR with all relevant instrumentations applied and necessary helper class-code included.

#### Generate a new application jar containing the static instrumentation

Execute the following command line with your application jar name and the desired output folder of the new jar:

`java -javaagent:opentelemetry-static-agent.jar -cp <your-app.jar> io.opentelemetry.contrib.staticinstrumenter.agent.main.Main <output-folder>`

The `opentelemetry-static-agent.jar` agent needs to be both attached (`-javaagent:`) and run as the main method (`io.opentelemetry.contrib.staticinstrumenter.agent.main.Main` class).

The generated jar will keep the name of your non-instrumented jar.

#### Run the instrumented application

Execute the following command line:

`java -cp <output-folder>/<your-app.jar><file-separator>no-inst-agent.jar <your-main-class-with-package-name>`

`<file-separator>` is `:` on UNIX systems and `;` on Windows systems.

### Maven3 plugin

Maven3 plugin running the static instrumentation agent during the `package` phase. Packaged archive contains statically instrumened class code.

## Component owners

- [Jakub Wach](https://github.com/kubawach), Splunk
- [Anna Nosek](https://github.com/anosek-an), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
