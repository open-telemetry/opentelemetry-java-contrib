# zstd Compressor

A [zstd](https://en.wikipedia.org/wiki/Zstd) implementation of [Compressor](https://github.com/open-telemetry/opentelemetry-java/blob/d9f9812d4375a4229caff43bd681c50b7a45776a/exporters/common/src/main/java/io/opentelemetry/exporter/internal/compression/Compressor.java) and [CompressorProvider](https://github.com/open-telemetry/opentelemetry-java/blob/d9f9812d4375a4229caff43bd681c50b7a45776a/exporters/common/src/main/java/io/opentelemetry/exporter/internal/compression/CompressorProvider.java) based on [luben/zstd-jni](https://github.com/luben/zstd-jni).

This enables zstd compression with [opentelemetry-java's](https://github.com/open-telemetry/opentelemetry-java) [OTLP exporters](https://opentelemetry.io/docs/instrumentation/java/exporters/#otlp).

## Usage

Add dependency, replacing `{{version}}` with the latest release version.

**Maven:**

```xml
<dependency>
  <groupId>io.opentelemetry.contrib</groupId>
  <artifactId>opentelemetry-compressor-zstd</artifactId>
  <version>{{version}}/version>
</dependency>
```

**Gradle:**

```groovy
dependencies {
  implementation "io.opentelemetry.contrib:opentelemetry-compressor-zstd:{{version}}"
}
```

If programmatically configuring the exporter:

```java
// same pattern applies to OtlpHttpMetricExporter, OtlpHttpSpanExporter, and the gRPC variants
OtlpHttpLogRecordExporter.builder()
    .setCompression("zstd")
    // ...additional configuration omitted for brevity
    .build()
```

If using [autoconfigure](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure):

```shell
export OTEL_EXPORTER_OTLP_COMPRESSION=zstd
```

## Component owners

- [Jack Berg](https://github.com/jack-berg), New Relic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
