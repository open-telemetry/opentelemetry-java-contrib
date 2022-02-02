# WildFly Metrics

The JMX Metric Gatherer provides built in WildFly metric gathering capabilities.

### Deployment Metrics
* Name: `wildfly.session.count`
* Description: The number of sessions created.
* Unit: `{sessions}`
* Labels: `deployment`
* Instrument Type: LongCounterCallback

* Name: `wildfly.session.active`
* Description: The number of currently active sessions.
* Unit: `{sessions}`
* Labels: `deployment`
* Instrument Type: LongUpDownCounterCallback

* Name: `wildfly.session.expired`
* Description: The number of sessions that have expired.
* Unit: `{sessions}`
* Labels: `deployment`
* Instrument Type: LongCounterCallback

* Name: `wildfly.session.rejected`
* Description: The number of sessions that have been rejected.
* Unit: `{sessions}`
* Labels: `deployment`
* Instrument Type: LongCounterCallback

### Listener Metrics
* Name: `wildfly.request.count`
* Description: The number of requests received.
* Unit: `{requests}`
* Labels: `server`, `listener`
* Instrument Type: LongCounterCallback

* Name: `wildfly.request.time`
* Description: The total amount of time spent on requests.
* Unit: `ns`
* Labels: `server`, `listener`
* Instrument Type: LongCounterCallback

* Name: `wildfly.request.server_error`
* Description: The number of requests that have resulted in a 5xx response.
* Unit: `{requests}`
* Labels: `server`, `listener`
* Instrument Type: LongCounterCallback

* Name: `wildfly.network.io`
* Description: The number of bytes transmitted.
* Unit: `by`
* Labels: `server`, `listener`, `state`
* Instrument Type: LongCounterCallback

### Data Source Metrics
* Name: `wildfly.jdbc.connection.open`
* Description: The number of open jdbc connections.
* Unit: `{connections}`
* Labels: `data_source`, `state`
* Instrument Type: LongUpDownCounterCallback

* Name: `wildfly.jdbc.request.wait`
* Description: The number of jdbc connections that had to wait before opening.
* Unit: `{requests}`
* Labels: `data_source`
* Instrument Type: LongCounterCallback

* Name: `wildfly.jdbc.transaction.count`
* Description: The number of transactions created.
* Unit: `{transactions}`
* Instrument Type: LongCounterCallback

* Name: `wildfly.jdbc.rollback.count`
* Description: The number of transactions rolled back.
* Unit: `{transactions}`
* Instrument Type: LongCounterCallback
