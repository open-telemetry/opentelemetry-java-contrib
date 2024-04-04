
# Span stacktrace capture

This module provides a `SpanProcessor` that captures the [`code.stacktrace`](https://opentelemetry.io/docs/specs/semconv/attributes-registry/code/).

Capturing the stack trace is an expensive operation and does not provide any value on short-lived spans.
As a consequence it should only be used when the span duration is known, thus on span end.

However, the current SDK API does not allow to modify span attributes on span end, so we have to
introduce other components to make it work as expected.

## Component owners

- [Jack Shirazi](https://github.com/jackshirazi), Elastic
- [Jonas Kunz](https://github.com/jonaskunz), Elastic
- [Sylvain Juge](https://github.com/sylvainjuge), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
