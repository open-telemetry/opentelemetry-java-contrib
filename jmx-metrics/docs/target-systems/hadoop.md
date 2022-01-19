# Hadoop Metrics

The JMX Metric Gatherer provides built in Hadoop metric gathering capabilities.
These metrics are sourced from: https://hadoop.apache.org/docs/r2.7.2/hadoop-project-dist/hadoop-common/Metrics.html

### HDFS Metrics
* Name: `hdfs.disk.usage`
* Description: The amount of disk used by data nodes.
* Unit: `by`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

* Name: `hdfs.disk.limit`
* Description: The total disk allotted to data nodes.
* Unit: `by`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

* Name: `hdfs.block.count`
* Description: The total number of blocks.
* Unit: `blocks`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

* Name: `hdfs.block.missing`
* Description: The number of blocks reported as missing.
* Unit: `blocks`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

* Name: `hdfs.block.corrupt`
* Description: The number of blocks reported as corrupt.
* Unit: `blocks`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

* Name: `hdfs.volume.failed`
* Description: The number of failed volumes.
* Unit: `volumes`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

* Name: `hdfs.file.count`
* Description: The total number of files being tracked by the name node.
* Unit: `files`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

* Name: `hdfs.file.load`
* Description: The current number of concurrent file accesses.
* Unit: `operations`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

* Name: `data_node.count`
* Description: The number of data nodes tracked by the name node.
* Unit: `nodes`
* Labels: `node_name`, `state`
* Instrument Type: LongUpDownCounterCallback
