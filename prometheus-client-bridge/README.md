# Prometheus client bridge

This module can be used to bridge OpenTelemetry metrics into the `prometheus-simpleclient` library.

Currently only registers with the CollectorRegistry's `defaultRegistry`.

* Build it with `./gradlew :prometheus-simpleclient-bridge:build`

## Usage

```
sdkMeterProvider.registerMetricReader(PrometheusCollector.create());
```

## Component owners

- [John Watson](https://github.com/jkwatson), Verta.ai

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
