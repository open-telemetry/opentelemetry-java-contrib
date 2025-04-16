/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.impl;

import io.opentelemetry.contrib.messaging.wrappers.model.Message;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class MessageRequest implements MessagingProcessRequest {

  private final Message message;

  @Nullable private final String clientId;

  @Nullable private final String eventBusName;

  public static MessageRequest of(Message message) {
    return of(message, null, null);
  }

  public static MessageRequest of(
      Message message, @Nullable String clientId, @Nullable String eventBusName) {
    return new MessageRequest(message, clientId, eventBusName);
  }

  @Override
  public String getSystem() {
    return "guava-eventbus";
  }

  @Nullable
  @Override
  public String getDestination() {
    return eventBusName;
  }

  @Nullable
  @Override
  public String getDestinationTemplate() {
    return null;
  }

  @Override
  public boolean isTemporaryDestination() {
    return false;
  }

  @Override
  public boolean isAnonymousDestination() {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId() {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize() {
    return (long) message.getBody().getBytes(StandardCharsets.UTF_8).length;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize() {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId() {
    return message.getId();
  }

  @Nullable
  @Override
  public String getClientId() {
    return clientId;
  }

  @Override
  public List<String> getMessageHeader(String name) {
    return Collections.singletonList(message.getHeaders().get(name));
  }

  public Message getMessage() {
    return message;
  }

  private MessageRequest(
      Message message, @Nullable String clientId, @Nullable String eventBusName) {
    this.message = message;
    this.clientId = clientId;
    this.eventBusName = eventBusName;
  }
}
