# Kafka Metrics

The JMX Metric Gatherer provides built in Kafka metric gathering capabilities for versions v0.8.2.x and above.
These metrics are sourced from Kafka's exposed JMX metrics for each instance: <https://kafka.apache.org/documentation/#monitoring>

## Broker Metrics

### kafka.message.count

* Name: `kafka.message.count`
* Description: The number of messages received by the broker
* Unit: `{messages}`
* Instrument Type: LongCounterObserver

### kafka.request.count

* Name: `kafka.request.count`
* Description: The number of requests received by the broker
* Unit: `{requests}`
* Attributes: `type`
* Instrument Type: LongCounterObserver

### kafka.request.failed

* Name: `kafka.request.failed`
* Description: The number of requests to the broker resulting in a failure
* Unit: `{requests}`
* Attributes: `type`
* Instrument Type: LongCounterObserver

### kafka.request.time.total

* Name: `kafka.request.time.total`
* Description: The total time the broker has taken to service requests
* Unit: `ms`
* Attributes: `type`
* Instrument Type: LongCounterObserver

### kafka.request.time.50p

* Name: `kafka.request.time.50p`
* Description: The 50th percentile time the broker has taken to service requests
* Unit: `ms`
* Attributes: `type`
* Instrument Type: DoubleValueObserver

### kafka.request.time.99p

* Name: `kafka.request.time.99p`
* Description: The 99th percentile time the broker has taken to service requests
* Unit: `ms`
* Attributes: `type`
* Instrument Type: DoubleValueObserver

### kafka.request.time.avg

* Name: `kafka.request.time.avg`
* Description: The average time the broker has taken to service requests
* Unit: `ms`
* Attributes: `type`
* Instrument Type: DoubleValueObserver

### kafka.network.io

* Name: `kafka.network.io`
* Description: The bytes received or sent by the broker
* Unit: `by`
* Attributes: `state`
* Instrument Type: LongCounterObserver

### kafka.purgatory.size

* Name: `kafka.purgatory.size`
* Description: The number of requests waiting in purgatory
* Unit: `{requests}`
* Attributes: `type`
* Instrument Type: LongValueObserver

### kafka.partition.count

* Name: `kafka.partition.count`
* Description: The number of partitions on the broker
* Unit: `{partitions}`
* Instrument Type: LongValueObserver

### kafka.partition.offline

* Name: `kafka.partition.offline`
* Description: The number of partitions offline
* Unit: `{partitions}`
* Instrument Type: LongValueObserver

### kafka.partition.under_replicated

* Name: `kafka.partition.under_replicated`
* Description: The number of under replicated partitions
* Unit: `{partitions}`
* Instrument Type: LongValueObserver

### kafka.isr.operation.count

* Name: `kafka.isr.operation.count`
* Description: The number of in-sync replica shrink and expand operations
* Unit: `{operations}`
* Attributes: `operation`
* Instrument Type: LongCounterObserver

### kafka.max.lag

* Name: `kafka.max.lag`
* Description: Max lag in messages between follower and leader replicas
* Unit: `{messages}`
* Instrument Type: LongValueObserver

### kafka.controller.active.count

* Name: `kafka.controller.active.count`
* Description: Controller is active on broker
* Unit: `{controllers}`
* Instrument Type: LongValueObserver

### kafka.leader.election.rate

* Name: `kafka.leader.election.rate`
* Description: Leader election rate - increasing indicates broker failures
* Unit: `{elections}`
* Instrument Type: LongCounterObserver

### kafka.unclean.election.rate

* Name: `kafka.unclean.election.rate`
* Description: Unclean leader election rate - increasing indicates broker failures
* Unit: `{elections}`
* Instrument Type: LongCounterObserver

### kafka.request.queue

* Name: `kafka.request.queue`
* Description: Size of the request queue
* Unit: `{requests}`
* Instrument Type: LongValueObserver

## Log metrics

### kafka.logs.flush.time.count

* Name: `kafka.logs.flush.time.count`
* Description: Log flush count
* Unit: `ms`
* Instrument Type: LongSumObserver

### kafka.logs.flush.time.median

* Name: `kafka.logs.flush.time.median`
* Description: Log flush time - 50th percentile
* Unit: `ms`
* Instrument Type: DoubleValueObserver

### kafka.logs.flush.time.99p

* Name: `kafka.logs.flush.time.99p`
* Description: Log flush time - 99th percentile
* Unit: `ms`
* Instrument Type: DoubleValueObserver
