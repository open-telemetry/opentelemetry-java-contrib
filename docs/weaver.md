# Weaver Code Generation

This project uses [OpenTelemetry Weaver](https://github.com/open-telemetry/weaver) to generate code, documentation, and configuration
from semantic convention models. As of now, this is only used for metrics in select modules, but use
cases are likely to expand in the future.

## Overview

[Weaver](https://github.com/open-telemetry/weaver) is a tool that generates consistent code across
OpenTelemetry implementations by processing semantic convention models defined in YAML format.
The `otel.weaver-conventions` Gradle plugin automates this process.

## Using the Weaver Plugin

### Prerequisites

- **Docker** must be installed and running

### Applying to Your Module

Add the plugin to your module's `build.gradle.kts`:

```kotlin
plugins {
  id("otel.weaver-conventions")
}
```

The plugin automatically detects modules with a `model/` directory and registers code generation
tasks.

### Configuring Java Output Package

**REQUIRED:** You must explicitly configure the Java output package path:

```kotlin
otelWeaver {
  javaOutputPackage.set("io/opentelemetry/ibm/mq/metrics")
}
```

This determines where generated Java code will be placed under `src/main/java/`.

**Important:**
- Use forward slashes (`/`) for the path, not dots or backslashes
- The path should match your module's package structure
- Generated code will be placed in `src/main/java/{your-path}/`

**Example:**
```kotlin
// For module "io.opentelemetry.contrib.ibm-mq-metrics"
otelWeaver {
  javaOutputPackage.set("io/opentelemetry/ibm/mq/metrics")
}
// Generates to: src/main/java/io/opentelemetry/ibm/mq/metrics/
```

### Module Structure

```
your-module/
├── build.gradle.kts
├── model/
│   ├── registry_manifest.yaml      # Weaver registry manifest
│   └── metrics.yaml                # Your semantic conventions
├── templates/                      # (Optional) Custom code generation templates
├── docs/
│   └── metrics.md                  # Generated documentation
```

## Available Tasks

### Generate All Artifacts

```bash
./gradlew :your-module:weaverGenerate
```

Generates Java code, markdown documentation, and YAML configuration.

### Generate Java Code Only

```bash
./gradlew :your-module:weaverGenerateJava
```

- Outputs to `src/main/java/{configured-package}/`
- **Automatically formats** generated code with `spotlessJavaApply`

### Generate Documentation

```bash
./gradlew :your-module:weaverGenerateDocs
```

Generates markdown documentation to `docs/metrics.md`.

### Generate Configuration

```bash
./gradlew :your-module:weaverGenerateYaml
```

Generates a YAML configuration template to `config.yml` in the module root.

### Validate Model

```bash
./gradlew :your-module:weaverCheck
```

Validates the weaver model for errors without generating code.

## Example

The `ibm-mq-metrics` module demonstrates weaver usage:

```kotlin
// ibm-mq-metrics/build.gradle.kts
plugins {
  id("otel.weaver-conventions")
}

otelJava.moduleName.set("io.opentelemetry.contrib.ibm-mq-metrics")

otelWeaver {
  javaOutputPackage.set("io/opentelemetry/ibm/mq/metrics")
}
// Generates to: src/main/java/io/opentelemetry/ibm/mq/metrics/
```

## Resources

- [OpenTelemetry Weaver Documentation](https://github.com/open-telemetry/weaver)
- [Semantic Conventions](https://github.com/open-telemetry/semantic-conventions)
- [IBM MQ Metrics Example](../ibm-mq-metrics/)
