# Kafka Producer Metrics

The JMX Metric Gatherer provides built in Kafka producer metric gathering capabilities for versions v0.8.2.x and above.
These metrics are sourced from Kafka's exposed Yammer metrics for each instance: https://kafka.apache.org/documentation/#monitoring

## Metrics

### Producer Metrics

* Name: `kafka.producer.io-wait-time-ns-avg`
* Description: The average length of time the I/O thread spent waiting for a socket ready for reads or writes.
* Unit: `ns`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

* Name: `kafka.producer.outgoing-byte-rate`
* Description: The average number of outgoing bytes sent per second to all servers.
* Unit: `by`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

* Name: `kafka.producer.request-latency-avg`
* Description: The average request latency.
* Unit: `ms`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

* Name: `kafka.producer.request-rate`
* Description: The average number of requests sent per second.
* Unit: `1`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

* Name: `kafka.producer.response-rate`
* Description: Responses received per second.
* Unit: `1`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

### Per-Topic Producer Metrics

* Name: `kafka.producer.byte-rate`
* Description: The average number of bytes sent per second for a topic.
* Unit: `by`
* Labels: `client-id`, `topic`
* Instrument Type: DoubleValueObserver

* Name: `kafka.producer.compression-rate`
* Description: The average compression rate of record batches for a topic.
* Unit: `1`
* Labels: `client-id`, `topic`
* Instrument Type: DoubleValueObserver

* Name: `kafka.producer.record-error-rate`
* Description: The average per-second number of record sends that resulted in errors for a topic.
* Unit: `1`
* Labels: `client-id`, `topic`
* Instrument Type: DoubleValueObserver

* Name: `kafka.producer.record-retry-rate`
* Description: The average per-second number of retried record sends for a topic.
* Unit: `1`
* Labels: `client-id`, `topic`
* Instrument Type: DoubleValueObserver

* Name: `kafka.producer.record-send-rate`
* Description: The average number of records sent per second for a topic.
* Unit: `1`
* Labels: `client-id`, `topic`
* Instrument Type: DoubleValueObserver
