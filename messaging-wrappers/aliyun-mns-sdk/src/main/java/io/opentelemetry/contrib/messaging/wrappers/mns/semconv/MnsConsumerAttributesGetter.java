/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.semconv;

import io.opentelemetry.contrib.messaging.wrappers.mns.MnsHelper;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public enum MnsConsumerAttributesGetter
    implements MessagingAttributesGetter<MnsProcessRequest, Void> {
  INSTANCE;

  @Override
  public String getSystem(MnsProcessRequest request) {
    return "smq";
  }

  @Nullable
  @Override
  public String getDestination(MnsProcessRequest request) {
    return request.getDestination();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(MnsProcessRequest request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(MnsProcessRequest request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(MnsProcessRequest request) {
    return false;
  }

  @Override
  @Nullable
  public String getConversationId(MnsProcessRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(MnsProcessRequest request) {
    return (long) request.getMessage().getMessageBodyAsBytes().length;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(MnsProcessRequest request) {
    return (long) request.getMessage().getMessageBodyAsRawBytes().length;
  }

  @Override
  @Nullable
  public String getMessageId(MnsProcessRequest request, @Nullable Void unused) {
    return request.getMessage().getMessageId();
  }

  @Nullable
  @Override
  public String getClientId(MnsProcessRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(MnsProcessRequest request, @Nullable Void unused) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(MnsProcessRequest request, String name) {
    String header = MnsHelper.getMessageHeader(request.getMessage(), name);
    if (header == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(header);
  }
}
