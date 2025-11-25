# Hadoop Metrics

The JMX Metric Gatherer provides built in Hadoop metric gathering capabilities.
These metrics are sourced from: <https://hadoop.apache.org/docs/r2.7.2/hadoop-project-dist/hadoop-common/Metrics.html>

## Name Node Metrics

### hadoop.name_node.capacity.usage

* Name: `hadoop.name_node.capacity.usage`
* Description: The current used capacity across all data nodes reporting to the name node.
* Unit: `by`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

### hadoop.name_node.capacity.limit

* Name: `hadoop.name_node.capacity.limit`
* Description: The total capacity allotted to data nodes reporting to the name node.
* Unit: `by`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

### hadoop.name_node.block.count

* Name: `hadoop.name_node.block.count`
* Description: The total number of blocks on the name node.
* Unit: `{blocks}`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

### hadoop.name_node.block.missing

* Name: `hadoop.name_node.block.missing`
* Description: The number of blocks reported as missing to the name node.
* Unit: `{blocks}`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

### hadoop.name_node.block.corrupt

* Name: `hadoop.name_node.block.corrupt`
* Description: The number of blocks reported as corrupt to the name node.
* Unit: `{blocks}`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

### hadoop.name_node.volume.failed

* Name: `hadoop.name_node.volume.failed`
* Description: The number of failed volumes reported to the name node.
* Unit: `{volumes}`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

### hadoop.name_node.file.count

* Name: `hadoop.name_node.file.count`
* Description: The total number of files being tracked by the name node.
* Unit: `{files}`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

### hadoop.name_node.file.load

* Name: `hadoop.name_node.file.load`
* Description: The current number of concurrent file accesses.
* Unit: `{operations}`
* Labels: `node_name`
* Instrument Type: LongUpDownCounterCallback

### hadoop.name_node.data_node.count

* Name: `hadoop.name_node.data_node.count`
* Description: The number of data nodes reporting to the name node.
* Unit: `{nodes}`
* Labels: `node_name`, `state`
* Instrument Type: LongUpDownCounterCallback
