# Tomcat Metrics

The JMX Metric Gatherer provides built in Tomcat metric gathering capabilities.
These metrics are sourced from: https://docs.oracle.com/cd/E11857_01/em.111/e10115/middleware_apache_tomcat.htm

### Metrics

* Name: `tomcat.sessions`
* Description: The number of active sessions.
* Unit: `sessions`
* Instrument Type: DoubleValueCallback

* Name: `tomcat.errors`
* Description: The number of errors encountered.
* Unit: `errors`
* Labels: `proto_handler`
* Instrument Type: LongCounterCallback

* Name: `tomcat.processing_time`
* Description: The total processing time.
* Unit: `ms`
* Labels: `proto_handler`
* Instrument Type: LongCounterCallback

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
