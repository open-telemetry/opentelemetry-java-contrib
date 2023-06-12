# Disk buffering

This module provides signal exporter wrappers that intercept and store signals in files which can be
sent later on demand.

## Configuration

The configurable parameters are provided **per exporter**, the available ones are:

* Max file size, defaults to 1MB.
* Max folder size, defaults to 10MB. All files are stored in a single folder per-signal, therefore
  if all 3 types of signals are stored, the total amount of space from disk to be taken by default
  would be of 30MB.
* Max age for file writing, defaults to 30 seconds.
* Min age for file reading, defaults to 33 seconds. It must be greater that the max age for file
  writing.
* Max age for file reading, defaults to 18 hours. After 18 passed, the file would be considered
  stale and will be removed when new files are created. No more data will be read from a file past
  this time.

## Usage

### Storing data

In order to use it, you need to wrap your own exporter with a new instance of
the ones provided in here:

* For a LogRecordExporter, it must be wrapped within
  a [LogRecordDiskExporter](src%2Fmain%2Fjava%2Fio%2Fopentelemetry%2Fcontrib%2Fdisk%2Fbuffering%2Fexporters%2FLogRecordDiskExporter.java).
* For a MetricExporter, it must be wrapped within
  a [MetricDiskExporter](src%2Fmain%2Fjava%2Fio%2Fopentelemetry%2Fcontrib%2Fdisk%2Fbuffering%2Fexporters%2FMetricDiskExporter.java).
* For a SpanExporter, it must be wrapped within
  a [SpanDiskExporter](src%2Fmain%2Fjava%2Fio%2Fopentelemetry%2Fcontrib%2Fdisk%2Fbuffering%2Fexporters%2FSpanDiskExporter.java).

Each wrapper will need the following when instantiating them:

* The exporter to be wrapped.
* A File instance of the root directory where all the data is going to be written. The same root dir
  can be used for all the wrappers, since each will create their own folder inside it.
* An instance
  of [StorageConfiguration](src%2Fmain%2Fjava%2Fio%2Fopentelemetry%2Fcontrib%2Fdisk%2Fbuffering%2Fstorage%2FStorageConfiguration.java)
  with the desired parameters. You can create one with default values by
  calling `StorageConfiguration.getDefault()`.

After wrapping your exporters, you must register the wrapper as the exporter you'll use. It will
take care of always storing the data it receives.

### Reading data

Each of the exporter wrappers can read from the disk and send the retrieved data over to their
wrapped exporter by calling this method from them:

```java
try {
    if(exporter.exportStoredBatch(1, TimeUnit.SECONDS)) {
        // A batch was successfully exported. You can call this method for as long as it keeps returning true.
    } else {
        // Either there was no data in the disk or the wrapped exporter returned CompletableResultCode.ofFailure().
    }
} catch (IOException e) {
    // Something unexpected happened.
}
```
