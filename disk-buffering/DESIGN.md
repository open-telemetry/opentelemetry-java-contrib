# Design Overview

The core of disk buffering
is [SignalStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/SignalStorage.java).
SignalStorage is an abstraction that defines the bare minimum functionalities needed for
implementations to allow writing and reading signals.

There is a default implementation per signal that writes serialized signal items to protobuf
delimited messages into files, where each file's name represents a timestamp of when it was created,
which will help later to know when it's ready to read, as well as when it's expired. These
implementations are the following:

* [FileSpanStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/impl/FileSpanStorage.java)
* [FileLogRecordStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/impl/FileLogRecordStorage.java)
* [FileMetricStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/impl/FileMetricStorage.java)

Each one has a `create()` method that takes a destination directory (to store data into) and an
optional [FileStorageConfiguration](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/impl/FileStorageConfiguration.java)
to have a finer control of the storing behavior.

Even
though [SignalStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/SignalStorage.java)
can receive signal items directly to be stored in disk, there are convenience exporter
implementations for each signal that handle the storing process on your behalf. Those are the
following:

* [SpanToDiskExporter](src/main/java/io/opentelemetry/contrib/disk/buffering/exporters/SpanToDiskExporter.java)
* [LogRecordToDiskExporter](src/main/java/io/opentelemetry/contrib/disk/buffering/exporters/LogRecordToDiskExporter.java)
* [MetricToDiskExporter](src/main/java/io/opentelemetry/contrib/disk/buffering/exporters/MetricToDiskExporter.java)

Each receive their
respective [SignalStorage](src/main/java/io/opentelemetry/contrib/disk/buffering/storage/SignalStorage.java)
object to delegate signals to as well as an optional callback object to notify its operations.

## Writing overview

![Writing flow](assets/writing-flow.png)

* Via the convenience toDisk exporters, the writing process happens automatically within their
  `export(Collection<SignalData> signals)` method, which is called by the configured signal
  processor.
* When a set of signals is received, these are delegated over to a type-specific serializer
  and then the serialized data is appended into a file.
* The data is written into a file directly, without the use of a buffer, to make sure no data gets
  lost in case the application ends unexpectedly.
* Each signal storage stores its signals in its own folder, which is expected to contain files
  that belong to that type of signal only.
* Each file may contain more than a batch of signals if the configuration parameters allow enough
  limit size for it.
* If the configured folder size for the signals has been reached and a new file is needed to be
  created to keep storing new data, the oldest available file will be removed to make space for the
  new one.

## Reading overview

![Reading flow](assets/reading-flow.png)

* The reading process has to be triggered manually by the library consumer via the signal storage
  iterator.
* A single file is read at a time and updated to remove the data gathered from it after it is
  successfully exported, until it's emptied. Each file previously created during the
  writing process has a timestamp in milliseconds, which is used to determine what file to start
  reading from, which will be the oldest one available.
* If the oldest file available is stale, which is determined based on the configuration provided at
  the time of creating the disk exporter, then it will be ignored, and the next oldest (and
  unexpired) one will be used instead.
* All the stale and empty files will be removed as a new file is created.
