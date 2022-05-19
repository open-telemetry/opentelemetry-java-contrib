# Jetty Metrics

The JMX Metric Gatherer provides built in Jetty metric gathering capabilities.
Details about using JMX with WildFly can be found here: https://www.eclipse.org/jetty/documentation/jetty-11/operations-guide/index.html#og-jmx

### Metrics
* Name: `jetty.select.count`
* Description: The number of select calls.
* Unit: `{operations}`
* Instrument Type: longCounterCallback


* Name: `jetty.session.count`
* Description: The number of sessions created.
* Unit: `{sessions}`
* Instrument Type: LongCounterCallback


* Name: `jetty.session.time.total`
* Description: The total time sessions have been active.
* Unit: `s`
* Instrument Type: longUpDownCounterCallback


* Name: `jetty.session.time.max`
* Description: The maximum amount of time a session has been active.
* Unit: `s`
* Instrument Type: longValueCallback


* Name: `jetty.thread.count`
* Description: The current number of threads.
* Unit: `{threads}`
* Labels: `state`
* Instrument Type: longValueCallback


* Name: `jetty.thread.queue.count`
* Description: The current number of threads in the queue.
* Unit: `{threads}`
* Instrument Type: longValueCallback
