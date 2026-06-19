# Prometheus client bridge

[![Maven](https://badges.mvnrepository.com/badge/io.opentelemetry.contrib/opentelemetry-prometheus-client-bridge/badge.svg?label=Maven&color=orange)](https://mvnrepository.com/artifact/io.opentelemetry.contrib/opentelemetry-prometheus-client-bridge)

This module can be used to bridge OpenTelemetry metrics into the `prometheus-simpleclient` library.

Currently only registers with the CollectorRegistry's `defaultRegistry`.

* Build it with `./gradlew :prometheus-simpleclient-bridge:build`

## Usage

```text
sdkMeterProvider.registerMetricReader(PrometheusCollector.create());
```

## Component owners

* [John Watson](https://github.com/jkwatson), Verta.ai

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
