# Kafka Metrics

The JMX Metric Gatherer provides built in Kafka metric gathering capabilities for versions v0.8.2.x and above.
These metrics are sourced from Kafka's exposed Yammer metrics for each instance: https://kafka.apache.org/documentation/#monitoring

## Metrics

### Broker Metrics

* Name: `kafka.message.count`
* Description: The number of messages received by the broker
* Unit: `messages`
* Attributes: `topic`
* Instrument Type: LongCounterObserver

* Name: `kafka.request.count`
* Description: The number of requests received by the broker
* Unit: `requests`
* Attributes: `topic`, `type`
* Instrument Type: LongCounterObserver

* Name: `kafka.request.failed`
* Description: The number of requests to the broker resulting in a failure
* Unit: `requests`
* Attributes: `topic`, `type`
* Instrument Type: LongCounterObserver

* Name: `kafka.request.time.total`
* Description: The total time the broker has taken to service requests
* Unit: `ms`
* Attributes: `type`
* Instrument Type: LongCounterObserver

* Name: `kafka.request.time.50p`
* Description: The 50th percentile time the broker has taken to service requests
* Unit: `ms`
* Attributes: `type`
* Instrument Type: DoubleValueObserver

* Name: `kafka.request.time.99p`
* Description: The 99th percentile time the broker has taken to service requests
* Unit: `ms`
* Attributes: `type`
* Instrument Type: DoubleValueObserver

* Name: `kafka.network.io`
* Description: The bytes received or sent by the broker
* Unit: `by`
* Attributes: `topic`, `state`
* Instrument Type: LongCounterObserver

* Name: `kafka.purgatory.size`
* Description: The number of requests waiting in purgatory
* Unit: `requests`
* Attributes: `type`
* Instrument Type: LongValueObserver

* Name: `kafka.partition.count`
* Description: The number of partitions on the broker
* Unit: `partitions`
* Instrument Type: LongValueObserver

* Name: `kafka.partition.offline`
* Description: The number of partitions offline
* Unit: `partitions`
* Instrument Type: LongValueObserver

* Name: `kafka.partition.under_replicated`
* Description: The number of under replicated partitions
* Unit: `partitions`
* Instrument Type: LongValueObserver

* Name: `kafka.isr.operation.count`
* Description: The number of in-sync replica shrink and expand operations
* Unit: `operations`
* Attributes: `operation`
* Instrument Type: LongCounterObserver

* Name: `kafka.max.lag`
* Description: Max lag in messages between follower and leader replicas
* Unit: `1`
* Instrument Type: LongValueObserver

* Name: `kafka.controller.active.count`
* Description: Controller is active on broker
* Unit: `1`
* Instrument Type: LongValueObserver

* Name: `kafka.leader.election.rate`
* Description: Leader election rate - non-zero indicates broker failures
* Unit: `1`
* Instrument Type: LongValueObserver

* Name: `kafka.unclean.election.rate`
* Description: Unclean leader election rate - non-zero indicates broker failures
* Unit: `1`
* Instrument Type: LongValueObserver

* Name: `kafka.request.queue`
* Description: Size of the request queue
* Unit: `1`
* Instrument Type: LongValueObserver

### Log metrics

* Name: `kafka.logs.flush.time.count`
* Description: Log flush count
* Unit: `1`
* Instrument Type: LongSumObserver

* Name: `kafka.logs.flush.time.median`
* Description: Log flush time - 50th percentile
* Unit: `ms`
* Instrument Type: DoubleValueObserver

* Name: `kafka.logs.flush.time.99p`
* Description: Log flush time - 99th percentile
* Unit: `ms`
* Instrument Type: DoubleValueObserver
