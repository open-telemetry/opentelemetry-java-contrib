# Casandra Metrics

The JMX Metric Gatherer provides built in Cassandra metric gathering capabilities.
These metrics are sourced from Cassandra's exposed Dropwizard Metrics for each node: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/metrics.html.

## Metrics

### Client Request Metrics

* Name: `cassandra.client.request.range_slice.latency.50p`
* Description: Token range read request latency - 50th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

* Name: `cassandra.client.request.range_slice.latency.99p`
* Description: Token range read request latency - 99th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

* Name: `cassandra.client.request.range_slice.latency.max`
* Description: Maximum token range read request latency
* Unit: `µs`
* Instrument Type: DoubleValueObserver

* Name: `cassandra.client.request.read.latency.50p`
* Description: Standard read request latency - 50th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

* Name: `cassandra.client.request.read.latency.99p`
* Description: Standard read request latency - 99th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

* Name: `cassandra.client.request.read.latency.max`
* Description: Maximum standard read request latency
* Unit: `µs`
* Instrument Type: DoubleValueObserver

* Name: `cassandra.client.request.write.latency.50p`
* Description: Regular write request latency - 50th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

* Name: `cassandra.client.request.write.latency.99p`
* Description: Regular write request latency - 99th percentile
* Unit: `µs`
* Instrument Type: DoubleValueObserver

* Name: `cassandra.client.request.write.latency.max`
* Description: Maximum regular write request latency
* Unit: `µs`
* Instrument Type: DoubleValueObserver

* Name: `cassandra.client.request.count`
* Description: Number of requests by operation
* Labels: `operation`
* Unit: `1`
* Instrument Type: LongSumObserver

* Name: `cassandra.client.request.error.count`
* Description: Number of request errors by operation
* Labels: `operation`, `status`
* Unit: `1`
* Instrument Type: LongSumObserver

### Compaction Metrics

* Name: `cassandra.compaction.tasks.completed`
* Description: Number of completed compactions since server [re]start
* Unit: `1`
* Instrument Type: LongSumObserver

* Name: `cassandra.compaction.tasks.pending`
* Description: Estimated number of compactions remaining to perform
* Unit: `1`
* Instrument Type: LongValueObserver

### Storage Metrics

* Name: `cassandra.storage.load.count`
* Description: Size of the on disk data size this node manages
* Unit: `by`
* Instrument Type: LongValueObserver

* Name: `cassandra.storage.total_hints.count`
* Description: Number of hint messages written to this node since [re]start
* Unit: `1`
* Instrument Type: LongSumObserver

* Name: `cassandra.storage.total_hints.in_progress.count`
* Description: Number of hints attempting to be sent currently
* Unit: `1`
* Instrument Type: LongValueObserver
