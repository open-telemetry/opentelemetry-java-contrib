/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.semconv;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import java.util.List;
import javax.annotation.Nullable;

public enum DefaultMessagingAttributesGetter
    implements MessagingAttributesGetter<MessagingProcessRequest, Void> {
  INSTANCE;

  @Nullable
  @Override
  public String getDestinationPartitionId(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.getDestinationPartitionId();
  }

  @Override
  public List<String> getMessageHeader(
      MessagingProcessRequest messagingProcessRequest, String name) {
    return messagingProcessRequest.getMessageHeader(name);
  }

  @Nullable
  @Override
  public String getSystem(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.getSystem();
  }

  @Nullable
  @Override
  public String getDestination(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.getDestination();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.getDestinationTemplate();
  }

  @Override
  public boolean isTemporaryDestination(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.isTemporaryDestination();
  }

  @Override
  public boolean isAnonymousDestination(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.isAnonymousDestination();
  }

  @Nullable
  @Override
  public String getConversationId(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.getConversationId();
  }

  @Nullable
  @Override
  public Long getMessageBodySize(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.getMessageBodySize();
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.getMessageEnvelopeSize();
  }

  @Nullable
  @Override
  public String getMessageId(
      MessagingProcessRequest messagingProcessRequest, @Nullable Void unused) {
    return messagingProcessRequest.getMessageId();
  }

  @Nullable
  @Override
  public String getClientId(MessagingProcessRequest messagingProcessRequest) {
    return messagingProcessRequest.getClientId();
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(
      MessagingProcessRequest messagingProcessRequest, @Nullable Void unused) {
    return messagingProcessRequest.getBatchMessageCount();
  }
}
