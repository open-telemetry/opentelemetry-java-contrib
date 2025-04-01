package io.opentelemetry.contrib.messaging.wrappers.mns.semconv;

import com.aliyun.mns.model.Message;
import io.opentelemetry.contrib.messaging.wrappers.mns.MNSHelper;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;

import java.util.Collections;
import java.util.List;

public class MNSProcessRequest implements MessagingProcessRequest {

  private final Message message;

  private final String destination;

  public static MNSProcessRequest of(Message message) {
    return of(message, null);
  }

  public static MNSProcessRequest of(Message message, String destination) {
    return new MNSProcessRequest(message, destination);
  }

  @Override
  public String getSystem() {
    return "mns";
  }

  @Override
  public String getDestination() {
    return this.destination;
  }

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

  @Override
  public String getConversationId() {
    return null;
  }

  @Override
  public Long getMessageBodySize() {
    if (message == null) {
      return null;
    }
    return (long) message.getMessageBodyAsBytes().length;
  }

  @Override
  public Long getMessageEnvelopeSize() {
    return null;
  }

  @Override
  public String getMessageId() {
    if (message == null) {
      return null;
    }
    return message.getMessageId();
  }

  @Override
  public List<String> getMessageHeader(String name) {
    if (message == null) {
      return Collections.emptyList();
    }
    String header = MNSHelper.getMessageHeader(message, name);
    if (header == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(header);
  }

  public Message getMessage() {
    return message;
  }

  private MNSProcessRequest(Message message, String destination) {
    this.message = message;
    this.destination = destination;
  }
}
