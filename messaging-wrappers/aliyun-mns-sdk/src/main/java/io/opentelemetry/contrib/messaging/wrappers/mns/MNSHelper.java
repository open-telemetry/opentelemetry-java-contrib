package io.opentelemetry.contrib.messaging.wrappers.mns;

import com.aliyun.mns.model.BaseMessage;
import com.aliyun.mns.model.MessagePropertyValue;
import com.aliyun.mns.model.MessageSystemPropertyName;
import com.aliyun.mns.model.MessageSystemPropertyValue;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MNSProcessRequest;

public final class MNSHelper {

  public static <REQUEST extends MNSProcessRequest> MNSProcessWrapperBuilder<REQUEST> processWrapperBuilder() {
    return new MNSProcessWrapperBuilder<>();
  }

  public static String getMessageHeader(BaseMessage message, String name) {
    MessageSystemPropertyName key = convert2SystemPropertyName(name);
    if (key != null) {
      MessageSystemPropertyValue systemProperty = message.getSystemProperty(key);
      if (systemProperty != null) {
        return systemProperty.getStringValueByType();
      }
    }
    MessagePropertyValue messagePropertyValue = message.getUserProperties().get(name);
    if (messagePropertyValue != null) {
      return messagePropertyValue.getStringValueByType();
    }
    return null;
  }

  /**
   * see {@link MessageSystemPropertyName}
   * */
  public static MessageSystemPropertyName convert2SystemPropertyName(String name) {
    if (name == null) {
      return null;
    } else if (name.equals(MessageSystemPropertyName.TRACE_PARENT.getValue())) {
      return MessageSystemPropertyName.TRACE_PARENT;
    } else if (name.equals(MessageSystemPropertyName.BAGGAGE.getValue())) {
      return MessageSystemPropertyName.BAGGAGE;
    } else if (name.equals(MessageSystemPropertyName.TRACE_STATE.getValue())) {
      return MessageSystemPropertyName.TRACE_STATE;
    }
    return null;
  }

  private MNSHelper() {
  }
}
