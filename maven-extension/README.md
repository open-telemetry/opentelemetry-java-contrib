# Maven OpenTelemetry extension

Maven extension to observe Maven builds as distributed traces.

## Getting Started

The Maven OpenTelemetry Extension is configured using environment variables or JVM system properties and can be added to a build using one of the following ways:
* adding the extension jar to `${maven.home}/lib/ext`
* adding the path to the extension jar to`-Dmaven.ext.class.path`,
* adding the extension as a build extension in the `pom.xml`,
* (since Maven 3.3.1) configuring the extension in `.mvn/extensions.xml`.


### Adding the extension to the classpath

Add the Maven OpenTelemetry Extension to `${maven.home}/lib/ext` or to the classpath using `-Dmaven.ext.class.path=`.

```
mvn dependency:copy -Dartifact=io.opentelemetry.contrib:opentelemetry-maven-extension:1.9.0-alpha

export OTEL_TRACES_EXPORTER="otlp"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://otel.example.com:4317"

mvn -Dmaven.ext.class.path=target/dependency/opentelemetry-maven-extension-1.9.0-alpha.jar verify
```

### Declaring the extension in the `pom.xml` file


Add the Maven OpenTelemetry Extension in the `pom.xml` file:

```xml
<project>
  ...
  <build>
    <extensions>
      <extension>
          <groupId>io.opentelemetry.contrib</groupId>
          <artifactId>opentelemetry-maven-extension</artifactId>
          <version>1.9.0-alpha</version>
      </extension>
    </extensions>
  </build>
</project>
```

```
export OTEL_TRACES_EXPORTER="otlp"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://otel.example.com:4317"

mvn verify
```

## Configuration

❕ The setting `-Dotel.traces.exporter` / `OTEL_TRACES_EXPORTER` MUST be defined for the Maven OpenTelemetry Extension to export traces.

Without this setting, the traces won't be exported and the OpenTelemetry Maven Extension will behave as a NoOp extension. `otlp` is currently the only supported exporter.

The Maven OpenTelemetry Extension supports a subset of the [OpenTelemetry autoconfiguration environment variables and JVM system properties](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure).


| System property                         | Environment variable                    | Default value           | Description                                                                                                                                     |
|-----------------------------------------|-----------------------------------------|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| otel.traces.exporter                    | OTEL_TRACES_EXPORTER                    | `none`                  | Select the OpenTelemetry exporter for tracing, the currently only supported values are `none` and `otlp`. `none` makes the instrumentation NoOp |
| otel.exporter.otlp.endpoint             | OTEL_EXPORTER_OTLP_ENDPOINT             | `http://localhost:4317` | The OTLP traces and metrics endpoint to connect to. Must be a URL with a scheme of either `http` or `https` based on the use of TLS.            |
| otel.exporter.otlp.headers              | OTEL_EXPORTER_OTLP_HEADERS              |                         | Key-value pairs separated by commas to pass as request headers on OTLP trace and metrics requests.                                              |
| otel.exporter.otlp.timeout              | OTEL_EXPORTER_OTLP_TIMEOUT              | `10000`                 | The maximum waiting time, in milliseconds, allowed to send each OTLP trace and metric batch.                                                    |
| otel.resource.attributes                | OTEL_RESOURCE_ATTRIBUTES                |                         | Specify resource attributes in the following format: key1=val1,key2=val2,key3=val3                                                              |
| otel.instrumentation.maven.mojo.enabled | OTEL_INSTRUMENTATION_MAVEN_MOJO_ENABLED | `true`                  | Whether to create spans for mojo goal executions, `true` or `false`. Can be configured to reduce the number of spans created for large builds.  |


ℹ️ The `service.name` is set to `maven` and the `service.version` to the version of the Maven runtime in use.

## Examples

Example of a trace of a Maven build.

![](https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-contrib/main/maven-extension/docs/images/maven-execution-trace-jaeger.png)

### Example of a distributed trace of a Jenkins pipeline executing a Maven build

Distributed trace of a Jenkins pipeline invoking a Maven build instrumented with the  [Jenkins OpenTelemetry plugin](https://plugins.jenkins.io/opentelemetry/) and the OpenTelemetry Maven Extension and visualized with [Jaeger Tracing](https://www.jaegertracing.io/)

![](https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-contrib/main/maven-extension/docs/images/jenkins-maven-execution-trace-jaeger.png)

## Span attributes per Maven plugin goal execution

### Span attributes captured for every Maven plugin goal execution

| Span attribute                   | Description                                                          |
|----------------------------------|----------------------------------------------------------------------|
| `maven.project.groupId`          | Group ID of the Maven project on which the Maven goal is executed    |
| `maven.project.artifactId`       | Artifact ID of the Maven project on which the Maven goal is executed |
| `maven.project.version`          | Version of the Maven project on which the Maven goal is executed     |
| `maven.plugin.groupId`           | Group ID of the Maven plugin on which the Maven goal is executed     |
| `maven.plugin.artifactId`        | Artifact ID of the Maven plugin on which the Maven goal is executed  |
| `maven.plugin.version`           | Version of the Maven plugin on which the Maven goal is executed      |
| `maven.execution.goal`           | Goal that is being executed                                          |
| `maven.execution.id`             | ID of the execution                                                  |
| `maven.execution.lifecyclePhase` | Lifecycle phase to which belong the execution                        |

### `deploy:deploy`

In addition to the span attributes captured on  every Maven plugin goal execution as described above:

| Span attribute              | Description                                                                                                                                         |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `maven.repository.id`       | ID of the Maven repository to which the artifact is deployed. See [Maven POM reference / Repository](https://maven.apache.org/pom.html#repository)  |
| `maven.repository.url`      | URL of the Maven repository to which the artifact is deployed. See [Maven POM reference / Repository](https://maven.apache.org/pom.html#repository) |
| `maven.repository.username` | Username used to upload artifacts to the Maven repository. See [Maven POM reference / Repository](https://maven.apache.org/pom.html#repository)     |
| `peer.service`              | Maven repository hostname deduced from the Repository URL                                                                                           |
| `http.url`                  | Base URL of the uploaded artifact `${repository.url}/${groupId}/${artifactId}/${version}` where the `.` of `${groupId}` are replaced by `/`         |
| `http.method`               | `POST`                                                                                                                                              |

The `span.kind` is set to `client`

### `spring-boot:build-image`

| Span attribute                | Description                                                                                                                                                                              |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `container.image.name`        | Docker image specified in the `build-image` goal configuration                                                                                                                           |
| `container.image.tag`         | Docker tag specified in the `build-image` goal configuration                                                                                                                             |
| `container.registry.url`      | URL of the container registry to which this image is uploaded.  Attribute only added when the `build-image` goal publishes the Docker image.                                             |
| `container.registry.username` | Username used to upload the image to the container registry.  Attribute only added when the `build-image` goal publishes the Docker image.                                               |
| `peer.service`                | Docker Registry hostname. Attribute only added when the `build-image` goal publishes the Docker image.                                                                                   |
| `http.url`                    | URL on the Docker registry deduced from the Docker image specified in the `build-image` goal configuration. Attribute only added when the `build-image` goal publishes the Docker image. |
| `http.method`                 | `POST`. Attribute only added when the `build-image` goal publishes the Docker image.                                                                                                     |

The `span.kind` is set to `client`

TODO How can we report cases when multiple Docker image tags are published?  Introducing `container.image.tags`?

## Other CI/CD Tools supporting OpenTelemetry traces

List of other CI/CD tools that support OpenTelemetry traces and integrate with the Maven OpenTelemetry Extension creating a distributed traces providing end to end visibility.

### Jenkins OpenTelemetry Plugin

The [Jenkins OpenTelemetry Plugin](https://plugins.jenkins.io/opentelemetry/) exposes Jenkins pipelines & jobs as OpenTelemetry traces and exposes Jenkins health indicators as OpenTelemetry metrics.

### Otel CLI

The [`otel-cli`](https://github.com/equinix-labs/otel-cli) is a command line wrapper to observe the execution of a shell command as an OpenTelemetry trace.
