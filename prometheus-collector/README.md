# Prometheus Collector

This module is useful for exposing OpenTelemetry metrics to a Prometheus registry.

Currently only registers with the Prometheus `defaultRegistry`.

* Build it with `./gradlew :prometheus-collector:build`

### Usage

```
sdkMeterProvider.registerMetricReader(PrometheusCollector.create());
```

## Component owners

- [John Watson](https://github.com/jkwatson), Verta.ai

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
