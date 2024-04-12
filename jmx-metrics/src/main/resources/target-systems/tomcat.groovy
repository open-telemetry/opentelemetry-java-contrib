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


def beantomcatmanager = otel.mbean("Catalina:type=Manager,host=localhost,context=*")
otel.instrument(beantomcatmanager, "tomcat.sessions", "The number of active sessions.", "sessions", "activeSessions", otel.&doubleValueCallback)

def beantomcatrequestProcessor = otel.mbean("Catalina:type=GlobalRequestProcessor,name=*")
otel.instrument(beantomcatrequestProcessor, "tomcat.errors", "The number of errors encountered.", "errors",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
  "errorCount", otel.&longCounterCallback)
otel.instrument(beantomcatrequestProcessor, "tomcat.request_count", "The total requests.", "requests",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
  "requestCount", otel.&longCounterCallback)
otel.instrument(beantomcatrequestProcessor, "tomcat.max_time", "Maximum time to process a request.", "ms",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
  "maxTime", otel.&longValueCallback)
otel.instrument(beantomcatrequestProcessor, "tomcat.processing_time", "The total processing time.", "ms",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
  "processingTime", otel.&longCounterCallback)
otel.instrument(beantomcatrequestProcessor, "tomcat.traffic",
  "The number of bytes transmitted and received.", "by",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name")}],
  ["bytesReceived":["direction" : {"received"}], "bytesSent": ["direction" : {"sent"}]],
  otel.&longCounterCallback)

def beantomcatconnectors = otel.mbean("Catalina:type=ThreadPool,name=*")
otel.instrument(beantomcatconnectors, "tomcat.threads", "The number of threads", "threads",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
  ["currentThreadCount":["state":{"idle"}],"currentThreadsBusy":["state":{"busy"}]], otel.&longValueCallback)

def beantomcatnewmanager = otel.mbean("Tomcat:type=Manager,host=localhost,context=*")
otel.instrument(beantomcatnewmanager, "tomcat.sessions", "The number of active sessions.", "sessions", "activeSessions", otel.&doubleValueCallback)

def beantomcatnewrequestProcessor = otel.mbean("Tomcat:type=GlobalRequestProcessor,name=*")
otel.instrument(beantomcatnewrequestProcessor, "tomcat.errors", "The number of errors encountered.", "errors",
    ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
    "errorCount", otel.&longCounterCallback)
otel.instrument(beantomcatnewrequestProcessor, "tomcat.request_count", "The total requests.", "requests",
    ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
    "requestCount", otel.&longCounterCallback)
otel.instrument(beantomcatnewrequestProcessor, "tomcat.max_time", "Maximum time to process a request.", "ms",
    ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
    "maxTime", otel.&longValueCallback)
otel.instrument(beantomcatnewrequestProcessor, "tomcat.processing_time", "The total processing time.", "ms",
    ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
    "processingTime", otel.&longCounterCallback)
otel.instrument(beantomcatnewrequestProcessor, "tomcat.traffic",
    "The number of bytes transmitted and received.", "by",
    ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name")}],
    ["bytesReceived":["direction" : {"received"}], "bytesSent": ["direction" : {"sent"}]],
    otel.&longCounterCallback)

def beantomcatnewconnectors = otel.mbean("Tomcat:type=ThreadPool,name=*")
otel.instrument(beantomcatnewconnectors, "tomcat.threads", "The number of threads", "threads",
    ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
    ["currentThreadCount":["state":{"idle"}],"currentThreadsBusy":["state":{"busy"}]], otel.&longValueCallback)
