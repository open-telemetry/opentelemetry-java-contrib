# Kafka Consumer Metrics

The JMX Metric Gatherer provides built in Kafka consumer metric gathering capabilities for versions v0.8.2.x and above.
These metrics are sourced from Kafka's exposed Yammer metrics for each instance: https://kafka.apache.org/documentation/#monitoring

## Metrics

### Consumer Metrics

* Name: `kafka.consumer.fetch-rate`
* Description: The number of fetch requests for all topics per second.
* Unit: `1`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

* Name: `kafka.consumer.records-lag-max`
* Description: Number of messages the consumer lags behind the producer.
* Unit: `1`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

* Name: `kafka.consumer.total.bytes-consumed-rate`
* Description: The average number of bytes consumed for all topics per second.
* Unit: `by`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

* Name: `kafka.consumer.total.fetch-size-avg`
* Description: The average number of bytes fetched per request for all topics.
* Unit: `by`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

* Name: `kafka.consumer.total.records-consumed-rate`
* Description: The average number of records consumed for all topics per second.
* Unit: `1`
* Labels: `client-id`
* Instrument Type: DoubleValueObserver

### Per-Topic Consumer Metrics

* Name: `kafka.consumer.bytes-consumed-rate`
* Description: The average number of bytes consumed per second
* Unit: `by`
* Labels: `client-id`, `topic`
* Instrument Type: DoubleValueObserver

* Name: `kafka.consumer.fetch-size-avg`
* Description: The average number of bytes fetched per request
* Unit: `by`
* Labels: `client-id`, `topic`
* Instrument Type: DoubleValueObserver

* Name: `kafka.consumer.records-consumed-rate`
* Description: The average number of records consumed per second
* Unit: `1`
* Labels: `client-id`, `topic`
* Instrument Type: DoubleValueObserver
