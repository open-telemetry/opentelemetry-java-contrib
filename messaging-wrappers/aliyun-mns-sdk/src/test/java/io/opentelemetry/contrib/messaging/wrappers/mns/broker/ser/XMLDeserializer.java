/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser;

import static com.aliyun.mns.common.MNSConstants.DEQUEUE_COUNT_TAG;
import static com.aliyun.mns.common.MNSConstants.ENQUEUE_TIME_TAG;
import static com.aliyun.mns.common.MNSConstants.FIRST_DEQUEUE_TIME_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_BODY_MD5_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_BODY_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_ERRORCODE_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_ERRORMESSAGE_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_ID_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_PROPERTY_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_SYSTEM_PROPERTY_TAG;
import static com.aliyun.mns.common.MNSConstants.NEXT_VISIBLE_TIME_TAG;
import static com.aliyun.mns.common.MNSConstants.PRIORITY_TAG;
import static com.aliyun.mns.common.MNSConstants.PROPERTY_NAME_TAG;
import static com.aliyun.mns.common.MNSConstants.PROPERTY_TYPE_TAG;
import static com.aliyun.mns.common.MNSConstants.PROPERTY_VALUE_TAG;
import static com.aliyun.mns.common.MNSConstants.RECEIPT_HANDLE_TAG;
import static com.aliyun.mns.common.MNSConstants.SYSTEM_PROPERTIES_TAG;
import static com.aliyun.mns.common.MNSConstants.USER_PROPERTIES_TAG;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.aliyun.mns.model.BaseMessage;
import com.aliyun.mns.model.ErrorMessageResult;
import com.aliyun.mns.model.Message;
import com.aliyun.mns.model.MessagePropertyValue;
import com.aliyun.mns.model.MessageSystemPropertyName;
import com.aliyun.mns.model.MessageSystemPropertyValue;
import com.aliyun.mns.model.PropertyType;
import com.aliyun.mns.model.SystemPropertyType;
import com.aliyun.mns.model.serialize.BaseXMLSerializer;
import com.aliyun.mns.model.serialize.Deserializer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("JavaUtilDate")
public abstract class XMLDeserializer<T> extends BaseXMLSerializer<T> implements Deserializer<T> {

  protected String safeGetElementContent(Element root, String tagName, String defaultValue) {
    NodeList nodes = root.getElementsByTagName(tagName);
    if (nodes != null) {
      Node node = nodes.item(0);
      if (node == null) {
        return defaultValue;
      } else {
        return node.getTextContent();
      }
    }
    return defaultValue;
  }

  protected Element safeGetElement(Element root, String tagName) {
    NodeList nodes = root.getElementsByTagName(tagName);
    if (nodes != null) {
      Node node = nodes.item(0);
      if (node == null) {
        return null;
      } else {
        return (Element) node;
      }
    }
    return null;
  }

  protected List<Element> safeGetElements(Element parent, String tagName) {
    NodeList nodeList = parent.getElementsByTagName(tagName);
    List<Element> elements = new ArrayList<Element>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        elements.add((Element) node);
      }
    }
    return elements;
  }

  protected ErrorMessageResult parseErrorMessageResult(Element root) {
    ErrorMessageResult result = new ErrorMessageResult();
    String errorCode = safeGetElementContent(root, MESSAGE_ERRORCODE_TAG, null);
    result.setErrorCode(errorCode);

    String errorMessage = safeGetElementContent(root, MESSAGE_ERRORMESSAGE_TAG, null);
    result.setErrorMessage(errorMessage);
    return result;
  }

  protected BaseMessage parseMessage(Element root) {
    Message message = new Message();

    String messageId = safeGetElementContent(root, MESSAGE_ID_TAG, null);
    if (messageId != null) {
      message.setMessageId(messageId);
    }

    String messageBody = safeGetElementContent(root, MESSAGE_BODY_TAG, null);
    if (messageBody != null) {
      message.setMessageBody(messageBody, Message.MessageBodyType.RAW_STRING);
    }

    String messageBodyMD5 = safeGetElementContent(root, MESSAGE_BODY_MD5_TAG, null);
    message.setMessageBodyMD5(messageBodyMD5);

    String receiptHandle = safeGetElementContent(root, RECEIPT_HANDLE_TAG, null);
    message.setReceiptHandle(receiptHandle);

    String enqueueTime = safeGetElementContent(root, ENQUEUE_TIME_TAG, null);
    if (enqueueTime != null) {
      message.setEnqueueTime(new Date(Long.parseLong(enqueueTime)));
    }

    String nextVisibleTime = safeGetElementContent(root, NEXT_VISIBLE_TIME_TAG, null);
    if (nextVisibleTime != null) {
      message.setNextVisibleTime(new Date(Long.parseLong(nextVisibleTime)));
    }

    String firstDequeueTime = safeGetElementContent(root, FIRST_DEQUEUE_TIME_TAG, null);
    if (firstDequeueTime != null) {
      message.setFirstDequeueTime(new Date(Long.parseLong(firstDequeueTime)));
    }

    String dequeueCount = safeGetElementContent(root, DEQUEUE_COUNT_TAG, null);
    if (dequeueCount != null) {
      message.setDequeueCount(Integer.parseInt(dequeueCount));
    }

    String priority = safeGetElementContent(root, PRIORITY_TAG, null);
    if (priority != null) {
      message.setPriority(Integer.parseInt(priority));
    }

    // 解析 userProperties
    safeAddPropertiesToMessage(root, message);

    // 解析 systemProperties
    safeAddSystemPropertiesToMessage(root, message);

    return message;
  }

  protected void safeAddPropertiesToMessage(Element root, Message message) {
    Element userPropertiesElement = safeGetElement(root, USER_PROPERTIES_TAG);
    if (userPropertiesElement != null) {
      Map<String, MessagePropertyValue> userProperties = message.getUserProperties();
      if (userProperties == null) {
        userProperties = new HashMap<String, MessagePropertyValue>();
        message.setUserProperties(userProperties);
      }

      for (Element propertyValueElement :
          safeGetElements(userPropertiesElement, MESSAGE_PROPERTY_TAG)) {
        String name = safeGetElementContent(propertyValueElement, PROPERTY_NAME_TAG, null);
        String value = safeGetElementContent(propertyValueElement, PROPERTY_VALUE_TAG, null);
        String type = safeGetElementContent(propertyValueElement, PROPERTY_TYPE_TAG, null);

        if (name != null && value != null && type != null) {
          PropertyType typeEnum = PropertyType.valueOf(type);
          // 如果是二进制类型，需要base64解码
          if (typeEnum == PropertyType.BINARY) {
            byte[] decodedBytes = Base64.decodeBase64(value);
            value = new String(decodedBytes, UTF_8);
          }
          MessagePropertyValue propertyValue =
              new MessagePropertyValue(PropertyType.valueOf(type), value);
          userProperties.put(name, propertyValue);
        }
      }
    }
  }

  protected void safeAddSystemPropertiesToMessage(Element root, Message message) {
    Element systemPropertiesElement = safeGetElement(root, SYSTEM_PROPERTIES_TAG);
    if (systemPropertiesElement != null) {
      for (Element propertyValueElement :
          safeGetElements(systemPropertiesElement, MESSAGE_SYSTEM_PROPERTY_TAG)) {
        String name = safeGetElementContent(propertyValueElement, PROPERTY_NAME_TAG, null);
        String value = safeGetElementContent(propertyValueElement, PROPERTY_VALUE_TAG, null);
        String type = safeGetElementContent(propertyValueElement, PROPERTY_TYPE_TAG, null);

        if (name != null && value != null && type != null) {
          SystemPropertyType systemPropertyType = SystemPropertyType.valueOf(type);
          MessageSystemPropertyValue propertyValue =
              new MessageSystemPropertyValue(systemPropertyType, value);
          MessageSystemPropertyName propertyName = MessageSystemPropertyName.getByValue(name);
          message.putSystemProperty(propertyName, propertyValue);
        }
      }
    }
  }
}
