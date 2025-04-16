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

## Component owners

- [Cesar Munoz](https://github.com/LikeTheSalad), Elastic
- [Jack Berg](https://github.com/jack-berg), New Relic
- [Jason Plumb](https://github.com/breedx-splk), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
