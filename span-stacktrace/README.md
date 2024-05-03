
# Span stacktrace capture

This module provides a `SpanProcessor` that captures the [`code.stacktrace`](https://opentelemetry.io/docs/specs/semconv/attributes-registry/code/).

Capturing the stack trace is an expensive operation and does not provide any value on short-lived spans.
As a consequence it should only be used when the span duration is known, thus on span end.

However, the current SDK API does not allow to modify span attributes on span end, so we have to
introduce other components to make it work as expected.

## Usage

This extension does not support autoconfiguration because it requires strict ordering of span
processors to ensure it's executed between the last span processor and the span exporter.

As a consequence you have to use [Manual SDK setup](#manual-sdk-setup)
section below to configure it.

### Manual SDK setup

Here is an example registration of `StackTraceSpanProcessor` to capture stack trace for all
the spans that have a duration >= 1000 ns. The spans that have an `ignorespan` string attribute
will be ignored.

```java
InMemorySpanExporter spansExporter = InMemorySpanExporter.create();
SpanProcessor exportProcessor = SimpleSpanProcessor.create(spansExporter);

Predicate<ReadableSpan> filterPredicate = readableSpan -> {
  if(readableSpan.getAttribute(AttributeKey.stringKey("ignorespan")) != null){
    return false;
  }
  return true;
};
SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
    .addSpanProcessor(new StackTraceSpanProcessor(exportProcessor, 1000, filterPredicate))
    .build();

OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
```

## Component owners

- [Jack Shirazi](https://github.com/jackshirazi), Elastic
- [Jonas Kunz](https://github.com/jonaskunz), Elastic
- [Sylvain Juge](https://github.com/sylvainjuge), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
