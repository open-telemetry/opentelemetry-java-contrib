# Copilot Instructions for OpenTelemetry Java Contrib

This repository provides observability instrumentation for Java applications.

## Code Review Priorities

### Style Guide Compliance

**PRIORITY**: Verify that all code changes follow the [Style Guide](../docs/style-guide.md). Check:

- Code formatting (auto-formatting, static imports, class organization)
- Java language conventions (`final` usage, `@Nullable` annotations, `Optional` usage)
- Performance constraints (hot path allocations)
- Implementation patterns (SPI registration, configuration conventions)
- Gradle conventions (Kotlin DSL, plugin usage, module naming)
- Documentation standards (README files, deprecation processes)

### Critical Areas

- **Public APIs**: Changes affect downstream users and require careful review
- **Performance**: Instrumentation must have minimal overhead
- **Thread Safety**: Ensure safe concurrent access patterns
- **Memory Management**: Prevent leaks and excessive allocations

### Quality Standards

- Proper error handling with appropriate logging levels
- OpenTelemetry specification and semantic convention compliance
- Resource cleanup and lifecycle management
- Comprehensive unit tests for new functionality
