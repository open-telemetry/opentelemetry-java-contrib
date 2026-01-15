# Casandra Metrics

The JMX Metric Gatherer provides built in Cassandra metric gathering capabilities.
These metrics are sourced from Cassandra's exposed Dropwizard Metrics for each node: <https://cassandra.apache.org/doc/latest/cassandra/managing/operating/metrics.html>.

## Client Request Metrics

### cassandra.client.request.range_slice.latency.50p

* Name: `cassandra.client.request.range_slice.latency.50p`
* Description: Token range read request latency - 50th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

### cassandra.client.request.range_slice.latency.99p

* Name: `cassandra.client.request.range_slice.latency.99p`
* Description: Token range read request latency - 99th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

### cassandra.client.request.range_slice.latency.max

* Name: `cassandra.client.request.range_slice.latency.max`
* Description: Maximum token range read request latency
* Unit: `µs`
* Instrument Type: DoubleValueObserver

### cassandra.client.request.read.latency.50p

* Name: `cassandra.client.request.read.latency.50p`
* Description: Standard read request latency - 50th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

### cassandra.client.request.read.latency.99p

* Name: `cassandra.client.request.read.latency.99p`
* Description: Standard read request latency - 99th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

### cassandra.client.request.read.latency.max

* Name: `cassandra.client.request.read.latency.max`
* Description: Maximum standard read request latency
* Unit: `µs`
* Instrument Type: DoubleValueObserver

### cassandra.client.request.write.latency.50p

* Name: `cassandra.client.request.write.latency.50p`
* Description: Regular write request latency - 50th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

### cassandra.client.request.write.latency.99p

* Name: `cassandra.client.request.write.latency.99p`
* Description: Regular write request latency - 99th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

### cassandra.client.request.write.latency.max

* Name: `cassandra.client.request.write.latency.max`
* Description: Maximum regular write request latency
* Unit: `µs`
* Instrument Type: DoubleValueObserver

### cassandra.client.request.count

* Name: `cassandra.client.request.count`
* Description: Number of requests by operation
* Labels: `operation`
* Unit: `1`
* Instrument Type: LongSumObserver

### cassandra.client.request.error.count

* Name: `cassandra.client.request.error.count`
* Description: Number of request errors by operation
* Labels: `operation`, `status`
* Unit: `1`
* Instrument Type: LongSumObserver

## Compaction Metrics

### cassandra.compaction.tasks.completed

* Name: `cassandra.compaction.tasks.completed`
* Description: Number of completed compactions since server [re]start
* Unit: `1`
* Instrument Type: LongSumObserver

### cassandra.compaction.tasks.pending

* Name: `cassandra.compaction.tasks.pending`
* Description: Estimated number of compactions remaining to perform
* Unit: `1`
* Instrument Type: LongValueObserver

## Storage Metrics

### cassandra.storage.load.count

* Name: `cassandra.storage.load.count`
* Description: Size of the on disk data size this node manages
* Unit: `by`
* Instrument Type: LongValueObserver

### cassandra.storage.total_hints.count

* Name: `cassandra.storage.total_hints.count`
* Description: Number of hint messages written to this node since [re]start
* Unit: `1`
* Instrument Type: LongSumObserver

### cassandra.storage.total_hints.in_progress.count

* Name: `cassandra.storage.total_hints.in_progress.count`
* Description: Number of hints attempting to be sent currently
* Unit: `1`
* Instrument Type: LongValueObserver
