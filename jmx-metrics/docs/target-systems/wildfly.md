# WildFly Metrics

The JMX Metric Gatherer provides built in WildFly metric gathering capabilities.
Details about using JMX with WildFly can be found here: <https://docs.jboss.org/author/display/WFLY/JMX%20subsystem%20configuration.html>

## Deployment Metrics

### wildfly.session.count

* Name: `wildfly.session.count`
* Description: The number of sessions created.
* Unit: `{sessions}`
* Labels: `deployment`
* Instrument Type: LongCounterCallback

### wildfly.session.active

* Name: `wildfly.session.active`
* Description: The number of currently active sessions.
* Unit: `{sessions}`
* Labels: `deployment`
* Instrument Type: LongUpDownCounterCallback

### wildfly.session.expired

* Name: `wildfly.session.expired`
* Description: The number of sessions that have expired.
* Unit: `{sessions}`
* Labels: `deployment`
* Instrument Type: LongCounterCallback

### wildfly.session.rejected

* Name: `wildfly.session.rejected`
* Description: The number of sessions that have been rejected.
* Unit: `{sessions}`
* Labels: `deployment`
* Instrument Type: LongCounterCallback

## Listener Metrics

### wildfly.request.count

* Name: `wildfly.request.count`
* Description: The number of requests received.
* Unit: `{requests}`
* Labels: `server`, `listener`
* Instrument Type: LongCounterCallback

### wildfly.request.time

* Name: `wildfly.request.time`
* Description: The total amount of time spent on requests.
* Unit: `ns`
* Labels: `server`, `listener`
* Instrument Type: LongCounterCallback

### wildfly.request.server_error

* Name: `wildfly.request.server_error`
* Description: The number of requests that have resulted in a 5xx response.
* Unit: `{requests}`
* Labels: `server`, `listener`
* Instrument Type: LongCounterCallback

### wildfly.network.io

* Name: `wildfly.network.io`
* Description: The number of bytes transmitted.
* Unit: `by`
* Labels: `server`, `listener`, `state`
* Instrument Type: LongCounterCallback

## Data Source Metrics

### wildfly.jdbc.connection.open

* Name: `wildfly.jdbc.connection.open`
* Description: The number of open jdbc connections.
* Unit: `{connections}`
* Labels: `data_source`, `state`
* Instrument Type: LongUpDownCounterCallback

### wildfly.jdbc.request.wait

* Name: `wildfly.jdbc.request.wait`
* Description: The number of jdbc connections that had to wait before opening.
* Unit: `{requests}`
* Labels: `data_source`
* Instrument Type: LongCounterCallback

### wildfly.jdbc.transaction.count

* Name: `wildfly.jdbc.transaction.count`
* Description: The number of transactions created.
* Unit: `{transactions}`
* Instrument Type: LongCounterCallback

### wildfly.jdbc.rollback.count

* Name: `wildfly.jdbc.rollback.count`
* Description: The number of transactions rolled back.
* Unit: `{transactions}`
* Instrument Type: LongCounterCallback
