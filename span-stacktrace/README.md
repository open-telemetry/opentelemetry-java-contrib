
# Span stacktrace capture

This module provides a `SpanProcessor` that captures the [`code.stacktrace`](https://opentelemetry.io/docs/specs/semconv/attributes-registry/code/).

Capturing the stack trace is an expensive operation and does not provide any value on short-lived spans.
As a consequence it should only be used when the span duration is known, thus on span end.

## Usage and configuration

This extension supports autoconfiguration.

`otel.java.experimental.span-stacktrace.min.duration`

- allows to configure the minimal duration for which spans have a stacktrace captured
- defaults to 5ms
- a value of zero will include all spans
- a negative value will disable the feature

`otel.java.experimental.span-stacktrace.filter`

- allows to filter spans to be excluded from stacktrace capture
- defaults to include all spans.
- value is the class name of a class implementing `java.util.function.Predicate<ReadableSpan>`
- filter class must be publicly accessible and provide a no-arg constructor

## Component owners

- [Jack Shirazi](https://github.com/jackshirazi), Elastic
- [Jonas Kunz](https://github.com/jonaskunz), Elastic
- [Sylvain Juge](https://github.com/sylvainjuge), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
