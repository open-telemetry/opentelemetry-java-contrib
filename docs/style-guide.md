# Style Guide

This project follows the
[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

## Code Formatting

### Auto-formatting

The build will fail if source code is not formatted according to Google Java Style.

Run the following command to reformat all files:

```bash
./gradlew spotlessApply
```

For IntelliJ users, an `.editorconfig` file is provided that IntelliJ will automatically use to
adjust code formatting settings. However, it does not support all required rules, so you may still
need to run `./gradlew spotlessApply` periodically.

### Static imports

Consider statically importing the following commonly used methods and constants:

- **Test methods**
  - `io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions` (assertThat, satisfies, etc.)
  - `org.assertj.core.api.Assertions.*` (assertThat, assertThatThrownBy, entry, etc.)
  - `org.mockito.Mockito.*` (when, mock, verify, times, etc.)
  - `org.mockito.ArgumentMatchers.*` (any, eq, anyLong, etc.)
- **Utility methods**
  - `io.opentelemetry.api.common.AttributeKey.*` (stringKey, longKey, etc.)
  - `java.util.Arrays.*` (asList, stream, etc.)
  - `java.util.Collections.*` (singletonList, emptyList, emptyMap, emptySet, etc.)
  - `java.util.stream.Collectors.*` (toList, toMap, toSet, etc.)
  - `java.util.Objects.requireNonNull`
  - `java.util.logging.Level.*` (FINE, INFO, WARNING, etc.)
  - `java.nio.charset.StandardCharsets.*` (UTF_8, etc.)
- **Time unit constants**
  - `java.util.concurrent.TimeUnit.*` (SECONDS, etc.)
- **OpenTelemetry semantic convention constants**
  - All constants under `io.opentelemetry.semconv.**`, except for `io.opentelemetry.semconv.SchemaUrls.*` constants.

### Class organization

Prefer this order:

- Static fields (final before non-final)
- Instance fields (final before non-final)
- Constructors
- Methods
- Nested classes

**Method ordering**: Place calling methods above the methods they call. For example, place private
methods below the non-private methods that use them.

**Static utility classes**: Place the private constructor (used to prevent instantiation) after all
methods.

## Java Language Conventions

### Visibility modifiers

Follow the principle of minimal necessary visibility. Use the most restrictive access modifier that
still allows the code to function correctly.

### Package conventions

Classes in `.internal` packages are not considered public API and may change without notice. These
packages contain implementation details that should not be used by external consumers.

- Use `.internal` packages for implementation classes that need to be public within the module but
  should not be used externally
- Try to avoid referencing `.internal` classes from other modules

### `final` keyword usage

Public non-internal classes should be declared `final` where possible.

Methods should only be declared `final` if they are in public non-internal non-final classes.

Fields should be declared `final` where possible.

Method parameters and local variables should never be declared `final`.

### `@Nullable` annotation usage

**Note: This section is aspirational and may not reflect the current codebase.**

Annotate all parameters and fields that can be `null` with `@Nullable` (specifically
`javax.annotation.Nullable`, which is included by the `otel.java-conventions` Gradle plugin as a
`compileOnly` dependency).

`@NonNull` is unnecessary as it is the default.

**Defensive programming**: Public APIs should still check for `null` parameters even if not
annotated with `@Nullable`. Internal APIs do not need these checks.

**Enforcement**: Use the `otel.errorprone-conventions` Gradle plugin in all modules:

```kotlin
plugins {
  id("otel.errorprone-conventions")
}
```

### `Optional` usage

Following the reasoning from
[Writing a Java library with better experience (slide 12)](https://speakerdeck.com/trustin/writing-a-java-library-with-better-experience?slide=12),
`java.util.Optional` usage is kept to a minimum.

**Guidelines**:

- `Optional` shouldn't appear in public API signatures
- Avoid `Optional` on the hot path (instrumentation code), unless the instrumented library uses it

## Tooling conventions

### AssertJ

Prefer AssertJ assertions over JUnit assertions (assertEquals, assertTrue, etc.) for better error
messages.

### JUnit

Test classes and test methods should generally be package-protected (no explicit visibility
modifier) rather than `public`. This follows the principle of minimal necessary visibility and is
sufficient for JUnit to discover and execute tests.

### AutoService

Use the `@AutoService` annotation when implementing SPI interfaces. This automatically generates the
necessary `META-INF/services/` files at compile time, eliminating the need to manually create and
maintain service registration files.

```java
@AutoService(AutoConfigurationCustomizerProvider.class)
public class MyCustomizerProvider implements AutoConfigurationCustomizerProvider {
  // implementation
}
```

### Gradle

- Use Kotlin instead of Groovy for build scripts
- Plugin versions should be specified in `settings.gradle.kts`, not in individual modules
- All modules should use `plugins { id("otel.java-conventions") }`
- Set module names with `otelJava.moduleName.set("io.opentelemetry.contrib.mymodule")`

## Configuration

- Use `otel.` prefix for all configuration property keys
- Read configuration via the `ConfigProperties` interface
- Provide sensible defaults and document all options
- Validate configuration early with clear error messages

## Performance

Avoid allocations on the hot path (instrumentation code) whenever possible. This includes `Iterator`
allocations from collections; note that `for (SomeType t : plainJavaArray)` does not allocate an
iterator object.

Non-allocating Stream API usage on the hot path is acceptable but may not fit the surrounding code
style; this is a judgment call. Some Stream APIs make efficient allocation difficult (e.g.,
`collect` with pre-sized sink data structures involves convoluted `Supplier` code, or lambdas passed
to `forEach` may be capturing/allocating lambdas).

## Documentation

### Component README files

- Include a component owners section in each module's README
- Document configuration options with examples

### Deprecation and breaking changes

Breaking changes are allowed in unstable modules (published with `-alpha` version suffix).

1. Mark APIs with `@Deprecated` and a removal timeline (there must be at least one release with the
   API marked as deprecated before removing it)
2. Document the replacement in Javadoc with `@deprecated` tag
3. Note the migration path for breaking changes under a "Migration notes" section of CHANGELOG.md
   (create this section at the top of the Unreleased section if not already present)
