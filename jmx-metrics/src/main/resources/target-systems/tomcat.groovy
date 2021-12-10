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

def beantomcatmanager = otel.mbean("Catalina:type=Manager,context=*,host=*")
otel.instrument(beantomcatmanager, "tomcat.sessions", "The number of active sessions", "sessions", "activeSessions", otel.&doubleValueObserver)

def beantomcatrequestProcessor = otel.mbean("Catalina:type=GlobalRequestProcessor,name=*")
otel.instrument(beantomcatrequestProcessor, "tomcat.errors", "The number of errors encountered.", "1",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
  "errorCount", otel.&longSumObserver)
otel.instrument(beantomcatrequestProcessor, "tomcat.processing_time", "The total processing time.", "ms",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
  "processingTime", otel.&longSumObserver)
otel.instrument(beantomcatrequestProcessor, "tomcat.traffic",
  "The number of bytes transmitted and received.", "by",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name")}],
  ["bytesReceived":["direction" : {"recieved"}], "bytesSent": ["direction" : {"sent"}]],
  otel.&longSumObserver)

def beantomcatconnectors = otel.mbean("Catalina:type=ThreadPool,name=*")
otel.instrument(beantomcatconnectors, "tomcat.threads.idle", "description", "1",
  ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
  ["currentThreadCount":["state":"idle"],"currentThreadsBusy":["state":"busy"]], otel.&doubleValueObserver)

