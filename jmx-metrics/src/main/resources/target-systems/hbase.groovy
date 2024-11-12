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

def beanMasterServer = otel.mbeans("Hadoop:service=HBase,name=Master,sub=Server")
otel.instrument(beanMasterServer, "hbase.master.region_server.count",
  "The number of region servers.", "{server}",
  ["numDeadRegionServers":["state" : {"dead"}], "numRegionServers": ["state" : {"live"}]],
  otel.&longUpDownCounterCallback)

def beanMasterAssignmentManager = otel.mbean("Hadoop:service=HBase,name=Master,sub=AssignmentManager")
otel.instrument(beanMasterAssignmentManager, "hbase.master.regions_in_transition.count",
  "The number of regions that are in transition.", "{region}",
  "ritCount", otel.&longUpDownCounterCallback)
otel.instrument(beanMasterAssignmentManager, "hbase.master.regions_in_transition.over_threshold",
  "The number of regions that have been in transition longer than a threshold time.", "{region}",
  "ritCountOverThreshold", otel.&longUpDownCounterCallback)
otel.instrument(beanMasterAssignmentManager, "hbase.master.regions_in_transition.oldest_age",
  "The age of the longest region in transition.", "ms",
  "ritOldestAge", otel.&longValueCallback)

def beanRegionServerServer = otel.mbean("Hadoop:service=HBase,name=RegionServer,sub=Server")
otel.instrument(beanRegionServerServer, "hbase.region_server.region.count",
  "The number of regions hosted by the region server.", "{region}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "regionCount", otel.&longUpDownCounterCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.disk.store_file.count",
  "The number of store files on disk currently managed by the region server.", "{file}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "storeFileCount", otel.&longUpDownCounterCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.disk.store_file.size",
  "Aggregate size of the store files on disk.", "By",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "storeFileSize", otel.&longUpDownCounterCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.write_ahead_log.count",
  "The number of write ahead logs not yet archived.", "{log}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "hlogFileCount", otel.&longUpDownCounterCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.request.count",
  "The number of requests received.", "{request}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  ["writeRequestCount":["state" : {"write"}], "readRequestCount": ["state" : {"read"}]],
  otel.&longUpDownCounterCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.queue.length",
  "The number of RPC handlers actively servicing requests.", "{handler}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  ["flushQueueLength":["state" : {"flush"}], "compactionQueueLength": ["state" : {"compaction"}]],
  otel.&longUpDownCounterCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.blocked_update.time",
  "Amount of time updates have been blocked so the memstore can be flushed.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "updatesBlockedTime", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.block_cache.operation.count",
  "Number of block cache hits/misses.", "{operation}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  ["blockCacheMissCount":["state" : {"miss"}], "blockCacheHitCount": ["state" : {"hit"}]],
  otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.files.local",
  "Percent of store file data that can be read from the local.", "%",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "percentFilesLocal", otel.&doubleValueCallback)

otel.instrument(beanRegionServerServer, "hbase.region_server.operation.append.latency.p99",
  "Append operation 99th Percentile latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Append_99th_percentile", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.append.latency.max",
  "Append operation max latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Append_max", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.append.latency.min",
  "Append operation minimum latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Append_min", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.append.latency.mean",
  "Append operation mean latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Append_mean", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.append.latency.median",
  "Append operation median latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Append_median", otel.&longValueCallback)

otel.instrument(beanRegionServerServer, "hbase.region_server.operation.delete.latency.p99",
  "Delete operation 99th Percentile latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Delete_99th_percentile", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.delete.latency.max",
  "Delete operation max latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Delete_max", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.delete.latency.min",
  "Delete operation minimum latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Delete_min", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.delete.latency.mean",
  "Delete operation mean latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Delete_mean", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.delete.latency.median",
  "Delete operation median latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Delete_median", otel.&longValueCallback)

otel.instrument(beanRegionServerServer, "hbase.region_server.operation.put.latency.p99",
  "Put operation 99th Percentile latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Put_99th_percentile", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.put.latency.max",
  "Put operation max latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Put_max", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.put.latency.min",
  "Put operation minimum latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Put_min", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.put.latency.mean",
  "Put operation mean latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Put_mean", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.put.latency.median",
  "Put operation median latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Put_median", otel.&longValueCallback)

otel.instrument(beanRegionServerServer, "hbase.region_server.operation.get.latency.p99",
  "Get operation 99th Percentile latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Get_99th_percentile", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.get.latency.max",
  "Get operation max latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Get_max", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.get.latency.min",
  "Get operation minimum latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Get_min", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.get.latency.mean",
  "Get operation mean latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Get_mean", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.get.latency.median",
  "Get operation median latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Get_median", otel.&longValueCallback)

otel.instrument(beanRegionServerServer, "hbase.region_server.operation.replay.latency.p99",
  "Replay operation 99th Percentile latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Replay_99th_percentile", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.replay.latency.max",
  "Replay operation max latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Replay_max", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.replay.latency.min",
  "Replay operation minimum latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Replay_min", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.replay.latency.mean",
  "Replay operation mean latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Replay_mean", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.replay.latency.median",
  "Replay operation median latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Replay_median", otel.&longValueCallback)

otel.instrument(beanRegionServerServer, "hbase.region_server.operation.increment.latency.p99",
  "Increment operation 99th Percentile latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Increment_99th_percentile", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.increment.latency.max",
  "Increment operation max latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Increment_max", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.increment.latency.min",
  "Increment operation minimum latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Increment_min", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.increment.latency.mean",
  "Increment operation mean latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Increment_mean", otel.&longValueCallback)
otel.instrument(beanRegionServerServer, "hbase.region_server.operation.increment.latency.median",
  "Increment operation median latency.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "Increment_median", otel.&longValueCallback)

otel.instrument(beanRegionServerServer, "hbase.region_server.operations.slow",
  "Number of operations that took over 1000ms to complete.", "{operation}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  [
    "slowDeleteCount":["operation" : {"delete"}],
    "slowAppendCount": ["operation" : {"append"}],
    "slowGetCount": ["operation" : {"get"}],
    "slowPutCount": ["operation" : {"put"}],
    "slowIncrementCount": ["operation" : {"increment"}]
  ],
  otel.&longUpDownCounterCallback)

def beanRegionServerIPC = otel.mbean("Hadoop:service=HBase,name=RegionServer,sub=IPC")
otel.instrument(beanRegionServerIPC, "hbase.region_server.open_connection.count",
  "The number of open connections at the RPC layer.", "{connection}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "numOpenConnections", otel.&longUpDownCounterCallback)
otel.instrument(beanRegionServerIPC, "hbase.region_server.active_handler.count",
  "The number of RPC handlers actively servicing requests.", "{handler}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "numActiveHandler", otel.&longUpDownCounterCallback)
otel.instrument(beanRegionServerIPC, "hbase.region_server.queue.request.count",
  "The number of currently enqueued requests.", "{request}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  [
    "numCallsInReplicationQueue":["state" : {"replication"}],
    "numCallsInGeneralQueue": ["state" : {"user"}],
    "numCallsInPriorityQueue": ["state" : {"priority"}]
  ],
  otel.&longUpDownCounterCallback)
otel.instrument(beanRegionServerIPC, "hbase.region_server.authentication.count",
  "Number of client connection authentication failures/successes.", "{authentication request}",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  ["authenticationSuccesses":["state" : {"successes"}], "authenticationFailures": ["state" : {"failures"}]],
  otel.&longUpDownCounterCallback)

def beanJVMMetrics = otel.mbean("Hadoop:service=HBase,name=JvmMetrics")
otel.instrument(beanJVMMetrics, "hbase.region_server.gc.time",
  "Time spent in garbage collection.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "GcTimeMillis", otel.&longCounterCallback)
otel.instrument(beanJVMMetrics, "hbase.region_server.gc.young_gen.time",
  "Time spent in garbage collection of the young generation.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "GcTimeMillisParNew", otel.&longCounterCallback)
otel.instrument(beanJVMMetrics, "hbase.region_server.gc.old_gen.time",
  "Time spent in garbage collection of the old generation.", "ms",
  ["region_server" : { mbean -> mbean.getProperty("tag.Hostname") }],
  "GcTimeMillisConcurrentMarkSweep", otel.&longCounterCallback)
