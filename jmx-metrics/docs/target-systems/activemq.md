# ActiveMQ Metrics

The JMX Metric Gatherer provides built in Tomcat metric gathering capabilities.
These metrics are sourced from: https://activemq.apache.org/jmx

### Metrics

* Name: `activemq.consumer.count`
* Description: The number of consumers currently reading from the broker.
* Unit: `{consumers}`
* Labels: `destination`
* Instrument Type: LongUpDownCounterCallback


* Name: `activemq.producer.count`
* Description: The number of producers currently attached to the broker.
* Unit: `{producers}`
* Labels: `destination`
* Instrument Type: LongUpDownCounterCallback


* Name: `activemq.memory.usage`
* Description: The percentage of configured memory used.
* Unit: `%`
* Labels: `destination`
* Instrument Type: DoubleValueCallback


* Name: `tomcat.traffic`
* Description: The number of bytes transmitted and received.
* Unit: `by`
* Labels: `proto_handler`, `direction`
* Instrument Type: LongCounterCallback


* Name: `tomcat.threads`
* Description: The number of threads.
* Unit: `threads`
* Labels: `proto_handler`, `state`
* Instrument Type: LongValueCallback


* Name: `tomcat.max_time`
* Description: Maximum time to process a request.
* Unit: `ms`
* Labels: `proto_handler`
* Instrument Type: LongCounterCallback


* Name: `tomcat.request_count`
* Description: The total requests.
* Unit: `requests`
* Labels: `proto_handler`
* Instrument Type: LongCounterCallback
