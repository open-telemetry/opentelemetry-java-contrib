---
title: TraceQL
menuTitle: TraceQL
description: Learn about TraceQL, Tempo's query language for traces
weight: 450
aliases:
  - /docs/tempo/latest/traceql/
keywords:
  - Tempo query language
  - query language
  - TraceQL
---

# TraceQL

Inspired by PromQL and LogQL, TraceQL is a query language designed for selecting traces in Tempo. Currently, TraceQL query can select traces based on the following:

- Span and resource attributes, timing, and duration
- Basic aggregates: `count()` and `avg()`

Read the blog post, "[Get to know TraceQL](https://grafana.com/blog/2023/02/07/get-to-know-traceql-a-powerful-new-query-language-for-distributed-tracing/)," for an introduction to TraceQL and its capabilities.

{{< vimeo 796408188 >}}

For information on where the language is headed, see [future work](architecture).
The TraceQL language uses similar syntax and semantics as [PromQL](https://grafana.com/blog/2020/02/04/introduction-to-promql-the-prometheus-query-language/) and [LogQL](https://grafana.com/docs/loki/latest/logql/), where possible.

TraceQL requires Tempo’s Parquet columnar format to be enabled. For information on enabling Parquet, refer to the [Apache Parquet backend](https://grafana.com/docs/tempo/latest/configuration/parquet/) Tempo documentation.

## TraceQL query editor

With Tempo 2.0, you can use the TraceQL query editor in the Tempo data source to build queries and drill-down into result sets. The editor is available in Grafana’s Explore interface. For more information, refer to [TraceQL query editor]({{< relref "query-editor" >}}).

<p align="center"><img src="assets/query-editor-http-method.png" alt="Query editor showing request for http.method" /></p>

## Construct a TraceQL query

In TraceQL, a query is an expression that is evaluated on one trace at a time. The query is structured as a set of chained expressions (a pipeline). Each expression in the pipeline selects or discards spansets from being included in the results set. For example:

```
{ span.http.status_code >= 200 && span.http.status_code < 300 } | count() > 2
```

In this example, the search reduces traces to those spans where:

* `http.status_code` is in the range of `200` to `299` and
* the number of matching spans within a trace is greater than two.

Queries select sets of spans and filter them through a pipeline of aggregators and conditions. If, for a given trace, this pipeline produces a spanset then it is included in the results of the query.


## Selecting spans

TraceQL differentiates between two types of span data: intrinsics, which are fundamental to spans, and attributes, which are customizable key-value pairs. You can use intrinsics and attributes to build filters and select spans.

In TraceQL, curly brackets `{}` always select a set of spans from the current trace. They are commonly paired with a condition to reduce the spans being passed in.


### Intrinsic fields

Intrinsic fields are fundamental to spans. These fields can be referenced when selecting spans. Note that custom attributes are prefixed with `.`, `span.` or `resource.` whereas intrinsics are typed directly.

The following table shows the current intrinsic fields:

| **Field**     | **Type**    | **Definition**                                                  | **Example**            |
|---------------|-------------|-----------------------------------------------------------------|------------------------|
| status        | status enum | status: error, ok, or unset                                     | { status = ok }        |
| duration      | duration    | end - start time of the span                                    | { duration > 100ms }   |
| name          | string      | operation or span name                                          | { name = "HTTP POST" } |
| kind          | kind enum   | kind: server, client, producer, consumer, internal, unspecified | { kind = server }      |

### Attribute fields

There are two types of attributes: span attributes and resource attributes. By expanding a span in the Grafana UI, you can see both its span attributes (1 in the screenshot) and resource attributes (2 in the screenshot).

<p align="center"><img src="assets/span-resource-attributes.png" alt="Example of span and resource  attributes." /></p>

Attribute fields are derived from the span and can be customized. Process and span attribute types are [defined by the attribute itself](https://github.com/open-telemetry/opentelemetry-proto/blob/b43e9b18b76abf3ee040164b55b9c355217151f3/opentelemetry/proto/common/v1/common.proto#L30-L38), whereas intrinsic fields have a built-in type. You can refer to dynamic attributes (also known as tags) on the span or the span's resource.

Attributes in a query start with a span scope (for example, `span.http`) or resource scope (for example, `resource.namespace`)  depending on what you want to query. This provides significant performance benefits because it allows Tempo to only scan the data you are interested in.

To find traces with the `GET HTTP` method, your query could look like this:

```
{ span.http.method = "GET" }
```

For more information about attributes and resources, refer to the [OpenTelemetry Resource SDK](https://opentelemetry.io/docs/reference/specification/resource/sdk/).
#### Examples

Find traces that passed through the `production` environment:
```
{ resource.deployment.environment = "production" }
```

Find any database connection string that goes to a Postgres or MySQL database:
```
{ span.db.system =~ "postgresql|mysql" }
```

### Unscoped attribute fields

Attributes can be unscoped if you are unsure if the requested attribute exists on the span or resource. When possible, use scoped instead of unscoped attributes. Scoped attributes provide faster query results. 

For example, to find traces with an attribute of `sla` set to `critical`:
```
{ .sla = "critical" }
```

### Comparison operators

Comparison operators are used to test values within an expression.

The implemented comparison operators are:

- `=` (equality)
- `!=` (inequality)
- `>` (greater than)
- `>=` (greater than or equal to)
- `<` (less than)
- `<=` (less than or equal to)
- `=~` (regular expression)

TraceQL uses Golang regular expressions. Online regular expression testing sites like https://regex101.com/ are convenient to validate regular expressions used in TraceQL queries.

For example, to find all traces where an `http.status_code` attribute in a span are greater than `400` but less than equal to `500`:

```
{ span.http.status_code >= 400 && span.http.status_code < 500 }
```

Find all traces where the `http.method` attribute is either `GET` or `DELETE`:

```
{ span.http.method =~ “DELETE|GET” }
```

### Field expressions

Fields can also be combined in various ways to allow more flexible search criteria. A field expression is a composite of multiple fields that define all of the criteria that must be matched to return results.

#### Examples

Find traces with "success" `http.status_code` codes:

```
{ span.http.status_code >= 200 && span.http.status_code < 300 }
```

Find traces where a `DELETE` HTTP method was used and the instrinsic span status was not OK:

```
{ span.http.method = "DELETE" && status != ok }
```

Both expressions require all conditions to be true on the same span. The entire expression inside of a pair of `{}` must be evaluated as true on a single span for it to be included in the result set.

In the above example, if a span includes an `.http.method` attribute set to `DELETE` where the span also includes a `status` attribute set to `ok`, the trace would not be included in the returned results.

## Combining spansets

Spanset operators let you combine two sets of spans using and (`&&`) as well as union (`||`).

- `{condA} && {condB}`
- `{condA} || {condB}`


For example, to find a trace that went through two specific `cloud.region`:

```
{ resource.cloud.region = "us-east-1" } && { resource.cloud.region = "us-west-1" }
```

Note the difference between the previous example and this one:

```
{ resource.cloud.region = "us-east-1" && resource.cloud.region = "us-west-1" }
```

The second expression returns no traces because it's impossible for a single span to have a `resource.cloud.region` attribute that is set to both region values at the same time.

## Aggregators

So far, all of the example queries expressions have been about individual spans. You can use aggregate functions to ask questions about a set of spans. These currently consist of:

- `count` - The count of spans in the spanset.
- `avg` - The average of a given attribute or intrinsic for a spanset.

Aggregate functions allow you to carry out operations on matching results to further refine the traces returned. For more information on planned future work, refer to [How TraceQL works]({{< relref "architecture" >}}).

For example, to find traces where the total number of spans is greater than `10`:

```
count() > 10
```

Find traces where the average duration of the spans in a trace is greater than `20ms`:

```
avg(duration) > 20ms
```

For example, find traces that have more than 3 spans with an attribute `http.status_code` with a value of `200`:

```
{ span.http.status_code = 200 } | count() > 3
```

## Arithmetic

TraceQL supports arbitrary arithmetic in your queries. This can be useful to make queries more human readable:
```
{ span.http.request_content_length > 10 * 1024 * 1024 }
```
to compare the ratios of two span attributes:
```
{ span.bytes_processed < span.jobs_processed * 10 }
```
or anything else that comes to mind.

## Examples

### Find traces of a specific operation

Let's say that you want to find traces of a specific operation, then both the operation name (the span attribute `name`) and the name of the service that holds this operation (the resource attribute `service.name`) should be specified for proper filtering.
In the example below, traces are filtered on the `resource.service.name` value `frontend` and the span `name` value `POST /api/order`:

```
{resource.service.name = "frontend" && name = "POST /api/orders"}
```

When using the same Grafana stack for multiple environments (e.g., `production` and `staging`) or having services that share the same name but are differentiated though their namespace, the query looks like:

```
{
  resource.service.namespace = "ecommerce" &&
  resource.service.name = "frontend" &&  
  resource.deployment.environment = "production" && 
  name = "POST /api/orders"
}
```

### Find traces having a particular outcome

This example finds all traces on the operation `POST /api/orders` that have an erroneous root span:

```
{
  resource.service.name="frontend" && 
  name = "POST /api/orders" && 
  status = error
}
```

This example finds all traces on the operation `POST /api/orders` that return with an HTTP 5xx error:

```
{
  resource.service.name="frontend" && 
  name = "POST /api/orders" && 
  span.http.status_code >= 500
}
```

### Find traces that have a particuliar behavior

You can use query filtering on multiple spans of the traces.
This example locates all the traces of the `GET /api/products/{id}` operation that access a database. It's a convenient request to identify abnormal access ratios to the database caused by caching problems.

```
{span.service.name="frontend" && name = "GET /api/products/{id}"} && {.db.system="postgresql"}
```

### Find traces going through `production` and `staging` instances

This example finds traces that go through `production` and `staging` instances. 
It's a convenient request to identify misconfigurations and leaks across production and non-production environments. 

```
{ resource.deployment.environment = "production" } && { resource.deployment.environment = "staging" }
```

### Other examples

Find any trace with a `deployment.environment` attribute set to `production` and `http.status_code` attribute set to `200`:

```
{ .deployment.environment = "production" && .http.status_code = 200 }
```

Find any trace where spans within it have a `deployment.environment` resource attribute set to `production` and a span `http.status_code` attribute set to `200`. In previous examples, all conditions had to be true on one span. These conditions can be true on either different spans or the same spans.

```
{ resource.deployment.environment = "production" } && { span.http.status_code = 200 }
```

Find any trace where any span has an `http.method` attribute set to `GET` as well as a `status` attribute set to `ok`, where any other span also exists that has an `http.method` attribute set to `DELETE`, but does not have a `status` attribute set to `ok`:

```
{ span.http.method = "GET" && status = ok } && { span.http.method = "DELETE" && status != ok }
```
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
mvn dependency:copy -Dartifact=io.opentelemetry.contrib:opentelemetry-maven-extension:1.10.0-alpha

export OTEL_TRACES_EXPORTER="otlp"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://otel.example.com:4317"

mvn -Dmaven.ext.class.path=target/dependency/opentelemetry-maven-extension-1.10.0-alpha.jar verify
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
          <version>1.23.0-alpha</version>
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

| System property                           <br /> Environment variable                      | Default value           | Description                                                                                                                                     |
|--------------------------------------------------------------------------------------------|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `otel.traces.exporter`                    <br /> `OTEL_TRACES_EXPORTER`                    | `none`                  | Select the OpenTelemetry exporter for tracing, the currently only supported values are `none` and `otlp`. `none` makes the instrumentation NoOp |
| `otel.exporter.otlp.endpoint`             <br /> `OTEL_EXPORTER_OTLP_ENDPOINT`             | `http://localhost:4317` | The OTLP traces and metrics endpoint to connect to. Must be a URL with a scheme of either `http` or `https` based on the use of TLS.            |
| `otel.exporter.otlp.headers`              <br /> `OTEL_EXPORTER_OTLP_HEADERS`              |                         | Key-value pairs separated by commas to pass as request headers on OTLP trace and metrics requests.                                              |
| `otel.exporter.otlp.timeout`              <br /> `OTEL_EXPORTER_OTLP_TIMEOUT`              | `10000`                 | The maximum waiting time, in milliseconds, allowed to send each OTLP trace and metric batch.                                                    |
| `otel.resource.attributes`                <br /> `OTEL_RESOURCE_ATTRIBUTES`                |                         | Specify resource attributes in the following format: key1=val1,key2=val2,key3=val3                                                              |
| `otel.instrumentation.maven.mojo.enabled` <br /> `OTEL_INSTRUMENTATION_MAVEN_MOJO_ENABLED` | `true`                  | Whether to create spans for mojo goal executions, `true` or `false`. Can be configured to reduce the number of spans created for large builds.  |

ℹ️ The `service.name` is set to `maven` and the `service.version` to the version of the Maven runtime in use.

## Examples

Example of a trace of a Maven build.

![Execution trace of a Maven build](https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-contrib/main/maven-extension/docs/images/maven-execution-trace-jaeger.png)

### Example of a distributed trace of a Jenkins pipeline executing a Maven build

Distributed trace of a Jenkins pipeline invoking a Maven build instrumented with the [Jenkins OpenTelemetry plugin](https://plugins.jenkins.io/opentelemetry/) and the OpenTelemetry Maven Extension and visualized with [Jaeger Tracing](https://www.jaegertracing.io/)

![Execution trace of a Jenkins/Maven build](https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-contrib/main/maven-extension/docs/images/jenkins-maven-execution-trace-jaeger.png)

## Span attributes per Maven plugin goal execution

### Span attributes captured for every Maven plugin goal execution

| Span attribute                   |  Type  | Description                                                          |
|----------------------------------|--------|----------------------------------------------------------------------|
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

In addition to the span attributes captured on  every Maven plugin goal execution as described above:

| Span attribute                         |  Type  | Description                                                                                                                                         |
|----------------------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `http.method`                          | string | `POST`                                                                                                                                              |
| `http.url`                             | string | Base URL of the uploaded artifact `${maven.build.repository.url}/${groupId}/${artifactId}/${version}` where the `.` of `${groupId}` are replaced by `/`         |
| `maven.build.repository.id`            | string | ID of the Maven repository to which the artifact is deployed. See [Maven POM reference / Repository](https://maven.apache.org/pom.html#repository)  |
| `maven.build.repository.url`           | string | URL of the Maven repository to which the artifact is deployed. See [Maven POM reference / Repository](https://maven.apache.org/pom.html#repository) |
| `peer.service`                         | string | Maven repository hostname deduced from the Repository URL                                                                                           |

The `span.kind` is set to `client`

### `jib:build`

| Span attribute                                  |  Type    | Description                                                                                                |
|-------------------------------------------------|----------|------------------------------------------------------------------------------------------------------------|
| `http.method`                                   | string   | `POST`                                                                                                     |
| `http.url`                                      | string   | URL on the Docker registry deduced from the Docker image specified in the `build` goal configuration.      |
| `maven.build.container.image.name`              | string   | Name of the produced Docker image                                                                          |
| `maven.build.container.image.tags`              | string[] | Tags of the produced Docker image                                                                          |
| `maven.build.container.registry.url`            | string   | URL of the container registry to which this image is uploaded.                                             |
| `peer.service`                                  | string   | Docker Registry hostname.                                                                                  |

The `span.kind` is set to `client`

### `snyk:monitor`

See <https://github.com/snyk/snyk-maven-plugin>.

| Span attribute |  Type  | Description                                                                                    |
|----------------|--------|------------------------------------------------------------------------------------------------|
| `http.method`  | string | `POST`                                                               |
| `http.url`     | string | `https://snyk.io/api/v1/monitor/maven` the underlying Snyk API URL invoked by the Maven plugin.|
| `rpc.method`   | string | `monitor`, the underlying Snyk CLI command invoked by the Maven plugin.|
| `peer.service` | string | `snyk.io`                                                            |

The `span.kind` is set to `client`

### `snyk:test`

See <https://github.com/snyk/snyk-maven-plugin>.

| Span attribute |  Type  | Description                                                          |
|----------------|--------|----------------------------------------------------------------------|
| `http.method`  | string | `POST`                                                               |
| `http.url`     | string | `https://snyk.io/api/v1/test-dep-graph`                              |
| `rpc.method`   | string | `test`, the underlying Snyk CLI command invoked by the Maven plugin. |
| `peer.service` | string | `snyk.io`                                                            |

The `span.kind` is set to `client`

### `spring-boot:build-image`

| Span attribute                                 |  Type    | Description                                                                                                                                 |
|------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `http.method`                                  | string   | `POST`. Attribute only added when the `build-image` goal publishes the Docker image.                                                        |
| `http.url`                                     | string   | URL on the Docker registry, deduced from the Docker image. Attribute only added when the `build-image` goal publishes the Docker image.     |
| `maven.build.container.image.name`             | string   | Name of the produced Docker image. Attribute only added when the `build-image` goal publishes the Docker image.                             |
| `maven.build.container.image.tags`             | string[] | Tags of the produced Docker image. Attribute only added when the `build-image` goal publishes the Docker image.                             |
| `maven.build.container.registry.url`           | string   | URL of the container registry to which this image is uploaded.  Attribute only added when the `build-image` goal publishes the Docker image.|
| `peer.service`                                 | string   | Docker Registry hostname. Attribute only added when the `build-image` goal publishes the Docker image.                                      |

The `span.kind` is set to `client`

## Other CI/CD Tools supporting OpenTelemetry traces

List of other CI/CD tools that support OpenTelemetry traces and integrate with the Maven OpenTelemetry Extension creating a distributed traces providing end to end visibility.

### Jenkins OpenTelemetry Plugin

The [Jenkins OpenTelemetry Plugin](https://plugins.jenkins.io/opentelemetry/) exposes Jenkins pipelines & jobs as OpenTelemetry traces and exposes Jenkins health indicators as OpenTelemetry metrics.

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
The [HTTP](https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/http/)
and [database client calls](https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/database/)
conventions are particularly useful when  invoking external systems.

Steps to instrument a Maven Mojo:

* Add the OpenTelemetry API dependency in the `pom.xml` of the Maven plugin.

```xml
<project>
    ...
    <dependencies>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
            <version>LATEST</version>
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
        try (Scope ignored = childSpan.makeCurrent()) {
          // ... mojo sub operation
        } finally {
            childSpan.end();
        }
    }
}
```

## Component owners

- [Cyrille Le Clerc](https://github.com/cyrille-leclerc), Elastic
- [Ken Finnigan](https://github.com/kenfinnigan), Workday

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
