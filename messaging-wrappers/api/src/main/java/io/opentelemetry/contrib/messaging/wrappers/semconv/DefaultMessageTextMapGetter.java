/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.semconv;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.List;
import javax.annotation.Nullable;

public enum DefaultMessageTextMapGetter implements TextMapGetter<MessagingProcessRequest> {
  INSTANCE;

  @Override
  public Iterable<String> keys(MessagingProcessRequest carrier) {
    return carrier.getAllMessageHeadersKey();
  }

  @Nullable
  @Override
  public String get(@Nullable MessagingProcessRequest carrier, String key) {
    if (carrier != null) {
      List<String> messageHeader = carrier.getMessageHeader(key);
      if (messageHeader != null && !messageHeader.isEmpty()) {
        return messageHeader.get(0);
      }
    }
    return null;
  }
}
