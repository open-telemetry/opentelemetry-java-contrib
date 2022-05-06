# Micrometer MeterProvider

This utility provides an implementation of `MeterProvider` which wraps a Micrometer `MeterRegistry`
and delegates the reporting of all metrics through Micrometer.  This enables projects which already
rely on Micrometer and cannot currently migrate to OpenTelemetry Metrics to be able to report on
metrics that are reported through the OpenTelemetry Metrics API.

### Usage

TBD

## Component owners

- [Justin Spindler](https://github.com/HaloFour), Comcast

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
