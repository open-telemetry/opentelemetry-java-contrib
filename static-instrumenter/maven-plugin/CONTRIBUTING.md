# Testing

## E2E test
Maven plugin has an E2E test `OpenTelemetryInstrumenterMojoTest`. This particular test uses sample HTTP test app, that can be built out of `test-app` module.

## Unit tests
Various unit tests use a bunch of JAR files to validate file operations. These JARs have been created manually and are reflected in test assertions. In case of a need, all of them can be changed along with the asserted data.