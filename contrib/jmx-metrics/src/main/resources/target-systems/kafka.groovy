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

def messagesInPerSec = otel.mbean("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec")
otel.instrument(messagesInPerSec, "kafka.messages.in", "number of messages in per second",
        "1", "Count", otel.&longCounter)

def bytesInPerSec = otel.mbean("kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec")
otel.instrument(bytesInPerSec, "kafka.bytes.in", "bytes in per second from clients",
        "by", "Count", otel.&longCounter)

def bytesOutPerSec = otel.mbean("kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec")
otel.instrument(bytesOutPerSec, "kafka.bytes.out", "bytes out per second to clients",
        "by", "Count", otel.&longCounter)

def isrShrinksPerSec = otel.mbean("kafka.server:type=ReplicaManager,name=IsrShrinksPerSec")
otel.instrument(isrShrinksPerSec, "kafka.isr.shrinks", "in-sync replica shrinks per second",
        "1", "Count", otel.&longCounter)

def isrExpandsPerSec = otel.mbean("kafka.server:type=ReplicaManager,name=IsrExpandsPerSec")
otel.instrument(isrExpandsPerSec, "kafka.isr.expands", "in-sync replica expands per second",
        "1", "Count", otel.&longCounter)

def maxLag = otel.mbean("kafka.server:type=ReplicaFetcherManager,name=MaxLag,clientId=Replica")
otel.instrument(maxLag, "kafka.max.lag", "max lag in messages between follower and leader replicas",
        "1", "Value", otel.&longUpDownCounter)

def activeControllerCount = otel.mbean("kafka.controller:type=KafkaController,name=ActiveControllerCount")
otel.instrument(activeControllerCount, "kafka.controller.active.count", "controller is active on broker",
        "1", "Value", otel.&longUpDownCounter)

def offlinePartitionsCount = otel.mbean("kafka.controller:type=KafkaController,name=OfflinePartitionsCount")
otel.instrument(offlinePartitionsCount, "kafka.partitions.offline.count", "number of partitions without an active leader",
        "1", "Value", otel.&longUpDownCounter)

def underReplicatedPartitionsCount = otel.mbean("kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions")
otel.instrument(underReplicatedPartitionsCount, "kafka.partitions.underreplicated.count", "number of under replicated partitions",
        "1", "Value", otel.&longUpDownCounter)

def leaderElectionRate = otel.mbean("kafka.controller:type=ControllerStats,name=LeaderElectionRateAndTimeMs")
otel.instrument(leaderElectionRate, "kafka.leader.election.rate", "leader election rate - non-zero indicates broker failures",
        "1", "Count", otel.&longCounter)

def uncleanLeaderElections = otel.mbean("kafka.controller:type=ControllerStats,name=UncleanLeaderElectionsPerSec")
otel.instrument(uncleanLeaderElections, "kafka.unclean.election.rate", "unclean leader election rate - non-zero indicates broker failures",
        "1", "Count", otel.&longCounter)

def requestQueueSize = otel.mbean("kafka.network:type=RequestChannel,name=RequestQueueSize")
otel.instrument(requestQueueSize, "kafka.request.queue", "size of the request queue",
        "1", "Value", otel.&longUpDownCounter)

def fetchConsumer = otel.mbean("kafka.network:type=RequestMetrics,name=TotalTimeMs,request=FetchConsumer")
otel.instrument(fetchConsumer, "kafka.fetch.consumer.total.time.count", "fetch consumer request count",
        "1", "Count", otel.&longCounter)
otel.instrument(fetchConsumer, "kafka.fetch.consumer.total.time.median", "fetch consumer request time - 50th percentile",
        "ms", "50thPercentile", otel.&doubleUpDownCounter)
otel.instrument(fetchConsumer, "kafka.fetch.consumer.total.time.99p", "fetch consumer request time - 99th percentile",
        "ms", "99thPercentile", otel.&doubleUpDownCounter)

def fetchFollower = otel.mbean("kafka.network:type=RequestMetrics,name=TotalTimeMs,request=FetchFollower")
otel.instrument(fetchFollower, "kafka.fetch.follower.total.time.count", "fetch follower request count",
        "1", "Count", otel.&longCounter)
otel.instrument(fetchFollower, "kafka.fetch.follower.total.time.median", "fetch follower request time - 50th percentile",
        "ms", "50thPercentile", otel.&doubleUpDownCounter)
otel.instrument(fetchFollower, "kafka.fetch.follower.total.time.99p", "fetch follower request time - 99th percentile",
        "ms", "99thPercentile", otel.&doubleUpDownCounter)

def produce = otel.mbean("kafka.network:type=RequestMetrics,name=TotalTimeMs,request=Produce")
otel.instrument(produce, "kafka.produce.total.time.count", "produce request count",
        "1", "Count", otel.&longCounter)
otel.instrument(produce, "kafka.produce.total.time.median", "produce request time - 50th percentile",
        "ms", "50thPercentile", otel.&doubleUpDownCounter)
otel.instrument(produce, "kafka.produce.total.time.99p", "produce request time - 99th percentile",
        "ms", "99thPercentile", otel.&doubleUpDownCounter)

def logFlushRate = otel.mbean("kafka.log:type=LogFlushStats,name=LogFlushRateAndTimeMs")
otel.instrument(logFlushRate, "kafka.logs.flush.time.count", "log flush count",
        "1", "Count", otel.&longCounter)
otel.instrument(logFlushRate, "kafka.logs.flush.time.median", "log flush time - 50th percentile",
        "ms", "50thPercentile", otel.&doubleUpDownCounter)
otel.instrument(logFlushRate, "kafka.logs.flush.time.99p", "log flush time - 99th percentile",
        "ms", "99thPercentile", otel.&doubleUpDownCounter)
