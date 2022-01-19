/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

def beanHadoopNameNodeFS = otel.mbean("Hadoop:service=NameNode,name=FSNamesystem")
otel.instrument(beanHadoopNameNodeFS, "hadoop.hdfs.disk.usage", "The amount of disk used by data nodes.", "by",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "CapacityUsed", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.hdfs.disk.limit", "The total disk allotted to data nodes.", "by",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "CapacityTotal", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.hdfs.block.count", "The total number of blocks.", "blocks",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "BlocksTotal", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.hdfs.block.missing", "The number of blocks reported as missing.", "blocks",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "MissingBlocks", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.hdfs.block.corrupt", "The number of blocks reported as corrupt.", "blocks",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "CorruptBlocks", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.hdfs.volume.failed", "The number of failed volumes.", "volumes",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "VolumeFailuresTotal", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.hdfs.file.count", "The total number of files being tracked by the name node.", "files",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "TotalFiles", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.hdfs.file.load", "The current number of concurrent file accesses.", "operations",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "TotalLoad", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.data_node.count", "The number of data nodes tracked by the name node.", "nodes",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  ["NumLiveDataNodes":["state":{"live"}], "NumDeadDataNodes": ["state":{"dead"}]],
  otel.&longUpDownCounterCallback)
