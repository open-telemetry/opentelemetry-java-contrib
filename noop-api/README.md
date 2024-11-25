# OpenTelemetry Noop API

An implementation of `OpenTelemetry` that is completely no-op. Unlike `OpenTelemetry#noop()`, this
implementation does not support in-process context propagation at all. This means that no objects
are allocated nor {@link ThreadLocal}s used in an application using this implementation.

## Component owners

- [Jack Berg](https://github.com/jack-berg), New Relic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
