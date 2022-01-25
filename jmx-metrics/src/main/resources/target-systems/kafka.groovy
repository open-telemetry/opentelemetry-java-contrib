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

def messagesInPerSec = otel.mbean("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec,topic=*")
otel.instrument(messagesInPerSec,
  "kafka.message.count",
  "The number of messages received by the broker",
  "messages",
  [
    "topic" : { mbean -> mbean.name().getKeyProperty("topic") },
  ],
  "Count", otel.&longCounterCallback)

def requests = otel.mbeans(["kafka.server:type=BrokerTopicMetrics,name=TotalProduceRequestsPerSec,topic=*",
                           "kafka.server:type=BrokerTopicMetrics,name=TotalFetchRequestsPerSec,topic=*"])
otel.instrument(requests,
  "kafka.request.count",
  "The number of requests received by the broker",
  "requests",
  [
    "topic" : { mbean -> mbean.name().getKeyProperty("topic") },
    "type" : { mbean -> switch(mbean.name().getKeyProperty("name")) {
      case "TotalProduceRequestsPerSec":
        return "Produce"
        break
      case "TotalFetchRequestsPerSec":
        return "Fetch"
        break
      }
    },
  ],
  "Count", otel.&longCounterCallback)

def failedRequests = otel.mbeans(["kafka.server:type=BrokerTopicMetrics,name=FailedProduceRequestsPerSec,topic=*",
                           "kafka.server:type=BrokerTopicMetrics,name=FailedFetchRequestsPerSec,topic=*"])
otel.instrument(failedRequests,
  "kafka.request.failed",
  "The number of requests to the broker resulting in a failure",
  "requests",
  [
    "topic" : { mbean -> mbean.name().getKeyProperty("topic") },
    "type" : { mbean -> switch(mbean.name().getKeyProperty("name")) {
      case "FailedProduceRequestsPerSec":
        return "Produce"
        break
      case "FailedFetchRequestsPerSec":
        return "Fetch"
        break
      }
    },
  ],
  "Count", otel.&longCounterCallback)

def requestTime = otel.mbean("kafka.network:type=RequestMetrics,name=TotalTimeMs,request=*")
otel.instrument(requestTime,
  "kafka.request.time.total",
  "The total time the broker has taken to service requests",
  "ms",
  [
    "type" : { mbean -> mbean.name().getKeyProperty("request") },
  ],
  "Count", otel.&longCounterCallback)
otel.instrument(requestTime,
  "kafka.request.time.50p",
  "The 50th percentile time the broker has taken to service requests",
  "ms",
  [
    "type" : { mbean -> mbean.name().getKeyProperty("request") },
  ],
  "50thPercentile", otel.&doubleValueCallback)
otel.instrument(requestTime,
  "kafka.request.time.99p",
  "The 99th percentile time the broker has taken to service requests",
  "ms",
  [
    "type" : { mbean -> mbean.name().getKeyProperty("request") },
  ],
  "99thPercentile", otel.&doubleValueCallback)



def network = otel.mbeans(["kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec,topic=*",
                          "kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec,topic=*"])
otel.instrument(network,
  "kafka.network.io",
  "The bytes received or sent by the broker",
  "by",
  [
    "topic" : { mbean -> mbean.name().getKeyProperty("topic") },
    "state" : { mbean -> switch(mbean.name().getKeyProperty("name")) {
      case "BytesInPerSec":
        return "In"
        break
      case "BytesOutPerSec":
        return "Out"
        break
      }
    },
  ],
  "Count", otel.&longCounterCallback)

def purgatorySize = otel.mbean("kafka.server:type=DelayedOperationPurgatory,name=PurgatorySize,delayedOperation=*")
otel.instrument(purgatorySize,
  "kafka.purgatory.size",
  "The number of requests waiting in purgatory",
  "requests",
  [
    "type" : { mbean -> mbean.name().getKeyProperty("delayedOperation") },
  ],
  "Value", otel.&longValueCallback)

def partitionCount = otel.mbean("kafka.server:type=ReplicaManager,name=PartitionCount")
otel.instrument(partitionCount,
  "kafka.partition.count",
  "The number of partitions on the broker",
  "partitions",
  "Value", otel.&longValueCallback)

def partitionOffline = otel.mbean("kafka.controller:type=KafkaController,name=OfflinePartitionsCount")
otel.instrument(partitionOffline,
  "kafka.partition.offline",
  "The number of partitions offline",
  "partitions",
  "Value", otel.&longValueCallback)

def partitionUnderReplicated = otel.mbean("kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions")
otel.instrument(partitionUnderReplicated,
  "kafka.partition.under_replicated",
  "The number of under replicated partitions",
  "partitions",
  "Value", otel.&longValueCallback)

def isrOperations = otel.mbeans(["kafka.server:type=ReplicaManager,name=IsrShrinksPerSec",
                                "kafka.server:type=ReplicaManager,name=IsrExpandsPerSec"])
otel.instrument(isrOperations,
  "kafka.isr.operation.count",
  "The number of in-sync replica shrink and expand operations",
  "operations",
  [
    "operation" : { mbean -> switch(mbean.name().getKeyProperty("name")) {
      case "IsrShrinksPerSec":
        return "Shrink"
        break
      case "IsrExpandsPerSec":
        return "Expand"
        break
      }
    },
  ],
  "Count", otel.&longCounterCallback)


def maxLag = otel.mbean("kafka.server:type=ReplicaFetcherManager,name=MaxLag,clientId=Replica")
otel.instrument(maxLag, "kafka.max.lag", "max lag in messages between follower and leader replicas",
        "messages", "Value", otel.&longValueCallback)

def activeControllerCount = otel.mbean("kafka.controller:type=KafkaController,name=ActiveControllerCount")
otel.instrument(activeControllerCount, "kafka.controller.active.count", "controller is active on broker",
        "controllers", "Value", otel.&longValueCallback)

def leaderElectionRate = otel.mbean("kafka.controller:type=ControllerStats,name=LeaderElectionRateAndTimeMs")
otel.instrument(leaderElectionRate, "kafka.leader.election.rate", "leader election rate - increasing indicates broker failures",
        "elections", "Count", otel.&longCounterCallback)

def uncleanLeaderElections = otel.mbean("kafka.controller:type=ControllerStats,name=UncleanLeaderElectionsPerSec")
otel.instrument(uncleanLeaderElections, "kafka.unclean.election.rate", "unclean leader election rate - increasing indicates broker failures",
        "elections", "Count", otel.&longCounterCallback)

def requestQueueSize = otel.mbean("kafka.network:type=RequestChannel,name=RequestQueueSize")
otel.instrument(requestQueueSize, "kafka.request.queue", "size of the request queue",
        "requests", "Value", otel.&longValueCallback)

def logFlushRate = otel.mbean("kafka.log:type=LogFlushStats,name=LogFlushRateAndTimeMs")
otel.instrument(logFlushRate, "kafka.logs.flush.time.count", "log flush count",
        "ms", "Count", otel.&longCounterCallback)
otel.instrument(logFlushRate, "kafka.logs.flush.time.median", "log flush time - 50th percentile",
        "ms", "50thPercentile", otel.&doubleValueCallback)
otel.instrument(logFlushRate, "kafka.logs.flush.time.99p", "log flush time - 99th percentile",
        "ms", "99thPercentile", otel.&doubleValueCallback)
