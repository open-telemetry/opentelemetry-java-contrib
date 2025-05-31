/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.semconv;

import com.aliyun.mns.model.Message;
import io.opentelemetry.contrib.messaging.wrappers.mns.MnsHelper;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class MnsProcessRequest implements MessagingProcessRequest {

  private final Message message;

  @Nullable private final String destination;

  public static MnsProcessRequest of(Message message) {
    return of(message, null);
  }

  public static MnsProcessRequest of(Message message, @Nullable String destination) {
    return new MnsProcessRequest(message, destination);
  }

  @Override
  public String getSystem() {
    return "smq";
  }

  @Nullable
  @Override
  public String getDestination() {
    return this.destination;
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
    return (long) message.getMessageBodyAsBytes().length;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize() {
    return (long) message.getMessageBodyAsRawBytes().length;
  }

  @Nullable
  @Override
  public String getMessageId() {
    return message.getMessageId();
  }

  @Override
  public List<String> getMessageHeader(String name) {
    String header = MnsHelper.getMessageHeader(message, name);
    if (header == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(header);
  }

  public Message getMessage() {
    return message;
  }

  private MnsProcessRequest(Message message, @Nullable String destination) {
    this.message = message;
    this.destination = destination;
  }
}
