## Contributing

Pull requests for bug fixes are always welcome!

Before submitting new features or changes to current functionality, it is recommended to first
[open an issue](https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/new)
and discuss your ideas or propose the changes you wish to make.

### Building

#### Snapshot builds

For developers testing code changes before a release is complete, there are
snapshot builds of the `main` branch. They are available from
the Sonatype OSS snapshots repository at `https://oss.sonatype.org/content/repositories/snapshots/`
([browse](https://oss.sonatype.org/content/repositories/snapshots/io/opentelemetry/contrib/))

#### Building from source

Building requires Java 11:

```bash
java -version
```

```bash
./gradlew assemble
```

### Style guide

See the [Style guide](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/style-guideline.md)
from the opentelemetry-java-instrumentation repository.
