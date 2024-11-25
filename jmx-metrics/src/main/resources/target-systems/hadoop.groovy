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
otel.instrument(beanHadoopNameNodeFS, "hadoop.name_node.capacity.usage", "The current used capacity across all data nodes reporting to the name node.", "by",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "CapacityUsed", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.name_node.capacity.limit", "The total capacity allotted to data nodes reporting to the name node.", "by",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "CapacityTotal", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.name_node.block.count", "The total number of blocks on the name node.", "{block}",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "BlocksTotal", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.name_node.block.missing", "The number of blocks reported as missing to the name node.", "{block}",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "MissingBlocks", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.name_node.block.corrupt", "The number of blocks reported as corrupt to the name node.", "{block}",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "CorruptBlocks", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.name_node.volume.failed", "The number of failed volumes reported to the name node.", "{volume}",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "VolumeFailuresTotal", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.name_node.file.count", "The total number of files being tracked by the name node.", "{file}",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "FilesTotal", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.name_node.file.load", "The current number of concurrent file accesses.", "{operation}",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "TotalLoad", otel.&longUpDownCounterCallback)
otel.instrument(beanHadoopNameNodeFS, "hadoop.name_node.data_node.count", "The number of data nodes reporting to the name node.", "{node}",
  ["node_name" : { mbean -> mbean.getProperty("tag.Hostname") }],
  ["NumLiveDataNodes":["state":{"live"}], "NumDeadDataNodes": ["state":{"dead"}]],
  otel.&longUpDownCounterCallback)
