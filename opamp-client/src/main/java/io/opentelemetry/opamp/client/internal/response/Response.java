/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.response;

import com.google.auto.value.AutoValue;
import opamp.proto.ServerToAgent;

@AutoValue
public abstract class Response {
  public abstract ServerToAgent getServerToAgent();

  public static Response create(ServerToAgent serverToAgent) {
    return new AutoValue_Response(serverToAgent);
  }
}
