# Micrometer MeterProvider

This utility provides an implementation of `MeterProvider` which wraps a Micrometer `MeterRegistry`
and delegates the reporting of all metrics through Micrometer.  This enables projects which already
rely on Micrometer and cannot currently migrate to OpenTelemetry Metrics to be able to report on
metrics that are reported through the OpenTelemetry Metrics API.

### Usage

Create the `MicrometerMeterProvider` passing an existing instance of `MeterRegistry`.  Then you can
use the OpenTelemetry Metrics `MeterProvider` API to create instruments.

```java
MeterRegistry meterRegistry = ...;

// create the meter provider
MeterProvider meterProvider = new MicrometerMeterProvider(meterRegistry);
Meter meter = meterProvider.get("my-app");

// create an instrument
LongCounter counter = meter.counterBuilder("my.counter")
    .build();

// record metrics
count.add(1, Attributes.of(AttributeKey.stringKey("key"), "value"));
```

**Note**: Instruments in OpenTelemetry are created without tags, which are reported with each
measurement.  But tags are required to create Micrometer metrics.  Because of this difference the
adapter must listen for when measurements are being read by the `MeterRegistry` in order to call
callbacks registered for observable metrics in order to create the Micrometer meters on demand.

## Component owners

- [Justin Spindler](https://github.com/HaloFour), Comcast

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
