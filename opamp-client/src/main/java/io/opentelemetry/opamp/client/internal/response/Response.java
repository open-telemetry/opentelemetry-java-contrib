/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.response;

import com.google.auto.value.AutoValue;
import opamp.proto.ServerToAgent;

/**
 * Holds a {@link ServerToAgent} message received from the server.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
@AutoValue
public abstract class Response {
  public abstract ServerToAgent getServerToAgent();

  public static Response create(ServerToAgent serverToAgent) {
    return new AutoValue_Response(serverToAgent);
  }
}
