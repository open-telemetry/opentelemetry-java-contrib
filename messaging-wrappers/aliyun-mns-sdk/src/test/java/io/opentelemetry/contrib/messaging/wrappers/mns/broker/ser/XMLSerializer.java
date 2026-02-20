/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser;

import static com.aliyun.mns.common.MNSConstants.DEFAULT_XML_NAMESPACE;
import static com.aliyun.mns.common.MNSConstants.DELAY_SECONDS_TAG;
import static com.aliyun.mns.common.MNSConstants.DEQUEUE_COUNT_TAG;
import static com.aliyun.mns.common.MNSConstants.ENQUEUE_TIME_TAG;
import static com.aliyun.mns.common.MNSConstants.ERROR_CODE_TAG;
import static com.aliyun.mns.common.MNSConstants.ERROR_HOST_ID_TAG;
import static com.aliyun.mns.common.MNSConstants.ERROR_REQUEST_ID_TAG;
import static com.aliyun.mns.common.MNSConstants.ERROR_TAG;
import static com.aliyun.mns.common.MNSConstants.FIRST_DEQUEUE_TIME_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_BODY_MD5_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_BODY_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_ID_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_PROPERTY_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_SYSTEM_PROPERTY_TAG;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_TAG;
import static com.aliyun.mns.common.MNSConstants.NEXT_VISIBLE_TIME_TAG;
import static com.aliyun.mns.common.MNSConstants.PRIORITY_TAG;
import static com.aliyun.mns.common.MNSConstants.PROPERTY_NAME_TAG;
import static com.aliyun.mns.common.MNSConstants.PROPERTY_TYPE_TAG;
import static com.aliyun.mns.common.MNSConstants.PROPERTY_VALUE_TAG;
import static com.aliyun.mns.common.MNSConstants.RECEIPT_HANDLE_TAG;
import static com.aliyun.mns.common.MNSConstants.SYSTEM_PROPERTIES_TAG;
import static com.aliyun.mns.common.MNSConstants.USER_PROPERTIES_TAG;

import com.aliyun.mns.model.AbstractMessagePropertyValue;
import com.aliyun.mns.model.ErrorMessage;
import com.aliyun.mns.model.Message;
import com.aliyun.mns.model.serialize.BaseXMLSerializer;
import com.aliyun.mns.model.serialize.Serializer;
import java.util.Date;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SuppressWarnings("JavaUtilDate")
public abstract class XMLSerializer<T> extends BaseXMLSerializer<T> implements Serializer<T> {

  public Element safeCreateContentElement(
      Document doc, String tagName, Object value, String defaultValue) {
    if (value == null && defaultValue == null) {
      return null;
    }

    Element node = doc.createElement(tagName);
    if (value instanceof Date) {
      node.setTextContent(String.valueOf(((Date) value).getTime()));
    } else if (value != null) {
      node.setTextContent(value.toString());
    } else {
      node.setTextContent(defaultValue);
    }
    return node;
  }

  public Element serializeError(Document doc, ErrorMessage msg) {
    Element root = doc.createElementNS(DEFAULT_XML_NAMESPACE, ERROR_TAG);

    Element node = safeCreateContentElement(doc, ERROR_CODE_TAG, msg.Code, "MessageNotExist");

    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, MESSAGE_TAG, msg.Message, null);

    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, ERROR_REQUEST_ID_TAG, msg.RequestId, null);

    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, ERROR_HOST_ID_TAG, msg.HostId, null);

    if (node != null) {
      root.appendChild(node);
    }

    return root;
  }

  public Element serializeMessage(Document doc, Message msg) {
    Element root = doc.createElementNS(DEFAULT_XML_NAMESPACE, MESSAGE_TAG);

    Element node = safeCreateContentElement(doc, MESSAGE_ID_TAG, msg.getMessageId(), null);

    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, MESSAGE_BODY_TAG, msg.getOriginalMessageBody(), "");

    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, MESSAGE_BODY_MD5_TAG, msg.getMessageBodyMD5(), null);

    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, DELAY_SECONDS_TAG, msg.getDelaySeconds(), null);
    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, PRIORITY_TAG, msg.getPriority(), null);
    if (node != null) {
      root.appendChild(node);
    }

    node =
        safeCreatePropertiesNode(
            doc, msg.getUserProperties(), USER_PROPERTIES_TAG, MESSAGE_PROPERTY_TAG);
    if (node != null) {
      root.appendChild(node);
    }

    node =
        safeCreatePropertiesNode(
            doc, msg.getSystemProperties(), SYSTEM_PROPERTIES_TAG, MESSAGE_SYSTEM_PROPERTY_TAG);
    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, RECEIPT_HANDLE_TAG, msg.getReceiptHandle(), null);
    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, ENQUEUE_TIME_TAG, msg.getEnqueueTime(), null);
    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, NEXT_VISIBLE_TIME_TAG, msg.getNextVisibleTime(), null);
    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, FIRST_DEQUEUE_TIME_TAG, msg.getFirstDequeueTime(), null);
    if (node != null) {
      root.appendChild(node);
    }

    node = safeCreateContentElement(doc, DEQUEUE_COUNT_TAG, msg.getDequeueCount(), null);
    if (node != null) {
      root.appendChild(node);
    }

    return root;
  }

  public Element safeCreatePropertiesNode(
      Document doc,
      Map<String, ? extends AbstractMessagePropertyValue> map,
      String nodeName,
      String propertyNodeName) {
    if (map == null || map.isEmpty()) {
      return null;
    }
    Element propertiesNode = doc.createElement(nodeName);
    for (Map.Entry<String, ? extends AbstractMessagePropertyValue> entry : map.entrySet()) {
      Element propNode = doc.createElement(propertyNodeName);

      Element nameNode = safeCreateContentElement(doc, PROPERTY_NAME_TAG, entry.getKey(), null);
      if (nameNode != null) {
        propNode.appendChild(nameNode);
      }

      Element valueNode =
          safeCreateContentElement(
              doc, PROPERTY_VALUE_TAG, entry.getValue().getStringValueByType(), null);
      if (valueNode != null) {
        propNode.appendChild(valueNode);
      }

      Element typeNode =
          safeCreateContentElement(
              doc, PROPERTY_TYPE_TAG, entry.getValue().getDataTypeString(), null);
      if (typeNode != null) {
        propNode.appendChild(typeNode);
      }

      propertiesNode.appendChild(propNode);
    }
    return propertiesNode;
  }
}
