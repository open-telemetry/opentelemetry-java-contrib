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

def beanWildflyDeployment = otel.mbeans("jboss.as:deployment=*,subsystem=undertow")
otel.instrument(beanWildflyDeployment, "wildfly.session.count", "The number of sessions created.", "{sessions}",
  ["deployment": { mbean -> mbean.name().getKeyProperty("deployment")}],
  "sessionsCreated", otel.&longCounterCallback)
otel.instrument(beanWildflyDeployment, "wildfly.session.active", "The number of currently active sessions.", "{sessions}",
  ["deployment": { mbean -> mbean.name().getKeyProperty("deployment")}],
  "activeSessions", otel.&longUpDownCounterCallback)
otel.instrument(beanWildflyDeployment, "wildfly.session.expired", "The number of sessions that have expired.", "{sessions}",
  ["deployment": { mbean -> mbean.name().getKeyProperty("deployment")}],
  "expiredSessions", otel.&longCounterCallback)
otel.instrument(beanWildflyDeployment, "wildfly.session.rejected", "The number of sessions that have been rejected.", "{sessions}",
  ["deployment": { mbean -> mbean.name().getKeyProperty("deployment")}],
  "rejectedSessions", otel.&longCounterCallback)

def beanWildflyHttpListener = otel.mbeans("jboss.as:subsystem=undertow,server=*,http-listener=*")
otel.instrument(beanWildflyHttpListener, "wildfly.request.count", "The number of requests received.", "{requests}",
  ["server": { mbean -> mbean.name().getKeyProperty("server")}, "listener": { mbean -> mbean.name().getKeyProperty("http-listener")}],
  "requestCount", otel.&longCounterCallback)
otel.instrument(beanWildflyHttpListener, "wildfly.request.time", "The total amount of time spent on requests.", "ns",
  ["server": { mbean -> mbean.name().getKeyProperty("server")}, "listener": { mbean -> mbean.name().getKeyProperty("http-listener")}],
  "processingTime", otel.&longCounterCallback)
otel.instrument(beanWildflyHttpListener, "wildfly.request.server_error", "The number of requests that have resulted in a 5xx response.", "{requests}",
  ["server": { mbean -> mbean.name().getKeyProperty("server")}, "listener": { mbean -> mbean.name().getKeyProperty("http-listener")}],
  "errorCount", otel.&longCounterCallback)
otel.instrument(beanWildflyHttpListener, "wildfly.network.io", "The number of bytes transmitted.", "by",
  ["server": { mbean -> mbean.name().getKeyProperty("server")}, "listener": { mbean -> mbean.name().getKeyProperty("http-listener")}],
  ["bytesSent":["state":{"out"}], "bytesReceived":["state":{"in"}]],
  otel.&longCounterCallback)

def beanWildflyDataSource = otel.mbeans("jboss.as:subsystem=datasources,data-source=*,statistics=pool")
otel.instrument(beanWildflyDataSource, "wildfly.jdbc.connection.open", "The number of open jdbc connections.", "{connections}",
  ["data_source": { mbean -> mbean.name().getKeyProperty("data-source")}],
  ["ActiveCount":["state":{"active"}], "IdleCount":["state":{"idle"}]],
  otel.&longUpDownCounterCallback)
otel.instrument(beanWildflyDataSource, "wildfly.jdbc.request.wait", "The number of jdbc connections that had to wait before opening.", "{requests}",
  ["data_source": { mbean -> mbean.name().getKeyProperty("data-source")}],
  "WaitCount", otel.&longCounterCallback)

def beanWildflyTransaction = otel.mbean("jboss.as:subsystem=transactions")
otel.instrument(beanWildflyTransaction, "wildfly.jdbc.transaction.count", "The number of transactions created.", "{transactions}",
  "numberOfTransactions", otel.&longCounterCallback)
otel.instrument(beanWildflyTransaction, "wildfly.jdbc.rollback.count", "The number of transactions rolled back.", "{transactions}",
  ["numberOfSystemRollbacks":["cause":{"system"}], "numberOfResourceRollbacks":["cause":{"resource"}], "numberOfApplicationRollbacks":["cause":{"application"}]],
  otel.&longCounterCallback)
