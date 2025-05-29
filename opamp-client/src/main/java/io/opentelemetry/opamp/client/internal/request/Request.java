/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.opentelemetry.opamp.client.internal.request;

import com.google.auto.value.AutoValue;
import opamp.proto.AgentToServer;

/** Wrapper class for "AgentToServer" request body. */
@AutoValue
public abstract class Request {
  public abstract AgentToServer getAgentToServer();

  public static Request create(AgentToServer agentToServer) {
    return new AutoValue_Request(agentToServer);
  }
}
