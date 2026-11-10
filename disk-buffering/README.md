# Disk buffering

[![Maven](https://img.shields.io/maven-central/v/io.opentelemetry.contrib/opentelemetry-disk-buffering?label=Maven&color=orange)](https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-disk-buffering)

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

The following examples use explicit deletion so that failed exports can be retried.

```java
// Root dir
File rootDir = new File("/some/root");

FileStorageConfiguration config = FileStorageConfiguration.builder()
    .setDeleteItemsOnIteration(false)
    .build();

// Setting up span storage
SignalStorage.Span spanStorage = FileSpanStorage.create(new File(rootDir, "spans"), config);

// Setting up metric storage
SignalStorage.Metric metricStorage = FileMetricStorage.create(new File(rootDir, "metrics"), config);

// Setting up log storage
SignalStorage.LogRecord logStorage = FileLogRecordStorage.create(new File(rootDir, "logs"), config);
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

To export buffered spans, keep a reader instance between calls. It retains a failed or timed-out
batch and retries it before advancing the iterator. With `deleteItemsOnIteration` set to `false`
as above, the batch also remains on disk until it is explicitly removed after a successful export.

Call the reader sequentially. The caller controls retry delays and limits.

```java
public final class SpanStorageReader {
  private final Iterator<Collection<SpanData>> spanBatches;
  private Collection<SpanData> pendingBatch;

  public SpanStorageReader(SignalStorage.Span spanStorage) {
    this.spanBatches = spanStorage.iterator();
  }

  /**
   * @return true if all available batches were exported, false if the current batch needs a retry
   */
  public boolean exportSpansFromDisk(SpanExporter networkExporter, long timeout) {
    while (pendingBatch != null || spanBatches.hasNext()) {
      if (pendingBatch == null) {
        pendingBatch = spanBatches.next();
      }

      CompletableResultCode resultCode = networkExporter.export(pendingBatch);
      resultCode.join(timeout, TimeUnit.MILLISECONDS);
      if (!resultCode.isSuccess()) {
        return false;
      }

      spanBatches.remove();
      pendingBatch = null;
    }
    return true;
  }
}
```

Reuse the same reader after a failed attempt so that it retries the pending batch first. A timed-out
export might still complete, so retrying can deliver duplicates.

### Deleting data

#### Automatic vs explicit deletion

By default, `deleteItemsOnIteration = true` automatically removes the previous batch from disk
when the iterator advances. Automatic deletion is independent of the network export result;
stopping an iteration after a failure does not protect that batch when iteration resumes.

The examples above use `deleteItemsOnIteration = false` and call `Iterator.remove()` only after
a successful network export. You can also clear all data at once by calling `SignalStorage.clear()`.

Even with explicit deletion, disk usage is bounded by the configured max folder size and max file
age, so stale files are automatically purged when there is not enough space before new data is written.
Set a `DiscardedFileListener` with `setDiscardedFileListener` to be notified when a file is deleted
before all of its data was read.

### More details on the writing and reading processes

Writer and reader operate on disjoint files: the writer appends to `<timestamp>.tmp`, and the
reader only considers files whose name is entirely numeric (matched against `\d+`). On close, the
writer promotes the temp file via `File.renameTo`, atomic on POSIX, best-effort on Windows,
so the reader never observes a partial file. Empty files are deleted instead of promoted. If the
reader runs while the active writer has exceeded `maxFileAgeForWriteMillis` and no other file is
ready, the reader force-closes the writer to make its data immediately visible.

On startup any `*.tmp` left over from a crash is promoted to its final name (unless that name
already exists, in which case the leftover is dropped), so buffered data is preserved.

## Component owners

* [Cesar Munoz](https://github.com/LikeTheSalad), Elastic
* [Jason Plumb](https://github.com/breedx-splk), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
