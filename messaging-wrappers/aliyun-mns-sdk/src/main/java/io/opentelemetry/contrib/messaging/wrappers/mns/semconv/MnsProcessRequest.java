/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.semconv;

import com.aliyun.mns.model.Message;
import javax.annotation.Nullable;

public class MnsProcessRequest {

  private final Message message;

  @Nullable private final String destination;

  public static MnsProcessRequest of(Message message) {
    return of(message, null);
  }

  public static MnsProcessRequest of(Message message, @Nullable String destination) {
    return new MnsProcessRequest(message, destination);
  }

  public Message getMessage() {
    return message;
  }

  @Nullable
  public String getDestination() {
    return this.destination;
  }

  private MnsProcessRequest(Message message, @Nullable String destination) {
    this.message = message;
    this.destination = destination;
  }
}
