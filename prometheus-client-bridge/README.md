# Prometheus client bridge

[![Maven](https://img.shields.io/maven-central/v/io.opentelemetry.contrib/opentelemetry-prometheus-client-bridge?label=Maven&color=orange)](https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-prometheus-client-bridge)

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
