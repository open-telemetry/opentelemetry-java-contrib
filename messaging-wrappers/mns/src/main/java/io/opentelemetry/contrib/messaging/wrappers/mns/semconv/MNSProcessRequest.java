package io.opentelemetry.contrib.messaging.wrappers.mns.semconv;

import com.aliyun.mns.model.Message;
import io.opentelemetry.contrib.messaging.wrappers.mns.MNSHelper;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class MNSProcessRequest implements MessagingProcessRequest {

  private final Message message;

  @Nullable
  private final String destination;

  public static MNSProcessRequest of(Message message) {
    return of(message, null);
  }

  public static MNSProcessRequest of(Message message, @Nullable String destination) {
    return new MNSProcessRequest(message, destination);
  }

  @Override
  public String getSystem() {
    return "mns";
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
    return null;
  }

  @Nullable
  @Override
  public String getMessageId() {
    return message.getMessageId();
  }

  @Override
  public List<String> getMessageHeader(String name) {
    String header = MNSHelper.getMessageHeader(message, name);
    if (header == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(header);
  }

  public Message getMessage() {
    return message;
  }

  private MNSProcessRequest(Message message, @Nullable String destination) {
    this.message = message;
    this.destination = destination;
  }
}
