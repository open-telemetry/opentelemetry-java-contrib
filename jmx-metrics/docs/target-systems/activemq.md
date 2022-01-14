# ActiveMQ Metrics

The [JMX Metric Gatherer]( https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/jmx-metrics/README.md#target-systems) provides built in ActiveMQ metric gathering capabilities.
These metrics are sourced from: https://activemq.apache.org/jmx

### Metrics

* Name: `activemq.consumer.count`
* Description: The number of consumers currently reading from the broker.
* Unit: `consumers`
* Labels: `destination`
* Instrument Type: ObservableLongUpDownCounter


* Name: `activemq.producer.count`
* Description: The number of producers currently attached to the broker.
* Unit: `producers`
* Labels: `destination`
* Instrument Type: ObservableLongUpDownCounter


* Name: `activemq.connectin.count`
* Description: The total number of current connections.
* Unit: `connections`
* Instrument Type: ObservableLongUpDownCounter


* Name: `activemq.memory.usage`
* Description: The percentage of configured memory used.
* Unit: `%`
* Labels: `destination`
* Instrument Type: ObservableDoubleValue


* Name: `activemq.disk.store_usage`
* Description: The percentage of configured disk used for persistent messages.
* Unit: `%`
* Instrument Type: ObservableDoubleValue


* Name: `activemq.disk.temp_usage`
* Description: The percentage of configured disk used for non-persistent messages.
* Unit: `%`
* Instrument Type: ObservableDoubleValue


* Name: `activemq.message.current`
* Description: The current number of messages waiting to be consumed.
* Unit: `messages`
* Labels: `destination`
* Instrument Type: ObservableLongUpDownCounter


* Name: `activemq.message.expired`
* Description: The total number of messages not delivered because they expired.
* Unit: `messages`
* Labels: `destination`
* Instrument Type: ObservableLongCounter


* Name: `activemq.message.enqueued`
* Description: The total number of messages received by the broker.
* Unit: `messages`
* Labels: `destination`
* Instrument Type: ObservableLongCounter


* Name: `activemq.message.dequeued`
* Description: The total number of messages delivered to consumers.
* Unit: `messages`
* Labels: `destination`
* Instrument Type: ObservableLongCounter


* Name: `activemq.message.wait_time.avg`
* Description: The average time a message was held on a destination.
* Unit: `ms`
* Labels: `destination`
* Instrument Type: ObservableDoubleValue