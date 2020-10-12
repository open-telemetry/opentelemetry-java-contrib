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

class CassandraMBean {
    // Necessary to have script-bound `otel` and `log` in class scope
    protected Binding sb
    protected GroovyMBean mbean

    private String objectName

    CassandraMBean(Binding scriptBinding, objectName) {
        sb = scriptBinding
        this.objectName = objectName
    }

    def fetch() {
        def mbeans = sb.otel.queryJmx(objectName)
        if (mbeans.size() == 0) {
            sb.log.warning("CassandraMBean.fetch(): Failed to fetch MBean ${objectName}.")
        } else {
            sb.log.fine("CassandraMBean.fetch(): Fetched ${mbeans.size()} MBeans - ${mbeans}")
            mbean = mbeans.first()
        }
    }

    def getAttribute(String attribute) {
        if (mbean == null) {
            return null
        }
        return mbean.getProperty(attribute)
    }
}

class CassandraMetric {
    private CassandraMBean cassandraMBean
    private Binding sb
    private String instrumentName
    private String description
    private String unit
    private String attribute
    private Closure instrument

    CassandraMetric(CassandraMBean cassandraMBean, String instrumentName, String description, String unit, String attribute, Closure instrument) {
        this.cassandraMBean = cassandraMBean
        this.sb = cassandraMBean.sb
        this.instrumentName = instrumentName
        this.description = description
        this.unit = unit
        this.attribute = attribute
        this.instrument = instrument
    }

    def update() {
        def value = cassandraMBean.getAttribute(attribute)
        if (value == null) {
            sb.log.warning("No valid value for ${instrumentName} - ${cassandraMBean}.${attribute}" as String)
            return
        }
        def inst = instrument(instrumentName, description, unit)
        sb.log.fine("Recording ${instrumentName} - ${inst} w/ ${value}")
        inst.add(value)
    }
}

def cassandraMetrics = "org.apache.cassandra.metrics"

def clientRequest = "${cassandraMetrics}:type=ClientRequest"
def clientRequestRangeSlice = "${clientRequest},scope=RangeSlice"
def clientRequestRead = "${clientRequest},scope=Read"
def clientRequestWrite = "${clientRequest},scope=Write"
def compaction = "${cassandraMetrics}:type=Compaction"
def storage = "${cassandraMetrics}:type=Storage"

def clientRequestRangeSliceLatency = new CassandraMBean(binding, "${clientRequestRangeSlice},name=Latency")
def clientRequestRangeSliceTimeouts = new CassandraMBean(binding, "${clientRequestRangeSlice},name=Timeouts")
def clientRequestRangeSliceUnavailables = new CassandraMBean(binding, "${clientRequestRangeSlice},name=Unavailables")
def clientRequestReadLatency = new CassandraMBean(binding, "${clientRequestRead},name=Latency")
def clientRequestReadTimeouts = new CassandraMBean(binding, "${clientRequestRead},name=Timeouts")
def clientRequestReadUnavailables = new CassandraMBean(binding, "${clientRequestRead},name=Unavailables")
def clientRequestWriteLatency = new CassandraMBean(binding, "${clientRequestWrite},name=Latency")
def clientRequestWriteTimeouts = new CassandraMBean(binding, "${clientRequestWrite},name=Timeouts")
def clientRequestWriteUnavailables = new CassandraMBean(binding, "${clientRequestWrite},name=Unavailables")
def compactionCompletedTasks = new CassandraMBean(binding, "${compaction},name=CompletedTasks")
def compactionPendingTasks = new CassandraMBean(binding, "${compaction},name=PendingTasks")
def storageLoad = new CassandraMBean(binding, "${storage},name=Load")
def storageTotalHints = new CassandraMBean(binding, "${storage},name=TotalHints")
def storageTotalHintsInProgress = new CassandraMBean(binding, "${storage},name=TotalHintsInProgress")

def clientRequestRangeSliceLatency50p = new CassandraMetric(
        clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.50p",
        "Token range read request latency - 50th percentile", "µs", "50thPercentile",
        otel.&doubleUpDownCounter)

def clientRequestRangeSliceLatency99p = new CassandraMetric(
        clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.99p",
        "Token range read request latency - 99th percentile", "µs", "99thPercentile",
        otel.&doubleUpDownCounter)

def clientRequestRangeSliceLatencyCount = new CassandraMetric(
        clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.count",
        "Total token range read request latency", "µs", "Count",
        otel.&longCounter)

def clientRequestRangeSliceLatencyMax = new CassandraMetric(
        clientRequestRangeSliceLatency,
        "cassandra.client.request.range_slice.latency.max",
        "Maximum token range read request latency", "µs", "Max",
        otel.&doubleUpDownCounter)

def clientRequestRangeSliceTimeoutCount = new CassandraMetric(
        clientRequestRangeSliceTimeouts,
        "cassandra.client.request.range_slice.timeout.count",
        "Number of token range read request timeouts encountered", "1", "Count",
        otel.&longCounter)

def clientRequestRangeSliceUnavailableCount = new CassandraMetric(
        clientRequestRangeSliceUnavailables,
        "cassandra.client.request.range_slice.unavailable.count",
        "Number of token range read request unavailable exceptions encountered", "1", "Count",
        otel.&longCounter)

def clientRequestReadLatency50p = new CassandraMetric(clientRequestReadLatency,
        "cassandra.client.request.read.latency.50p",
        "Standard read request latency - 50th percentile", "µs", "50thPercentile",
        otel.&doubleUpDownCounter)

def clientRequestReadLatency99p = new CassandraMetric(
        clientRequestReadLatency,
        "cassandra.client.request.read.latency.99p",
        "Standard read request latency - 99th percentile", "µs", "99thPercentile",
        otel.&doubleUpDownCounter)

def clientRequestReadLatencyCount = new CassandraMetric(
        clientRequestReadLatency,
        "cassandra.client.request.read.latency.count",
        "Total standard read request latency", "µs", "Count",
        otel.&longCounter)

def clientRequestReadLatencyMax = new CassandraMetric(
        clientRequestReadLatency,
        "cassandra.client.request.read.latency.max",
        "Maximum standard read request latency", "µs", "Max",
        otel.&doubleUpDownCounter)

def clientRequestReadTimeoutCount = new CassandraMetric(
        clientRequestReadTimeouts,
        "cassandra.client.request.read.timeout.count",
        "Number of standard read request timeouts encountered", "1", "Count",
        otel.&longCounter)

def clientRequestReadUnavailableCount = new CassandraMetric(
        clientRequestReadUnavailables,
        "cassandra.client.request.read.unavailable.count",
        "Number of standard read request unavailable exceptions encountered", "1", "Count",
        otel.&longCounter)

def clientRequestWriteLatency50p = new CassandraMetric(
        clientRequestWriteLatency,
        "cassandra.client.request.write.latency.50p",
        "Regular write request latency - 50th percentile", "µs", "50thPercentile",
        otel.&doubleUpDownCounter)

def clientRequestWriteLatency99p = new CassandraMetric(
        clientRequestWriteLatency,
        "cassandra.client.request.write.latency.99p",
        "Regular write request latency - 99th percentile", "µs", "99thPercentile",
        otel.&doubleUpDownCounter)

def clientRequestWriteLatencyCount = new CassandraMetric(
        clientRequestWriteLatency,
        "cassandra.client.request.write.latency.count",
        "Total regular write request latency", "µs", "Count",
        otel.&longCounter)

def clientRequestWriteLatencyMax = new CassandraMetric(
        clientRequestWriteLatency,
        "cassandra.client.request.write.latency.max",
        "Maximum regular write request latency", "µs", "Max",
        otel.&doubleUpDownCounter)

def clientRequestWriteTimeoutCount = new CassandraMetric(
        clientRequestWriteTimeouts,
        "cassandra.client.request.write.timeout.count",
        "Number of regular write request timeouts encountered", "1", "Count",
        otel.&longCounter)

def clientRequestWriteUnavailableCount = new CassandraMetric(
        clientRequestWriteUnavailables,
        "cassandra.client.request.write.unavailable.count",
        "Number of regular write request unavailable exceptions encountered", "1", "Count",
        otel.&longCounter)

def storageLoadCount = new CassandraMetric(
        storageLoad,
        "cassandra.storage.load.count",
        "Size of the on disk data size this node manages", "by", "Count",
        otel.&longUpDownCounter)

def storageTotalHintsCount = new CassandraMetric(
        storageTotalHints,
        "cassandra.storage.total_hints.count",
        "Number of hint messages written to this node since [re]start", "1", "Count",
        otel.&longCounter)

def storageTotalHintsInProgressCount = new CassandraMetric(
        storageTotalHintsInProgress,
        "cassandra.storage.total_hints.in_progress.count",
        "Number of hints attempting to be sent currently", "1", "Count",
        otel.&longUpDownCounter)

def compactionTasksPending = new CassandraMetric(
        compactionPendingTasks,
        "cassandra.compaction.tasks.pending",
        "Estimated number of compactions remaining to perform", "1", "Value",
        otel.&longUpDownCounter)

def compactionTasksCompleted = new CassandraMetric(
        compactionCompletedTasks,
        "cassandra.compaction.tasks.completed",
        "Number of completed compactions since server [re]start", "1", "Value",
        otel.&longCounter)

[
    clientRequestRangeSliceLatency,
    clientRequestRangeSliceTimeouts,
    clientRequestRangeSliceUnavailables,
    clientRequestReadLatency,
    clientRequestReadTimeouts,
    clientRequestReadUnavailables,
    clientRequestWriteLatency,
    clientRequestWriteTimeouts,
    clientRequestWriteUnavailables,
    compactionCompletedTasks,
    compactionPendingTasks,
    storageLoad,
    storageTotalHints,
    storageTotalHintsInProgress,
].each {
    it.fetch()
}

[
    clientRequestRangeSliceLatency50p,
    clientRequestRangeSliceLatency99p,
    clientRequestRangeSliceLatencyCount,
    clientRequestRangeSliceLatencyMax,
    clientRequestRangeSliceTimeoutCount,
    clientRequestRangeSliceUnavailableCount,
    clientRequestReadLatency50p,
    clientRequestReadLatency99p,
    clientRequestReadLatencyCount,
    clientRequestReadLatencyMax,
    clientRequestReadTimeoutCount,
    clientRequestReadUnavailableCount,
    clientRequestWriteLatency50p,
    clientRequestWriteLatency99p,
    clientRequestWriteLatencyCount,
    clientRequestWriteLatencyMax,
    clientRequestWriteTimeoutCount,
    clientRequestWriteUnavailableCount,
    compactionTasksCompleted,
    compactionTasksPending,
    storageLoadCount,
    storageTotalHintsCount,
    storageTotalHintsInProgressCount,
].each {
    it.update()
}
