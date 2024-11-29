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

def beanSelector = otel.mbean("org.eclipse.jetty.io:context=*,type=managedselector,id=*")
otel.instrument(beanSelector, "jetty.select.count", "The number of select calls.", "{operation}","selectCount", otel.&longCounterCallback)

def beanSessions = otel.mbean("org.eclipse.jetty.server.session:context=*,type=sessionhandler,id=*")
otel.instrument(beanSessions, "jetty.session.count", "The number of sessions established in total.", "{session}",
  ["resource" : { mbean -> mbean.name().getKeyProperty("context") }],
  "sessionsCreated", otel.&longCounterCallback)
otel.instrument(beanSessions, "jetty.session.time.total", "The total time sessions have been active.", "s",
  ["resource" : { mbean -> mbean.name().getKeyProperty("context") }],
  "sessionTimeTotal", otel.&longUpDownCounterCallback)
otel.instrument(beanSessions, "jetty.session.time.max", "The maximum amount of time a session has been active.", "s",
  ["resource" : { mbean -> mbean.name().getKeyProperty("context") }],
  "sessionTimeMax", otel.&longValueCallback)

def beanThreads = otel.mbean("org.eclipse.jetty.util.thread:type=queuedthreadpool,id=*")
otel.instrument(beanThreads, "jetty.thread.count", "The current number of threads.", "{thread}",
  [
    "busyThreads":["state" : {"busy"}],
    "idleThreads": ["state" : {"idle"}]
  ], otel.&longValueCallback)
otel.instrument(beanThreads, "jetty.thread.queue.count", "The current number of threads in the queue.", "{thread}","queueSize", otel.&longValueCallback)
