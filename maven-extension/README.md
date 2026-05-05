# Maven OpenTelemetry extension

Maven extension to observe Maven builds as distributed traces.

## Getting Started

The Maven OpenTelemetry Extension is configured using environment variables or
JVM system properties and can be added to a build in one of the following ways:

* adding the extension jar to `${maven.home}/lib/ext`
* adding the path to the extension jar to`-Dmaven.ext.class.path`,
* adding the extension as a build extension in the `pom.xml`,
* (since Maven 3.3.1) configuring the extension in `.mvn/extensions.xml`.

In the code snippets below, replace `OPENTELEMETRY_MAVEN_VERSION` with the [latest
release](https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-maven-extension).

### Adding the extension to the classpath

Add the Maven OpenTelemetry Extension to `${maven.home}/lib/ext` or to the classpath using `-Dmaven.ext.class.path=`.

```text
mvn dependency:copy -Dartifact=io.opentelemetry.contrib:opentelemetry-maven-extension:1.10.0-alpha

export OTEL_TRACES_EXPORTER="otlp"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://otel.example.com:4317"

mvn -Dmaven.ext.class.path=target/dependency/opentelemetry-maven-extension-OPENTELEMETRY_MAVEN_VERSION.jar verify
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
          <version>OPENTELEMETRY_MAVEN_VERSION</version>
      </extension>
    </extensions>
  </build>
</project>
```

```text
export OTEL_TRACES_EXPORTER="otlp"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://otel.example.com:4317"

mvn verify
```

## Configuration

❕ The setting `-Dotel.traces.exporter` / `OTEL_TRACES_EXPORTER` MUST be defined
for the Maven OpenTelemetry Extension to export traces.

Without this setting, traces will not be exported and the OpenTelemetry Maven
Extension will behave as a NoOp extension.
`otlp` is currently the only supported exporter.

The Maven OpenTelemetry Extension supports a subset of the
[OpenTelemetry autoconfiguration environment variables and JVM system properties](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure).

Supported configuration:

* `otel.traces.exporter` / `OTEL_TRACES_EXPORTER`
  Default: `none`
  Selects the tracing exporter.
  The only supported values are `none` and `otlp`.
* `otel.exporter.otlp.endpoint` / `OTEL_EXPORTER_OTLP_ENDPOINT`
  Default: `http://localhost:4317`
  Sets the OTLP traces and metrics endpoint.
* `otel.exporter.otlp.headers` / `OTEL_EXPORTER_OTLP_HEADERS`
  Passes comma-separated request headers for OTLP trace and metric requests.
* `otel.exporter.otlp.timeout` / `OTEL_EXPORTER_OTLP_TIMEOUT`
  Default: `10000`
  Sets the maximum wait time in milliseconds for each OTLP trace or metric
  batch.
* `otel.resource.attributes` / `OTEL_RESOURCE_ATTRIBUTES`
  Specifies resource attributes in the format `key1=val1,key2=val2`.
* `otel.instrumentation.maven.mojo.enabled` /
  `OTEL_INSTRUMENTATION_MAVEN_MOJO_ENABLED`
  Default: `true`
  Controls whether spans are created for mojo goal executions.
* `otel.instrumentation.maven.transfer.enabled` /
  `OTEL_INSTRUMENTATION_MAVEN_TRANSFER_ENABLED`
  Default: `false`
  Controls whether spans are created for artifact transfers.

ℹ️ The `service.name` is set to `maven` and the `service.version` to the version of the Maven runtime in use.

## Examples

Example of a trace of a Maven build.

![Execution trace of a Maven build](https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-contrib/main/maven-extension/docs/images/maven-execution-trace-jaeger.png)

### Example of a distributed trace of a Jenkins pipeline executing a Maven build

This is a distributed trace of a Jenkins pipeline invoking a Maven build
instrumented with the
[Jenkins OpenTelemetry plugin](https://plugins.jenkins.io/opentelemetry/) and
the OpenTelemetry Maven Extension, visualized with
[Jaeger Tracing](https://www.jaegertracing.io/).

![Execution trace of a Jenkins/Maven build](https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-contrib/main/maven-extension/docs/images/jenkins-maven-execution-trace-jaeger.png)

## Span attributes per Maven plugin goal execution

### Span attributes captured for every Maven plugin goal execution

| Span attribute                   | Type   | Description                                                          |
| -------------------------------- | ------ | -------------------------------------------------------------------- |
| `maven.project.groupId`          | string | Group ID of the Maven project on which the Maven goal is executed    |
| `maven.project.artifactId`       | string | Artifact ID of the Maven project on which the Maven goal is executed |
| `maven.project.version`          | string | Version of the Maven project on which the Maven goal is executed     |
| `maven.plugin.groupId`           | string | Group ID of the Maven plugin on which the Maven goal is executed     |
| `maven.plugin.artifactId`        | string | Artifact ID of the Maven plugin on which the Maven goal is executed  |
| `maven.plugin.version`           | string | Version of the Maven plugin on which the Maven goal is executed      |
| `maven.execution.goal`           | string | Goal that is being executed                                          |
| `maven.execution.id`             | string | ID of the execution                                                  |
| `maven.execution.lifecyclePhase` | string | Lifecycle phase to which belong the execution                        |

### `deploy:deploy`

In addition to the span attributes captured on every Maven plugin goal execution as described above:

| Span attribute               | Type   | Description                                                                                                                                             |
| ---------------------------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `http.method`                | string | `POST`                                                                                                                                                  |
| `http.url`                   | string | Base URL of the uploaded artifact `${maven.build.repository.url}/${groupId}/${artifactId}/${version}` where the `.` of `${groupId}` are replaced by `/` |
| `maven.build.repository.id`  | string | ID of the Maven repository to which the artifact is deployed. See [Maven POM reference / Repository](https://maven.apache.org/pom.html#Repository)      |
| `maven.build.repository.url` | string | URL of the Maven repository to which the artifact is deployed. See [Maven POM reference / Repository](https://maven.apache.org/pom.html#Repository)     |
| `peer.service`               | string | Maven repository hostname deduced from the Repository URL                                                                                               |

The `span.kind` is set to `client`

### `jib:build`

| Span attribute                       | Type     | Description                                                                                           |
| ------------------------------------ | -------- | ----------------------------------------------------------------------------------------------------- |
| `http.method`                        | string   | `POST`                                                                                                |
| `http.url`                           | string   | URL on the Docker registry deduced from the Docker image specified in the `build` goal configuration. |
| `maven.build.container.image.name`   | string   | Name of the produced Docker image                                                                     |
| `maven.build.container.image.tags`   | string[] | Tags of the produced Docker image                                                                     |
| `maven.build.container.registry.url` | string   | URL of the container registry to which this image is uploaded.                                        |
| `peer.service`                       | string   | Docker Registry hostname.                                                                             |

The `span.kind` is set to `client`

### `snyk:monitor`

See <https://github.com/snyk/snyk-maven-plugin>.

| Span attribute | Type   | Description                                                                                  |
| -------------- | ------ | -------------------------------------------------------------------------------------------- |
| `http.method`  | string | `POST`                                                                                       |
| `http.url`     | string | Underlying Snyk API URL invoked by the Maven plugin: `https://snyk.io/api/v1/monitor/maven`. |
| `rpc.method`   | string | `monitor`, the underlying Snyk CLI command invoked by the Maven plugin.                      |
| `peer.service` | string | `snyk.io`                                                                                    |

The `span.kind` is set to `client`

### `snyk:test`

See <https://github.com/snyk/snyk-maven-plugin>.

| Span attribute | Type   | Description                                                          |
| -------------- | ------ | -------------------------------------------------------------------- |
| `http.method`  | string | `POST`                                                               |
| `http.url`     | string | `https://snyk.io/api/v1/test-dep-graph`                              |
| `rpc.method`   | string | `test`, the underlying Snyk CLI command invoked by the Maven plugin. |
| `peer.service` | string | `snyk.io`                                                            |

The `span.kind` is set to `client`

### `spring-boot:build-image`

| Span attribute                       | Type     | Description                                                                                                                                  |
| ------------------------------------ | -------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `http.method`                        | string   | `POST`. Attribute only added when the `build-image` goal publishes the Docker image.                                                         |
| `http.url`                           | string   | URL on the Docker registry, deduced from the Docker image. Attribute only added when the `build-image` goal publishes the Docker image.      |
| `maven.build.container.image.name`   | string   | Name of the produced Docker image. Attribute only added when the `build-image` goal publishes the Docker image.                              |
| `maven.build.container.image.tags`   | string[] | Tags of the produced Docker image. Attribute only added when the `build-image` goal publishes the Docker image.                              |
| `maven.build.container.registry.url` | string   | URL of the container registry to which this image is uploaded.  Attribute only added when the `build-image` goal publishes the Docker image. |
| `peer.service`                       | string   | Docker Registry hostname. Attribute only added when the `build-image` goal publishes the Docker image.                                       |

The `span.kind` is set to `client`

## Other CI/CD Tools supporting OpenTelemetry traces

This section lists other CI/CD tools that support OpenTelemetry traces and
integrate with the Maven OpenTelemetry Extension to provide end-to-end
visibility.

### Jenkins OpenTelemetry Plugin

The [Jenkins OpenTelemetry Plugin](https://plugins.jenkins.io/opentelemetry/)
exposes Jenkins pipelines and jobs as OpenTelemetry traces and Jenkins health
indicators as OpenTelemetry metrics.

### Otel CLI

The [`otel-cli`](https://github.com/equinix-labs/otel-cli) is a command line wrapper to observe the execution of a shell command as an OpenTelemetry trace.

## Instrumenting Maven Mojos for better visibility in Maven builds

Maven plugin authors can instrument Mojos for better visibility in Maven builds.

Common instrumentation patterns include:

* Adding contextual data as attributes on the spans created by the OpenTelemetry Maven Extension,
* Creating additional sub spans to breakdown long mojo goal executions in finer grained steps

Note that the instrumentation of a plugin is enabled when the OpenTelemetry Maven extension is added to the build and activated.
Otherwise, the instrumentation of the Maven plugin is noop.

It is recommended to enrich spans using the [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/concepts/semantic-conventions/)
to improve the visualization and analysis in Observability products.
The HTTP and database client
[semantic conventions](https://github.com/open-telemetry/semantic-conventions)
are particularly useful when invoking external systems.

Steps to instrument a Maven Mojo:

* Add the OpenTelemetry API dependency in the `pom.xml` of the Maven plugin.
  Replace `OPENTELEMETRY_VERSION` with the
  [latest release](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-api).

```xml
<project>
    ...
    <dependencies>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
            <version>OPENTELEMETRY_VERSION</version>
        </dependency>
        ...
    </dependencies>
</project>
````

* Instrument the Mojo:

```java
@Mojo(name = "test", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class TestMojo extends AbstractMojo {

  @Override
  public void execute() {
    Span mojoExecuteSpan = Span.current();

    // ENRICH THE DEFAULT SPAN OF THE MOJO EXECUTION
    // (this span is created by the opentelemetry-maven-extension)
    mojoExecuteSpan.setAttribute("an-attribute", "a-value");

    // ... some logic

    // CREATE SUB SPANS TO CAPTURE FINE GRAINED DETAILS OF THE MOJO EXECUTION
    Tracer tracer = GlobalOpenTelemetry.get().getTracer("com.example.maven.otel_aware_plugin");
    Span childSpan = tracer.spanBuilder("otel-aware-goal-sub-span").setAttribute("another-attribute", "another-value").startSpan();
    try (Scope ignored2 = childSpan.makeCurrent()) {
      // ... mojo sub operation
    } finally {
      childSpan.end();
    }
  }
}
```

## Component owners

* [Cyrille Le Clerc](https://github.com/cyrille-leclerc), Grafana Labs
* [Ken Finnigan](https://github.com/kenfinnigan), Workday

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
