# Disk buffering

This module provides an abstraction
named [SignalStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/SignalStorage.java),
as well as default implementations for each signal type that allow writing signals to disk and
reading them later.

For a more detailed information on how the whole process works, take a look at
the [DESIGN.md](DESIGN.md) file.

## Default implementation usage

The default implementations are the following:

* [FileSpanStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/impl/FileSpanStorage.java)
* [FileLogRecordStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/impl/FileLogRecordStorage.java)
* [FileMetricStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/impl/FileMetricStorage.java)

### Set up

We need to create a signal storage object per signal type to start writing signals to disk. Each
`File*Storage` implementation has a `create()` function that receives:

* A File directory to store the signal files. Note that each signal storage object must have a
  dedicated directory to work properly.
* (Optional) a configuration object.

The available configuration parameters are the following:

* Max file size, defaults to 1MB.
* Max folder size, defaults to 10MB.
* Max age for file writing. It sets the time window where a file can get signals appended to it.
  Defaults to 30 seconds.
* Min age for file reading. It sets the time to wait before starting to read from a file after
  its creation. Defaults to 33 seconds. It must be greater that the max age for file writing.
* Max age for file reading. After that time passes, the file will be considered stale and will be
  removed when new files are created. No more data will be read from a file past this time. Defaults
  to 18 hours.

```java
// Root dir
File rootDir = new File("/some/root");

// Setting up span storage
SignalStorage.Span spanStorage = FileSpanStorage.create(new File(rootDir, "spans"));

// Setting up metric storage
SignalStorage.Metric metricStorage = FileMetricStorage.create(new File(rootDir, "metrics"));

// Setting up log storage
SignalStorage.LogRecord logStorage = FileLogRecordStorage.create(new File(rootDir, "logs"));
```

### Storing data

While you could manually call your `SignalStorage.write(items)` function, disk buffering
provides convenience exporters that you can use in your OpenTelemetry's instance, so
that all signals are automatically stored as they are created.

* For a span storage, use
  a [SpanToDiskExporter](src/main/java/io/opentelemetry/contrib/disk/buffering/exporters/SpanToDiskExporter.java).
* For a log storage, use
  a [LogRecordToDiskExporter](src/main/java/io/opentelemetry/contrib/disk/buffering/exporters/LogRecordToDiskExporter.java).
* For a metric storage, use
  a [MetricToDiskExporter](src/main/java/io/opentelemetry/contrib/disk/buffering/exporters/MetricToDiskExporter.java).

Each will wrap a signal storage for its respective signal type, as well as an optional callback
to notify when it succeeds, fails, and gets shutdown.

```java
// Setting up span to disk exporter
SpanToDiskExporter spanToDiskExporter =
    SpanToDiskExporter.builder(spanStorage).setExporterCallback(spanCallback).build();
// Setting up metric to disk
MetricToDiskExporter metricToDiskExporter =
    MetricToDiskExporter.builder(metricStorage).setExporterCallback(metricCallback).build();
// Setting up log to disk exporter
LogRecordToDiskExporter logToDiskExporter =
    LogRecordToDiskExporter.builder(logStorage).setExporterCallback(logCallback).build();

// Using exporters in your OpenTelemetry instance.
OpenTelemetry openTelemetry =
    OpenTelemetrySdk.builder()
        // Using span to disk exporter
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanToDiskExporter).build())
                .build())
        // Using log to disk exporter
        .setLoggerProvider(
            SdkLoggerProvider.builder()
                .addLogRecordProcessor(
                    BatchLogRecordProcessor.builder(logToDiskExporter).build())
                .build())
        // Using metric to disk exporter
        .setMeterProvider(
            SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.create(metricToDiskExporter))
                .build())
        .build();
```

Now when creating signals using your `OpenTelemetry` instance, those will get stored in disk.

### Reading data

In order to read data, we can iterate through our signal storage objects and then forward them to
a network exporter, as shown in the example for spans below.

```java
/**
 * Example of reading and exporting spans from disk.
 *
 * @return true, if the exporting was successful, false, if it needs to be retried
 */
public boolean exportSpansFromDisk(SpanExporter networkExporter, long timeout) {
  Iterator<Collection<SpanData>> spansIterator = spanStorage.iterator();
  while (spansIterator.hasNext()) {
    CompletableResultCode resultCode = networkExporter.export(spansIterator.next());
    resultCode.join(timeout, TimeUnit.MILLISECONDS);

    if (resultCode.isSuccess()) {
      spansIterator.remove(); // Remove the current item, as it was successfully exported to the network
    } else {
      logger.trace("Error while exporting", resultCode.getFailureThrowable());
      // The iteration should be aborted here to avoid consuming batches, which were not exported successfully
      return false;
    }
  }
  logger.trace("Finished exporting");
  return true;
}
```

### Deleting data

There are 2 ways to delete data previously stored by calling the `SignalStorage.write(items)`
function:

* During iteration. This is done by calling `Iterator.remove()` as shown in the example above. This
  will remove the last item retrieved from the iterator. Ideally, this should be done after the data
  has been successfully exported to the network.
* Clearing all data at once by calling `SignalStorage.clear()`.

### More details on the writing and reading processes

Both the writing and reading processes can run in parallel as they won't overlap
because each is supposed to happen in different files. We ensure that reader and writer don't
accidentally meet in the same file by using the configurable parameters. These parameters set
non-overlapping time frames for each action to be done on a single file at a time. On top of that,
there's a mechanism in place to avoid overlapping on edge cases where the time frames ended but the
resources haven't been released. For that mechanism to work properly, this tool assumes that both
the reading and the writing actions are executed within the same application process.

## Component owners

- [Cesar Munoz](https://github.com/LikeTheSalad), Elastic
- [Gregor Zeitlinger](https://github.com/zeitlinger), Grafana

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
