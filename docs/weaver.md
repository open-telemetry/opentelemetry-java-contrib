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

By default, the plugin derives the Java output package path from your `otelJava.moduleName`:

```kotlin
otelJava.moduleName.set("io.opentelemetry.contrib.ibm-mq-metrics")
// Generates to: src/main/java/io/opentelemetry/ibm/mq/metrics/
```

**Derivation logic:**
1. Remove `io.opentelemetry.contrib.` or `io.opentelemetry.` prefix
2. Convert dots (`.`) and hyphens (`-`) to forward slashes (`/`)
3. Prepend `io/opentelemetry/`

**Example transformations:**
- `io.opentelemetry.contrib.ibm-mq-metrics` → `io/opentelemetry/ibm/mq/metrics`
- `io.opentelemetry.contrib.my-module` → `io/opentelemetry/my/module`

To override the default, configure the extension explicitly:

```kotlin
otelWeaver {
  javaOutputPackage.set("io/opentelemetry/custom/path/metrics")
}
```

**Note:** Use forward slashes (`/`) for the path, not dots or backslashes.

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

- Outputs to `src/main/java/{inferred-package}/`
- **Automatically formats** generated code with `spotlessJavaApply`
- Runs before `compileJava` task

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

The `ibm-mq-metrics` module demonstrates weaver usage. The plugin automatically derives the output
path from the module name:

```kotlin
// ibm-mq-metrics/build.gradle.kts
otelJava.moduleName.set("io.opentelemetry.contrib.ibm-mq-metrics")
// Result: Generates to src/main/java/io/opentelemetry/ibm/mq/metrics/
```

To use a custom path:

```kotlin
otelWeaver {
  javaOutputPackage.set("io/opentelemetry/custom/metrics")
}
```

## Resources

- [OpenTelemetry Weaver Documentation](https://github.com/open-telemetry/weaver)
- [Semantic Conventions](https://github.com/open-telemetry/semantic-conventions)
- [IBM MQ Metrics Example](../ibm-mq-metrics/)
