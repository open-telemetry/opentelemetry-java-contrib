# Processors

## Interceptable exporters

This module provides tools to intercept and process signals globally:

* `InterceptableSpanExporter`
* `InterceptableMetricExporter`
* `InterceptableLogRecordExporter`

## Event to SpanEvent Bridge

`EventToSpanEventBridge` is a `LogRecordProcessor` which records events (i.e. log records with an `event.name` attribute) as span events for the current span if:

* The log record has a valid span context
* `Span.current()` returns a span where `Span.isRecording()` is true

For details of how the event log record is translated to span event, see [EventToSpanEventBridge Javadoc](./src/main/java/io/opentelemetry/contrib/eventbridge/EventToSpanEventBridge.java).

`EventToSpanEventBridge` can be referenced in [declarative configuration](https://opentelemetry.io/docs/languages/java/configuration/#declarative-configuration) as follows:

```yaml
# Configure tracer provider as usual, omitted for brevity
tracer_provider: ...

logger_provider:
  processors:
      - event_to_span_event_bridge:
```

## Filtering Log Processor

`FilteringLogRecordProcessor` is a `LogRecordProcessor` that only keep logs  based on a predicate

## Filtering Span Exporter

`FilteringSpanExporter` is a `SpanExporter` wrapper that filters spans within each export batch before delegating to the underlying exporter. Filtering is composable via two interfaces:

- `SpanFilter` - evaluates individual spans (e.g., error status, slow duration)
- `TraceFilter` - evaluates all spans belonging to a trace within the batch (e.g., overall trace wall-clock duration)

Within a batch, if any `SpanFilter` matches any span or any `TraceFilter` matches a trace's span group, all spans sharing that trace ID in the batch are exported together.

**Note:** Filtering decisions are scoped to a single `export()` call. Spans from the same trace arriving in different batches are evaluated independently, so a trace split across batches may be partially exported.

Built-in filters:

- `ErrorSpanFilter` - matches spans with error status
- `DurationSpanFilter` - matches spans exceeding a duration threshold
- `TraceDurationFilter` - matches when a trace's wall-clock duration (max end - min start) in the batch exceeds a threshold

Usage:

```java
SpanExporter delegate = OtlpGrpcSpanExporter.getDefault();

// Export spans whose batch-colocated trace has errors, individual spans > 2s, or trace duration > 10s
SpanExporter filtering = new FilteringSpanExporter(
    delegate,
    Arrays.asList(new ErrorSpanFilter(), new DurationSpanFilter(Duration.ofSeconds(2))),
    Collections.singletonList(new TraceDurationFilter(Duration.ofSeconds(10))));

// Custom filters
SpanFilter nameFilter = span -> span.getName().contains("important");
SpanExporter custom = new FilteringSpanExporter(
    delegate,
    Collections.singletonList(nameFilter),
    Collections.emptyList());

// Optionally pass a Meter to emit dropped-span metrics
Meter meter = openTelemetry.getMeter("my-service");
SpanExporter withMetrics = new FilteringSpanExporter(
    delegate,
    Arrays.asList(new ErrorSpanFilter(), new DurationSpanFilter(Duration.ofSeconds(2))),
    Collections.singletonList(new TraceDurationFilter(Duration.ofSeconds(10))),
    meter);
```

## Component owners

- [Cesar Munoz](https://github.com/LikeTheSalad), Elastic
- [Jack Berg](https://github.com/jack-berg), New Relic
- [Jason Plumb](https://github.com/breedx-splk), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
