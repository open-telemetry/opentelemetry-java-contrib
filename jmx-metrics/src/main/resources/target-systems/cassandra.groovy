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

def cassandraMetrics = "org.apache.cassandra.metrics"
def clientRequest = "${cassandraMetrics}:type=ClientRequest"
def clientRequestRangeSlice = "${clientRequest},scope=RangeSlice"

def clientRequestRangeSliceLatency = otel.mbean("${clientRequestRangeSlice},name=Latency")
otel.instrument(clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.50p",
        "Token range read request latency - 50th percentile", "us", "50thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.99p",
        "Token range read request latency - 99th percentile", "us", "99thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.max",
        "Maximum token range read request latency", "us", "Max",
        otel.&doubleValueCallback)

def clientRequestRead = "${clientRequest},scope=Read"
def clientRequestReadLatency = otel.mbean("${clientRequestRead},name=Latency")
otel.instrument(clientRequestReadLatency,
        "cassandra.client.request.read.latency.50p",
        "Standard read request latency - 50th percentile", "us", "50thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestReadLatency,
        "cassandra.client.request.read.latency.99p",
        "Standard read request latency - 99th percentile", "us", "99thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestReadLatency,
        "cassandra.client.request.read.latency.max",
        "Maximum standard read request latency", "us", "Max",
        otel.&doubleValueCallback)

def clientRequestWrite = "${clientRequest},scope=Write"
def clientRequestWriteLatency = otel.mbean("${clientRequestWrite},name=Latency")
otel.instrument(clientRequestWriteLatency,
        "cassandra.client.request.write.latency.50p",
        "Regular write request latency - 50th percentile", "us", "50thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestWriteLatency,
        "cassandra.client.request.write.latency.99p",
        "Regular write request latency - 99th percentile", "us", "99thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestWriteLatency,
        "cassandra.client.request.write.latency.max",
        "Maximum regular write request latency", "us", "Max",
        otel.&doubleValueCallback)

def storage = "${cassandraMetrics}:type=Storage"
def storageLoad = otel.mbean("${storage},name=Load")
otel.instrument(storageLoad,
        "cassandra.storage.load.count",
        "Size of the on disk data size this node manages", "by", "Count",
        otel.&longUpDownCounterCallback)

def storageTotalHints = otel.mbean("${storage},name=TotalHints")
otel.instrument(storageTotalHints,
        "cassandra.storage.total_hints.count",
        "Number of hint messages written to this node since [re]start", "1", "Count",
        otel.&longCounterCallback)

def storageTotalHintsInProgress = otel.mbean("${storage},name=TotalHintsInProgress")
otel.instrument(storageTotalHintsInProgress,
        "cassandra.storage.total_hints.in_progress.count",
        "Number of hints attempting to be sent currently", "1", "Count",
        otel.&longUpDownCounterCallback)


def compaction = "${cassandraMetrics}:type=Compaction"
def compactionPendingTasks = otel.mbean("${compaction},name=PendingTasks")
otel.instrument(compactionPendingTasks,
        "cassandra.compaction.tasks.pending",
        "Estimated number of compactions remaining to perform", "1", "Value",
        otel.&longValueCallback)

def compactionCompletedTasks = otel.mbean("${compaction},name=CompletedTasks")
otel.instrument(compactionCompletedTasks,
        "cassandra.compaction.tasks.completed",
        "Number of completed compactions since server [re]start", "1", "Value",
        otel.&longCounterCallback)


def clientRequests = otel.mbeans([
  "${clientRequestRangeSlice},name=Latency",
  "${clientRequestRead},name=Latency",
  "${clientRequestWrite},name=Latency",
])

otel.instrument(clientRequests,
  "cassandra.client.request.count",
  "Number of requests by operation",
  "1",
  [
    "operation" : { mbean -> mbean.name().getKeyProperty("scope") },
  ],
  "Count", otel.&longCounterCallback)

def clientRequestErrors = otel.mbeans([
  "${clientRequestRangeSlice},name=Unavailables",
  "${clientRequestRangeSlice},name=Timeouts",
  "${clientRequestRangeSlice},name=Failures",
  "${clientRequestRead},name=Unavailables",
  "${clientRequestRead},name=Timeouts",
  "${clientRequestRead},name=Failures",
  "${clientRequestWrite},name=Unavailables",
  "${clientRequestWrite},name=Timeouts",
  "${clientRequestWrite},name=Failures",
])

otel.instrument(clientRequestErrors,
  "cassandra.client.request.error.count",
  "Number of request errors by operation",
  "1",
  [
    "operation" : { mbean -> mbean.name().getKeyProperty("scope") },
    "status" : {
      mbean -> switch(mbean.name().getKeyProperty("name")) {
        case "Unavailables":
          return "Unavailable"
          break
        case "Timeouts":
          return "Timeout"
          break
        case "Failures":
          return "Failure"
          break
      }
    }
  ],
  "Count", otel.&longCounterCallback)
