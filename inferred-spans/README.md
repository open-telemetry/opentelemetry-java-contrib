# OpenTelemetry Inferred Spans Extension

OpenTelemetry extension for generating spans via profiling instead of instrumentation.
This extension enhances traces by running
[async-profiler](https://github.com/async-profiler/async-profiler) in
wall-clock profiling mode whenever there is an active sampled OpenTelemetry
span.

The resulting profiling data is analyzed afterward and spans are "inferred".
This means there is a delay between the regular and the inferred spans being visible in your OpenTelemetry backend/UI.

Only platform threads are supported. Virtual threads are not supported and will not be profiled.

## Usage

This section describes the usage of this extension outside of an agent.
Add the following dependency to your project:

```text
<dependency>
    <groupId>io.opentelemetry.contrib</groupId>
    <artifactId>opentelemetry-inferred-spans</artifactId>
    <version>{latest version}</version>
</dependency>
```

### Autoconfiguration

This extension supports [autoconfiguration](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure).

So if you are using an autoconfigured OpenTelemetry SDK, you only need to add
this extension to your class path and configure it via system properties or
environment variables:

Configuration options:

- `otel.inferred.spans.enabled` / `OTEL_INFERRED_SPANS_ENABLED`
  Default: `false`
  Enables the inferred spans feature.
- `otel.inferred.spans.logging.enabled` / `OTEL_INFERRED_SPANS_LOGGING_ENABLED`
  Default: `true`
  By default, async-profiler prints warning messages about missing JVM symbols
  to standard output. Set this option to `false` to suppress those messages.
- `otel.inferred.spans.backup.diagnostic.files` / `OTEL_INFERRED_SPANS_BACKUP_DIAGNOSTIC_FILES`
  Default: `false`
  Do not delete the temporary profiling files so they can be used later for
  reproduction.
- `otel.inferred.spans.safe.mode` / `OTEL_INFERRED_SPANS_SAFE_MODE`
  Default: `0`
  Controls the async-profiler recovery behavior as a bit mask from `0` to `31`.
  `0` enables all recovery attempts and `31` disables all five.
- `otel.inferred.spans.post.processing.enabled` / `OTEL_INFERRED_SPANS_POST_PROCESSING_ENABLED`
  Default: `true`
  Lets you test the effect of async-profiler in isolation from the agent's
  post-processing.
- `otel.inferred.spans.sampling.interval` / `OTEL_INFERRED_SPANS_SAMPLING_INTERVAL`
  Default: `50ms`
  Controls how often stack traces are gathered within a profiling session.
  Lower values improve duration accuracy at the cost of higher overhead.
- `otel.inferred.spans.min.duration` / `OTEL_INFERRED_SPANS_MIN_DURATION`
  Default: `0ms`
  Sets the minimum duration of an inferred span.
- `otel.inferred.spans.included.classes` / `OTEL_INFERRED_SPANS_INCLUDED_CLASSES`
  Default: `*`
  Limits inferred spans to matching classes.
  Example: `org.example.myapp.*`
- `otel.inferred.spans.excluded.classes` / `OTEL_INFERRED_SPANS_EXCLUDED_CLASSES`
  Default:
  `java.*`, `javax.*`, `sun.*`, `com.sun.*`, `jdk.*`,
  `org.apache.tomcat.*`, `org.apache.catalina.*`, `org.apache.coyote.*`,
  `org.jboss.as.*`, `org.glassfish.*`, `org.eclipse.jetty.*`,
  `com.ibm.websphere.*`, and `io.undertow.*`
  Excludes classes for which no profiler-inferred spans should be created.
- `otel.inferred.spans.interval` / `OTEL_INFERRED_SPANS_INTERVAL`
  Default: `5s`
  Sets the interval at which profiling sessions should be started.
- `otel.inferred.spans.duration` / `OTEL_INFERRED_SPANS_DURATION`
  Default: `5s`
  Sets the duration of each profiling session.
  For sampled transactions fully contained within a profiling session,
  inferred spans will be created and shown in the trace waterfall.
- `otel.inferred.spans.lib.directory` / `OTEL_INFERRED_SPANS_LIB_DIRECTORY`
  Default: `java.io.tmpdir`
  Controls where the async-profiler shared library is extracted before loading.
  If `/tmp` is mounted `noexec`, set this to an executable directory such as
  `/var/tmp`.
- `otel.inferred.spans.parent.override.handler` / `OTEL_INFERRED_SPANS_PARENT_OVERRIDE_HANDLER`
  Default: a handler that adds span-links to the inferred span
  Lets you override how inferred-parent relationships are represented by
  providing a `BiConsumer<SpanBuilder, SpanContext>` implementation.

### Usage with declarative configuration

You can configure the inferred spans processor using declarative YAML configuration with the
OpenTelemetry SDK. For example:

```yaml
file_format: 1.0-rc.1
tracer_provider:
  processors:
    - inferred_spans/development:
        enabled: true # true by default unlike autoconfiguration described above
        sampling_interval: 25ms
        included_classes: "org.example.myapp.*"
        excluded_classes: "java.*"
        min_duration: 10ms
        interval: 5s
        duration: 5s
        lib_directory: "/var/tmp"
        parent_override_handler: "com.example.MyParentOverrideHandler"
```

All the same settings as for [autoconfiguration](#autoconfiguration) can be used here,
just with the `otel.inferred.spans.` prefix stripped.
For example, `otel.inferred.spans.sampling.interval` becomes `sampling_interval` in YAML.

### Manual SDK setup

If you manually set-up your `OpenTelemetrySDK`, you need to create and register an `InferredSpansProcessor` with your `TracerProvider`:

```java
InferredSpansProcessor inferredSpans = InferredSpansProcessor.builder()
  //.samplingInterval(Duration.ofMillis(10))
  .build();
SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
  .addSpanProcessor(inferredSpans)
  .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder()
    .setEndpoint("http://localhost:4317")
    .build()).build())
  .build();
inferredSpans.setTracerProvider(tracerProvider);
```

The `setTracerProvider(..)` call shown at the end may be omitted.
In that case, `GlobalOpenTelemetry` will be used for generating the inferred
spans.

## Known issues

### Missing inferred spans

- After each profiling session, while the stack traces and activation events are processed, no traces are collected.
  - Under load, processing can take seconds; ~200ms are normal.
  - Log:

    ```text
    DEBUG Processing {} stack traces
    ...
    DEBUG Processing traces took {}µs
    ```

- While stack traces are processed, activation events are still put into the
  ring buffer.
  However, they don't get processed.
  If, during this period, there are more activation events than the buffer can
  handle, activation events are lost.
  - Log: `Could not add activation event to ring buffer as no slots are available`
  - Lost activation events can lead to orphaned call trees (lost end event),
    missing roots (lost start event), and broken parent/child relationships
    (lost span activations/deactivations).
    Log:

  ```text
  DEBUG Illegal state ...
  ```

- Under load, the activation event ring buffer can also get full
- The actual `otel.inferred.spans.sampling.interval` might be a bit lower.
  async-profiler aims to keep the interval relatively consistent, but if too
  many threads are actively running transactions or there is a traffic spike,
  the interval can be lower.
- As a result of the above, some transactions don't contain inferred spans, even if their duration is longer than `otel.inferred.spans.sampling.interval`.
  Log:

  ```text
  DEBUG Created no spans for thread {} (count={})
  ```

- There can be a race condition when putting activation events into the queue,
  which leads to older events being in front of newer ones, like
  `1, 2, 4, 3, 5`.
  This is infrequent, and the consequences are similar to losing that
  activation event or getting an event without any consequence.
  Log:

  ```text
  Timestamp of current activation event ({}) is lower than the one from the previous event ({})
  ```

### Incorrect parent/child relationships

#### Without workaround

Inferred span starts after actual span, even though it should be the parent

```text
 ---------[inferred ]
 [actual]
^         ^         ^
```

Inferred span ends before actual span, even though it should be the parent

```text
[inferred   ]------------
             [actual]
^           ^           ^
```

```text
   -------[inferred ]-------                 [actual         ]
       [actual         ]          ->     -------[inferred ]-------
^         ^         ^         ^
```

Two consecutive method invocations are interpreted as one longer execution

```text
[actual]   [actual]   ->  [--------  --------]
^          ^
```

#### With workaround

These are some tricky situations we have managed to find a workaround for.

##### Regular spans as a child of an inferred span

This is tricky as regular spans are exported right after the event has ended.
Inferred spans are sent later - after the profiling session ends.

This is how the situation looks like without a workaround:

```text
[transaction   ]     [transaction   ]
└─[inferred  ]    -> ├─[inferred  ]
  └─[actual]         └───[actual]
```

There are situations where the ordering is off as a result of that.

The workaround is that inferred spans have span-links with a special `is_child` attribute,
pointing to the regular spans they are the parent of.

##### Parent inferred span ends before child

Workaround: set end timestamp of inferred span to end timestamp of actual span.

```text
[inferred ]--------         [inferred  -----]--
         [actual]       ->           [actual]
^         ^         ^
```

##### Parent inferred span starts after child

Workaround: set start timestamp of inferred span to start timestamp of actual span.

```text
  --------[inferred ]          --[------inferred ]
    [actual ]           ->       [actual ]
^         ^         ^
```

#### Example

In this screenshot, we can see several problems at once
![Inferred spans issues](https://user-images.githubusercontent.com/2163464/75677751-710bd880-5c8c-11ea-8bd9-1c6d5f3268d5.png)

## Component owners

- [Jack Shirazi](https://github.com/jackshirazi), Elastic
- [Jonas Kunz](https://github.com/jonaskunz), Elastic
- [Sylvain Juge](https://github.com/sylvainjuge), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
