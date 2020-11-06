# Kafka Metrics

The JMX Metric Gatherer provides built in Kafka metric gathering capabilities for versions v0.8.2.x and above.
These metrics are sourced from Kafka's exposed Yammer metrics for each instance: https://kafka.apache.org/documentation/#monitoring

## Metrics

### Broker Metrics

* Name: `kafka.messages.in`
* Description: Number of messages in per second
* Unit: `1`
* Instrument Type: LongCounter

* Name: `kafka.bytes.in`
* Description: Bytes in per second from clients
* Unit: `by`
* Instrument Type: LongCounter

* Name: `kafka.bytes.out`
* Description: Bytes out per second to clients
* Unit: `by`
* Instrument Type: LongCounter

* Name: `kafka.isr.shrinks`
* Description: In-sync replica shrinks per second
* Unit: `1`
* Instrument Type: LongCounter

* Name: `kafka.isr.expands`
* Description: In-sync replica expands per second
* Unit: `1`
* Instrument Type: LongCounter

* Name: `kafka.max.lag`
* Description: Max lag in messages between follower and leader replicas
* Unit: `1`
* Instrument Type: LongUpDownCounter

* Name: `kafka.controller.active.count`
* Description: Controller is active on broker
* Unit: `1`
* Instrument Type: LongUpDownCounter

* Name: `kafka.partitions.offline.count`
* Description: Number of partitions without an active leader
* Unit: `1`
* Instrument Type: LongUpDownCounter

* Name: `kafka.partitions.underreplicated.count`
* Description: Number of under replicated partitions
* Unit: `1`
* Instrument Type: LongUpDownCounter

* Name: `kafka.leader.election.rate`
* Description: Leader election rate - non-zero indicates broker failures
* Unit: `1`
* Instrument Type: LongCounter

* Name: `kafka.unclean.election.rate`
* Description: Unclean leader election rate - non-zero indicates broker failures
* Unit: `1`
* Instrument Type: LongCounter

* Name: `kafka.request.queue`
* Description: Size of the request queue
* Unit: `1`
* Instrument Type: LongUpDownCounter

* Name: `kafka.fetch.consumer.total.time.count`
* Description: Fetch consumer request count
* Unit: `1`
* Instrument Type: LongCounter

* Name: `kafka.fetch.consumer.total.time.median`
* Description: Fetch consumer request time - 50th percentile
* Unit: `ms`
* Instrument Type: DoubleUpDownCounter

* Name: `kafka.fetch.consumer.total.time.99p`
* Description: Fetch consumer request time - 99th percentile
* Unit: `ms`
* Instrument Type: DoubleUpDownCounter

* Name: `kafka.fetch.follower.total.time.count`
* Description: Fetch follower request count
* Unit: `1`
* Instrument Type: LongCounter

* Name: `kafka.fetch.follower.total.time.median`
* Description: Fetch follower request time - 50th percentile
* Unit: `ms`
* Instrument Type: DoubleUpDownCounter

* Name: `kafka.fetch.follower.total.time.99p`
* Description: Fetch follower request time - 99th percentile
* Unit: `ms`
* Instrument Type: DoubleUpDownCounter

* Name: `kafka.produce.total.time.count`
* Description: Produce request count
* Unit: `1`
* Instrument Type: LongCounter

* Name: `kafka.produce.total.time.median`
* Description: Produce request time - 50th percentile
* Unit: `ms`
* Instrument Type: DoubleUpDownCounter

* Name: `kafka.produce.total.time.99p`
* Description: Produce request time - 99th percentile
* Unit: `ms`
* Instrument Type: DoubleUpDownCounter

### Log metrics

* Name: `kafka.logs.flush.time.count`
* Description: Log flush count
* Unit: `1`
* Instrument Type: LongCounter

* Name: `kafka.logs.flush.time.median`
* Description: Log flush time - 50th percentile
* Unit: `ms`
* Instrument Type: DoubleUpDownCounter

* Name: `kafka.logs.flush.time.99p`
* Description: Log flush time - 99th percentile
* Unit: `ms`
* Instrument Type: DoubleUpDownCounter
