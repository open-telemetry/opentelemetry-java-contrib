# Disk buffering

[![Maven](https://badges.mvnrepository.com/badge/io.opentelemetry.contrib/opentelemetry-disk-buffering/badge.svg?label=Maven&color=orange)](https://mvnrepository.com/artifact/io.opentelemetry.contrib/opentelemetry-disk-buffering)

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
* Min age for file reading. Optional delay applied before a finalized file becomes eligible for
  reading. Defaults to `0`. With the rename-on-close design the writer and reader are already
  synchronized via atomic file rename, so this knob is no longer required for correctness; set it
  if you want to give writers more time to accumulate larger batches before they're consumed.
* Max age for file reading. After that time passes, the file will be considered stale and will be
  removed when new files are created. No more data will be read from a file past this time. Defaults
  to 18 hours.
* Delete items on iteration. Controls whether items are automatically removed from disk as the
  iterator advances. Defaults to `true`. See [Deleting data](#deleting-data) for more details.

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
a network exporter. By default, items are automatically deleted from disk as the iterator advances,
so a simple iteration is all that's needed:

```java
/**
 * Example of reading and exporting spans from disk.
 *
 * @return true, if the exporting was successful, false, if it needs to be retried
 */
public boolean exportSpansFromDisk(SpanExporter networkExporter, long timeout) {
    for (Collection<SpanData> spanData : spanStorage) {
        CompletableResultCode resultCode = networkExporter.export(spanData);
        resultCode.join(timeout, TimeUnit.MILLISECONDS);

        if (!resultCode.isSuccess()) {
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

By default, items are automatically deleted from disk as the iterator advances. You can also
clear all data at once by calling `SignalStorage.clear()`.

#### Automatic vs explicit deletion

The default behavior (`deleteItemsOnIteration = true`) automatically removes items from disk during
iteration. This means you don't need to call `Iterator.remove()` since the data is cleaned up as the
iterator advances.

If you need more control (e.g., only deleting items after a successful network export), set
`deleteItemsOnIteration` to `false` in the configuration:

```java
FileStorageConfiguration config = FileStorageConfiguration.builder()
    .setDeleteItemsOnIteration(false)
    .build();
SignalStorage.Span spanStorage = FileSpanStorage.create(new File(rootDir, "spans"), config);
```

With this setting, items remain on disk until explicitly removed via `Iterator.remove()`:

```java
public boolean exportSpansFromDisk(SpanExporter networkExporter, long timeout) {
  Iterator<Collection<SpanData>> spansIterator = spanStorage.iterator();
  while (spansIterator.hasNext()) {
    CompletableResultCode resultCode = networkExporter.export(spansIterator.next());
    resultCode.join(timeout, TimeUnit.MILLISECONDS);

    if (resultCode.isSuccess()) {
      spansIterator.remove();
    } else {
      return false;
    }
  }
  return true;
}
```

Note that even with explicit deletion, disk usage is still bounded by the configured max folder size and max file
age, so stale files are automatically purged when there's not enough space available before new data is written.

### More details on the writing and reading processes

The writing and reading processes can run in parallel because they target disjoint files: the
writer appends to `<timestamp>.tmp` and only files whose name is *entirely* numeric are visible to
the reader (the reader applies `Matcher.matches()` on `\d+`, so the trailing `.tmp` excludes
in-flight files). Each rollover atomically promotes the temp file to its final name via
`Files.move(..., ATOMIC_MOVE)`, so the reader can never observe a partially-written file. Empty
rolled files are deleted instead of being promoted. If the reader is invoked
while the active writer has already exceeded its `maxFileAgeForWriteMillis` window but no other
ready file is available, the reader force-closes the writer (triggering the rename) so its data
becomes immediately readable.

Temporary files left behind by an unclean JVM shutdown are recovered the next time the storage is
re-opened: any `*.tmp` files whose target name doesn't already exist are promoted back to their
final form so that no buffered data is silently dropped.

## Component owners

* [Cesar Munoz](https://github.com/LikeTheSalad), Elastic
* [Jason Plumb](https://github.com/breedx-splk), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
