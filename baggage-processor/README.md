# OpenTelemetry Baggage Span Processor

The `BaggageSpanProcessor` and `BaggageLogRecordPRocessor` read entries stored in Baggage from the 
parent context and adds the baggage keys and values to the `Span`, respectively `LogRecord`, as 
attributes on start, respectively emit.

Add this span and log processors to the tracer and logger providers.

Warning!

To repeat: a consequence of adding data to Baggage is that the keys and values
will appear in all outgoing trace and log context headers from the application.

Do not put sensitive information in Baggage.

## Usage

Add the span and log processor when configuring the tracer and logger providers.

To configure the span and log processors to copy all baggage entries during configuration:

```java
import io.opentelemetry.contrib.baggage.processor;
// ...

TracerProvider tracerProvider = SdkTracerProvider.builder()
    .addSpanProcessor(BaggageSpanProcessor.allowAllBaggageKeys())
    .build();

LoggerProvider loggerProvider = SdkLoggerProvider.builder()
    .addLogRecordProcessor(BaggageLogRecordProcessor.allowAllBaggageKeys())
    .build();
```

Alternatively, you can provide a custom baggage key predicate to select which baggage keys you want to copy.

For example, to only copy baggage entries that start with `my-key`:

```java
new BaggageSpanProcessor(baggageKey -> baggageKey.startsWith("my-key"));
new BaggageLogRecordProcessor(baggageKey -> baggageKey.startsWith("my-key"));
```

For example, to only copy baggage entries that match the regex `^key.+`:

```java
Pattern pattern = Pattern.compile("^key.+");
new BaggageSpanProcessor(baggageKey -> pattern.matcher(baggageKey).matches());
new BaggageLogRecordProcessor(baggageKey -> pattern.matcher(baggageKey).matches());
```

## Usage with SDK auto-configuration

If you are using the OpenTelemetry SDK auto-configuration, you can add the span and log baggage 
processors through configuration.

| Property                                                           | Description                                                                                       |
|--------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| `otel.java.experimental.span-attributes.copy-from-baggage.include` | Add baggage entries as span attributes, e.g. `key1,key2` or `*` to add all baggage items as keys. |
| `otel.java.experimental.log-attributes.copy-from-baggage.include`  | Add baggage entries as log attributes, e.g. `key1,key2` or `*` to add all baggage items as keys.  |

## Component owners

- [Mike Golsmith](https://github.com/MikeGoldsmith), Honeycomb
- [Gregor Zeitlinger](https://github.com/zeitlinger), Grafana

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
