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
        "Token range read request latency - 50th percentile", "µs", "50thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.99p",
        "Token range read request latency - 99th percentile", "µs", "99thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.count",
        "Number of token range read request operations", "1", "Count",
        otel.&longCounterCallback)

otel.instrument(clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.max",
        "Maximum token range read request latency", "µs", "Max",
        otel.&doubleValueCallback)

def clientRequestRangeSliceTimeouts = otel.mbean("${clientRequestRangeSlice},name=Timeouts")
otel.instrument(clientRequestRangeSliceTimeouts,
        "cassandra.client.request.range_slice.timeout.count",
        "Number of token range read request timeouts encountered", "1", "Count",
        otel.&longCounterCallback)

def clientRequestRangeSliceUnavailables = otel.mbean("${clientRequestRangeSlice},name=Unavailables")
otel.instrument(clientRequestRangeSliceUnavailables,
        "cassandra.client.request.range_slice.unavailable.count",
        "Number of token range read request unavailable exceptions encountered", "1", "Count",
        otel.&longCounterCallback)

def clientRequestRead = "${clientRequest},scope=Read"
def clientRequestReadLatency = otel.mbean("${clientRequestRead},name=Latency")
otel.instrument(clientRequestReadLatency,
        "cassandra.client.request.read.latency.50p",
        "Standard read request latency - 50th percentile", "µs", "50thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestReadLatency,
        "cassandra.client.request.read.latency.99p",
        "Standard read request latency - 99th percentile", "µs", "99thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestReadLatency,
        "cassandra.client.request.read.latency.count",
        "Number of standard read request operations", "1", "Count",
        otel.&longCounterCallback)

otel.instrument(clientRequestReadLatency,
        "cassandra.client.request.read.latency.max",
        "Maximum standard read request latency", "µs", "Max",
        otel.&doubleValueCallback)

def clientRequestReadTimeouts = otel.mbean("${clientRequestRead},name=Timeouts")
otel.instrument(clientRequestReadTimeouts,
        "cassandra.client.request.read.timeout.count",
        "Number of standard read request timeouts encountered", "1", "Count",
        otel.&longCounterCallback)

def clientRequestReadUnavailables = otel.mbean("${clientRequestRead},name=Unavailables")
otel.instrument(clientRequestReadUnavailables,
        "cassandra.client.request.read.unavailable.count",
        "Number of standard read request unavailable exceptions encountered", "1", "Count",
        otel.&longCounterCallback)

def clientRequestWrite = "${clientRequest},scope=Write"
def clientRequestWriteLatency = otel.mbean("${clientRequestWrite},name=Latency")
otel.instrument(clientRequestWriteLatency,
        "cassandra.client.request.write.latency.50p",
        "Regular write request latency - 50th percentile", "µs", "50thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestWriteLatency,
        "cassandra.client.request.write.latency.99p",
        "Regular write request latency - 99th percentile", "µs", "99thPercentile",
        otel.&doubleValueCallback)

otel.instrument(clientRequestWriteLatency,
        "cassandra.client.request.write.latency.count",
        "Number of regular write request operations", "1", "Count",
        otel.&longCounterCallback)

otel.instrument(clientRequestWriteLatency,
        "cassandra.client.request.write.latency.max",
        "Maximum regular write request latency", "µs", "Max",
        otel.&doubleValueCallback)

def clientRequestWriteTimeouts = otel.mbean("${clientRequestWrite},name=Timeouts")
otel.instrument(clientRequestWriteTimeouts,
        "cassandra.client.request.write.timeout.count",
        "Number of regular write request timeouts encountered", "1", "Count",
        otel.&longCounterCallback)

def clientRequestWriteUnavailables = otel.mbean("${clientRequestWrite},name=Unavailables")
otel.instrument(clientRequestWriteUnavailables,
        "cassandra.client.request.write.unavailable.count",
        "Number of regular write request unavailable exceptions encountered", "1", "Count",
        otel.&longCounterCallback)

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
