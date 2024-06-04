# OpenTelemetry Baggage Span Processor

The BaggageSpanProcessor reads entries stored in Baggage from the parent context
and adds the baggage keys and values to the `Span` as attributes on start.

Add this span processor to a tracer provider.

Warning!

To repeat: a consequence of adding data to Baggage is that the keys and values
will appear in all outgoing trace context headers from the application.

Do not put sensitive information in Baggage.

## Usage

Add the span processor when configuring the tracer provider.

To configure the span processor to copy all baggage entries during configuration:

```java
import io.opentelemetry.contrib.baggage.processor;
// ...

Tracer tracer = SdkTracerProvider.builder()
  .addSpanProcessor(new BaggageSpanProcessor(BaggageSpanProcessor.allowAllBaggageKeys))
  .build()
```

Alternatively, you can provide a custom baggage key predicate to select which baggage keys you want to copy.

For example, to only copy baggage entries that start with `my-key`:

```java
new BaggageSpanProcessor(baggageKey -> baggageKey.startsWith("my-key"))
```

For example, to only copy baggage entries that match the regex `^key.+`:

```java
Pattern pattern = Pattern.compile("^key.+");
new BaggageSpanProcessor(baggageKey -> pattern.matcher(baggageKey).matches())
```

## Component owners

- [Mike Golsmith](https://github.com/MikeGoldsmith), Honeycomb

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
