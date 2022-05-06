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
otel.instrument(messagesInPerSec,
  "kafka.message.count",
  "The number of messages received by the broker",
  "{messages}",
  "Count", otel.&longCounterCallback)

def requests = otel.mbeans(["kafka.server:type=BrokerTopicMetrics,name=TotalProduceRequestsPerSec",
                           "kafka.server:type=BrokerTopicMetrics,name=TotalFetchRequestsPerSec"])
otel.instrument(requests,
  "kafka.request.count",
  "The number of requests received by the broker",
  "{requests}",
  [
    "type" : { mbean -> switch(mbean.name().getKeyProperty("name")) {
      case "TotalProduceRequestsPerSec":
        return "produce"
        break
      case "TotalFetchRequestsPerSec":
        return "fetch"
        break
      }
    },
  ],
  "Count", otel.&longCounterCallback)

def failedRequests = otel.mbeans(["kafka.server:type=BrokerTopicMetrics,name=FailedProduceRequestsPerSec",
                           "kafka.server:type=BrokerTopicMetrics,name=FailedFetchRequestsPerSec"])
otel.instrument(failedRequests,
  "kafka.request.failed",
  "The number of requests to the broker resulting in a failure",
  "{requests}",
  [
    "type" : { mbean -> switch(mbean.name().getKeyProperty("name")) {
      case "FailedProduceRequestsPerSec":
        return "produce"
        break
      case "FailedFetchRequestsPerSec":
        return "fetch"
        break
      }
    },
  ],
  "Count", otel.&longCounterCallback)

def requestTime = otel.mbeans(["kafka.network:type=RequestMetrics,name=TotalTimeMs,request=Produce",
  "kafka.network:type=RequestMetrics,name=TotalTimeMs,request=FetchConsumer",
  "kafka.network:type=RequestMetrics,name=TotalTimeMs,request=FetchFollower"])
otel.instrument(requestTime,
  "kafka.request.time.total",
  "The total time the broker has taken to service requests",
  "ms",
  [
    "type" : { mbean -> mbean.name().getKeyProperty("request").toLowerCase() },
  ],
  "Count", otel.&longCounterCallback)
otel.instrument(requestTime,
  "kafka.request.time.50p",
  "The 50th percentile time the broker has taken to service requests",
  "ms",
  [
    "type" : { mbean -> mbean.name().getKeyProperty("request").toLowerCase() },
  ],
  "50thPercentile", otel.&doubleValueCallback)
otel.instrument(requestTime,
  "kafka.request.time.99p",
  "The 99th percentile time the broker has taken to service requests",
  "ms",
  [
    "type" : { mbean -> mbean.name().getKeyProperty("request").toLowerCase() },
  ],
  "99thPercentile", otel.&doubleValueCallback)



def network = otel.mbeans(["kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec",
                          "kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec"])
otel.instrument(network,
  "kafka.network.io",
  "The bytes received or sent by the broker",
  "by",
  [
    "state" : { mbean -> switch(mbean.name().getKeyProperty("name")) {
      case "BytesInPerSec":
        return "in"
        break
      case "BytesOutPerSec":
        return "out"
        break
      }
    },
  ],
  "Count", otel.&longCounterCallback)

def purgatorySize = otel.mbeans(["kafka.server:type=DelayedOperationPurgatory,name=PurgatorySize,delayedOperation=Produce",
                                 "kafka.server:type=DelayedOperationPurgatory,name=PurgatorySize,delayedOperation=Fetch"])
otel.instrument(purgatorySize,
  "kafka.purgatory.size",
  "The number of requests waiting in the purgatory",
  "{requests}",
  [
    "type" : { mbean -> mbean.name().getKeyProperty("delayedOperation").toLowerCase() },
  ],
  "Value", otel.&longUpDownCounterCallback)

def partitionCount = otel.mbean("kafka.server:type=ReplicaManager,name=PartitionCount")
otel.instrument(partitionCount,
  "kafka.partition.count",
  "The number of partitions in the broker",
  "{partitions}",
  "Value", otel.&longUpDownCounterCallback)

def partitionOffline = otel.mbean("kafka.controller:type=KafkaController,name=OfflinePartitionsCount")
otel.instrument(partitionOffline,
  "kafka.partition.offline",
  "The number of partitions offline",
  "{partitions}",
  "Value", otel.&longUpDownCounterCallback)

def partitionUnderReplicated = otel.mbean("kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions")
otel.instrument(partitionUnderReplicated,
  "kafka.partition.under_replicated",
  "The number of under replicated partitions",
  "{partitions}",
  "Value", otel.&longUpDownCounterCallback)

def isrOperations = otel.mbeans(["kafka.server:type=ReplicaManager,name=IsrShrinksPerSec",
                                "kafka.server:type=ReplicaManager,name=IsrExpandsPerSec"])
otel.instrument(isrOperations,
  "kafka.isr.operation.count",
  "The number of in-sync replica shrink and expand operations",
  "{operations}",
  [
    "operation" : { mbean -> switch(mbean.name().getKeyProperty("name")) {
      case "IsrShrinksPerSec":
        return "shrink"
        break
      case "IsrExpandsPerSec":
        return "expand"
        break
      }
    },
  ],
  "Count", otel.&longCounterCallback)


def maxLag = otel.mbean("kafka.server:type=ReplicaFetcherManager,name=MaxLag,clientId=Replica")
otel.instrument(maxLag, "kafka.max.lag", "max lag in messages between follower and leader replicas",
        "{messages}", "Value", otel.&longUpDownCounterCallback)

def activeControllerCount = otel.mbean("kafka.controller:type=KafkaController,name=ActiveControllerCount")
otel.instrument(activeControllerCount, "kafka.controller.active.count", "The number of active controllers in the broker",
        "{controllers}", "Value", otel.&longUpDownCounterCallback)

def leaderElectionRate = otel.mbean("kafka.controller:type=ControllerStats,name=LeaderElectionRateAndTimeMs")
otel.instrument(leaderElectionRate, "kafka.leader.elections", "Leader election rate (increasing values indicates broker failures)",
        "{elections}", "Count", otel.&longCounterCallback)

def uncleanLeaderElections = otel.mbean("kafka.controller:type=ControllerStats,name=UncleanLeaderElectionsPerSec")
otel.instrument(uncleanLeaderElections, "kafka.leader.unclean-elections", "Unclean leader election rate (increasing values indicates broker failures)",
        "{elections}", "Count", otel.&longCounterCallback)

def requestQueueSize = otel.mbean("kafka.network:type=RequestChannel,name=RequestQueueSize")
otel.instrument(requestQueueSize, "kafka.request.queue", "The number of requests in the request queue",
        "{requests}", "Value", otel.&longUpDownCounterCallback)

def logFlushRate = otel.mbean("kafka.log:type=LogFlushStats,name=LogFlushRateAndTimeMs")
otel.instrument(logFlushRate, "kafka.logs.flush.time.count", "log flush count",
        "ms", "Count", otel.&longCounterCallback)
otel.instrument(logFlushRate, "kafka.logs.flush.time.median", "log flush time - 50th percentile",
        "ms", "50thPercentile", otel.&doubleValueCallback)
otel.instrument(logFlushRate, "kafka.logs.flush.time.99p", "log flush time - 99th percentile",
        "ms", "99thPercentile", otel.&doubleValueCallback)
