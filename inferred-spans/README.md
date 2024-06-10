# OpenTelemetry Inferred Spans Extension

OpenTelemetry extension for generating spans via profiling instead of instrumentation.
This extension enhances traces by running [async-profiler](https://github.com/async-profiler/async-profiler) in wall-clock profiling mode whenever there is an active sampled OpenTelemetry span.

The resulting profiling data is afterwards analyzed and spans are "inferred".
This means there is a delay between the regular and the inferred spans being visible in your OpenTelemetry backend/UI.

Only platform threads are supported. Virtual threads are not supported and will not be profiled.

## Usage

This section describes the usage of this extension outside of an agent.
Add the following dependency to your project:

```
<dependency>
    <groupId>io.opentelemetry.contrib</groupId>
    <artifactId>opentelemetry-inferred-spans</artifactId>
    <version>{latest version}</version>
</dependency>
```

### Autoconfiguration

This extension supports [autoconfiguration](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure).

So if you are using an autoconfigured OpenTelemetry SDK, you'll only need to add this extension to your class path and configure it via system properties or environment variables:

| Property Name  / Environment Variable Name                                                    | Default                                                                                                                                                                                                                                                           | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|-----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| otel.inferred.spans.enabled <br/> OTEL_INFERRED_SPANS_ENABLED                                 | `false`                                                                                                                                                                                                                                                           | Enables the inferred spans feature.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| otel.inferred.spans.logging.enabled <br/> OTEL_INFERRED_SPANS_LOGGING_ENABLED                 | `true`                                                                                                                                                                                                                                                            | By default, async profiler prints warning messages about missing JVM symbols to standard output. Set this option to `true` to suppress such messages                                                                                                                                                                                                                                                                                                                                                                                                                |
| otel.inferred.spans.backup.diagnostic.files <br/> OTEL_INFERRED_SPANS_BACKUP_DIAGNOSTIC_FILES | `false`                                                                                                                                                                                                                                                           | Do not delete the temporary profiling files, can be used later to reproduce in case of issues.                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| otel.inferred.spans.safe.mode <br/> OTEL_INFERRED_SPANS_SAFE_MODE                             | `0`                                                                                                                                                                                                                                                               | Can be used for analysis: the Async Profiler's area that deals with recovering stack trace frames is known to be sensitive in some systems. It is used as a bit mask using values are between 0 and 31, where 0 enables all recovery attempts and 31 disables all five (corresponding 1, 2, 4, 8 and 16).                                                                                                                                                                                                                                                           |
| otel.inferred.spans.post.processing.enabled <br/> OTEL_INFERRED_SPANS_POST_PROCESSING_ENABLED | `true`                                                                                                                                                                                                                                                            | Can be used to test the effect of the async-profiler in isolation from the agent's post-processing.                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| otel.inferred.spans.sampling.interval <br/> OTEL_INFERRED_SPANS_SAMPLING_INTERVAL             | `50ms`                                                                                                                                                                                                                                                            | he frequency at which stack traces are gathered within a profiling session. The lower you set it, the more accurate the durations will be. This comes at the expense of higher overhead and more spans for potentially irrelevant operations. The minimal duration of a profiling-inferred span is the same as the value of this setting.                                                                                                                                                                                                                           |
| otel.inferred.spans.min.duration <br/> OTEL_INFERRED_SPANS_MIN_DURATION                       | `0ms`                                                                                                                                                                                                                                                             | The minimum duration of an inferred span. Note that the min duration is also implicitly set by the sampling interval. However, increasing the sampling interval also decreases the accuracy of the duration of inferred spans.                                                                                                                                                                                                                                                                                                                                      |
| otel.inferred.spans.included.classes <br/> OTEL_INFERRED_SPANS_INCLUDED_CLASSES               | `*`                                                                                                                                                                                                                                                               | If set, the agent will only create inferred spans for methods which match this list. Setting a value may slightly reduce overhead and can reduce clutter by only creating spans for the classes you are interested in. <br/> Example: `org.example.myapp.*`                                                                                                                                                                                                                                                                                                         |
| otel.inferred.spans.excluded.classes <br/> OTEL_INFERRED_SPANS_EXCLUDED_CLASSES               | `java.*`<br/>`javax.*`<br/>`sun.*`<br/>`com.sun.*`<br/>`jdk.*`<br/>`org.apache.tomcat.*`<br/>`org.apache.catalina.*`<br/>`org.apache.coyote.*`<br/>`org.jboss.as.*`<br/>`org.glassfish.*`<br/>`org.eclipse.jetty.*`<br/>`com.ibm.websphere.*`<br/>`io.undertow.*` | Excludes classes for which no profiler-inferred spans should be created.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| otel.inferred.spans.interval <br/> OTEL_INFERRED_SPANS_INTERVAL                               | `5s`                                                                                                                                                                                                                                                              | The interval at which profiling sessions should be started.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| otel.inferred.spans.duration <br/> OTEL_INFERRED_SPANS_DURATION                               | `5s`                                                                                                                                                                                                                                                              | The duration of a profiling session. For sampled transactions which fall within a profiling session (they start after and end before the session), so-called inferred spans will be created. They appear in the trace waterfall view like regular spans. <br/> NOTE: It is not recommended to set much higher durations as it may fill the activation events file and async-profiler's frame buffer. Warnings will be logged if the activation events file is full. If you want to have more profiling coverage, try decreasing `profiling_inferred_spans_interval` |
| otel.inferred.spans.lib.directory <br/> OTEL_INFERRED_SPANS_LIB_DIRECTORY                     | Defaults to the value of `java.io.tmpdir`                                                                                                                                                                                                                         | Profiling requires that the [async-profiler](https://github.com/async-profiler/async-profiler) shared library  is exported to a temporary location and loaded by the JVM. The partition backing this location must be executable, however in some server-hardened environments,  `noexec` may be set on the standard `/tmp` partition, leading to `java.lang.UnsatisfiedLinkError` errors. Set this property to an alternative directory (e.g. `/var/tmp`) to resolve this.                                                                                         |

### Manual SDK setup

If you manually set-up your `OpenTelemetrySDK`, you need to create and register an `InferredSpansProcessor` with your `TracerProvider`:

```java
InferredSpansProcessor inferredSpans = InferredSpansProcessor.builder()
  //.samplingInterval(Duration.ofMillis(10))
  .build();
SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
  .addSpanProcessor(inferredSpans)
  .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder()
    .setEndpoint("https://<clusterid>.apm.europe-west3.gcp.cloud.es.io:443")
    .addHeader("Authorization", "Bearer <secrettoken>>")
    .build()))
  .build();
inferredSpans.setTracerProvider(tracerProvider);
```

The `setTracerProvider(..)` call shown at the end may be omitted, in that case `GlobalOpenTelemetry` will be used for generating the inferred spans.

## Known issues

### Missing inferred spans

- After each profiling session, while the stack traces and activation events are processed, no traces are collected.
  - Under load, processing can take seconds; ~200ms are normal.
  - Log:
    ```
    DEBUG Processing {} stack traces
    ...
    DEBUG Processing traces took {}µs
    ```
- While stack traces are processed, activation events are still put into the ring buffer. However, they don't get processed. If, during this period, there are more activation events than the buffer can handle, we're losing activation events.
  - Log: `Could not add activation event to ring buffer as no slots are available`
  - Lost activation events can lead to orpaned call trees (lost end event), missing roots (lost start event) and messed up parent/child relationships (lost span activations/deactivations)
    Log:

  ```
  DEBUG Illegal state ...
  ```

- Under load, the activation event ring buffer can also get full
- The actual `otel.inferred.spans.sampling.interval` might be a bit lower. async-profiler aims to keep the interval relatively consistent but if there are too many threads actively running transactions or if there's a traffic spike, the interval can be lower.
- As a result of the above, some transactions don't contain inferred spans, even if their duration is longer than `otel.inferred.spans.sampling.interval`.
  Log:

  ```
  DEBUG Created no spans for thread {} (count={})
  ```

- There can be a race condition when putting activation events into the queue which leads to older events being in front of newer ones, like `1, 2, 4, 3, 5`. But this is quite infrequent and the consequences are similar to loosing that activation event or event without any consequence.
  Log:

  ```
  Timestamp of current activation event ({}) is lower than the one from the previous event ({})
  ```

### Incorrect parent/child relationships

#### Without workaround

Inferred span starts after actual span, even though it should be the parent
```
 ---------[inferred ]
 [actual]
^         ^         ^
```

Inferred span ends before actual span, even though it should be the parent

```
[inferred   ]------------
             [actual]
^           ^           ^
```

```
   -------[inferred ]-------                 [actual         ]
       [actual         ]          ->     -------[inferred ]-------
^         ^         ^         ^
```

Two consecutive method invocations are interpreted as one longer execution

```
[actual]   [actual]   ->  [--------  --------]
^          ^
```

#### With workaround

These are some tricky situations we have managed to find a workaround for.

##### Regular spans as a child of an inferred span

This is tricky as regular spans are sent to APM Server right after the event has ended.
Inferred spans are sent later - after the profiling session ends.

This is how the situation looks like without a workaround:

```
[transaction   ]     [transaction   ]
└─[inferred  ]    -> ├─[inferred  ]
  └─[actual]         └───[actual]
```

There are situations where the ordering is off as a result of that.

The workaround is that inferred spans have span-links with a special `is_child` attribute,
pointing to the regular spans they are the parent of.

##### Parent inferred span ends before child

Workaround: set end timestamp of inferred span to end timestamp of actual span.
```
[inferred ]--------         [inferred  -----]--
         [actual]       ->           [actual]
^         ^         ^
```

##### Parent inferred span starts after child

Workaround: set start timestamp of inferred span to start timestamp of actual span.
```
  --------[inferred ]          --[------inferred ]
    [actual ]           ->       [actual ]
^         ^         ^
```

#### Example

In this screenshot, we can see several problems at once
<img width="1137" alt="inferred spans issues" src="https://user-images.githubusercontent.com/2163464/75677751-710bd880-5c8c-11ea-8bd9-1c6d5f3268d5.png">

## Component owners

- [Jack Shirazi](https://github.com/jackshirazi), Elastic
- [Jonas Kunz](https://github.com/jonaskunz), Elastic
- [Sylvain Juge](https://github.com/sylvainjuge), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
