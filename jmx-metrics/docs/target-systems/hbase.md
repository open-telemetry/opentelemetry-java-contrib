# Hbase Metrics

The JMX Metric Gatherer provides built in Hbase metric gathering capabilities.
These metrics are sourced from: https://hbase.apache.org/book.html#hbase_metrics

### Metrics

* Name: `hbase.master.region_server.count`
* Description: The number of region servers.
* Unit: `{servers}`
* Labels: `state`
* Instrument Type: longUpDownCounter


* Name: `hbase.master.in_transition_regions.count`
* Description: The number of regions that are in transition.
* Unit: `1`
* Instrument Type: longUpDownCounter


* Name: `hbase.master.in_transition_regions.over_threshold`
* Description: The number of regions that have been in transition longer than a threshold time.
* Unit: `1`
* Instrument Type: longUpDownCounter


* Name: `hbase.master.in_transition_regions.oldest_age`
* Description: The age of the longest region in transition.
* Unit: `ms`
* Instrument Type: longValue


* Name: `hbase.region_server.region.count`
* Description: The number of regions hosted by the region server.
* Unit: `{regions}`
* Labels: `region_server`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.disk.store_file.count`
* Description: The number of store files on disk currently managed by the region server.
* Unit: `{store_files}`
* Labels: `region_server`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.disk.store_file.size`
* Description: Aggregate size of the store files on disk.
* Unit: `By`
* Labels: `region_server`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.write_ahead_log.count`
* Description: The number of write ahead logs not yet archived.
* Unit: `{logs}`
* Labels: `region_server`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.request.count`
* Description: The number of requests received.
* Unit: `{requests}`
* Labels: `region_server`, `state`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.queue.length`
* Description: The number of RPC handlers actively servicing requests.
* Unit: `{handlers}`
* Labels: `region_server`, `state`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.blocked_update.time`
* Description: Amount of time updates have been blocked so the memstore can be flushed.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.block_cache.operation.count`
* Description: Number of block cache hits/misses.
* Unit: `{operations}`
* Labels: `region_server`, `state`
* Instrument Type: longValue


* Name: `hbase.region_server.files.local`
* Description: Percent of store file data that can be read from the local.
* Unit: `%`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.append.latency.p99`
* Description: Append operation 99th Percentile latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.append.latency.max`
* Description: Append operation max latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.delete.latency.p99`
* Description: Delete operation 99th Percentile latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.delete.latency.max`
* Description: Delete operation max latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.put.latency.p99`
* Description: Put operation 99th Percentile latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.put.latency.max`
* Description: Put operation max latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.get.latency.p99`
* Description: Get operation 99th Percentile latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.get.latency.max`
* Description: Get operation max latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.replay.latency.p99`
* Description: Replay operation 99th Percentile latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.replay.latency.max`
* Description: Replay operation max latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.increment.latency.p99`
* Description: Increment operation 99th Percentile latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operation.increment.latency.max`
* Description: Increment operation max latency.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longValue


* Name: `hbase.region_server.operations.slow`
* Description: Number of operations that took over 1000ms to complete.
* Unit: `{operations}`
* Labels: `region_server`, `operation`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.open_connection.count`
* Description: The number of open connections at the RPC layer.
* Unit: `{connections}`
* Labels: `region_server`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.active_handler.count`
* Description: The number of RPC handlers actively servicing requests.
* Unit: `{handlers}`
* Labels: `region_server`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.queue.request.count`
* Description: The number of currently enqueued requests.
* Unit: `{requests}`
* Labels: `region_server`, `state`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.authentication.count`
* Description: Number of client connection authentication failures/successes.
* Unit: `1`
* Labels: `region_server`, `state`
* Instrument Type: longUpDownCounter


* Name: `hbase.region_server.gc.time`
* Description: Time spent in garbage collection.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longCounter


* Name: `hbase.region_server.gc.young_gen.time`
* Description: Time spent in garbage collection of the young generation.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longCounter


* Name: `hbase.region_server.gc.old_gen.time`
* Description: Time spent in garbage collection of the old generation.
* Unit: `ms`
* Labels: `region_server`
* Instrument Type: longCounter
