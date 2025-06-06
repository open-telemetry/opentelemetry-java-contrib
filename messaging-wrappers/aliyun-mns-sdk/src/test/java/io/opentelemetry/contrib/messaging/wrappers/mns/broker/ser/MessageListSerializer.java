/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser;

import static com.aliyun.mns.common.MNSConstants.DEFAULT_XML_NAMESPACE;
import static com.aliyun.mns.common.MNSConstants.MESSAGE_LIST_TAG;

import com.aliyun.mns.model.Message;
import com.aliyun.mns.model.serialize.XmlUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MessageListSerializer extends XMLSerializer<List<Message>> {

  @Override
  public InputStream serialize(List<Message> msgs, String encoding) throws Exception {
    Document doc = getDocumentBuilder().newDocument();

    Element messages = doc.createElementNS(DEFAULT_XML_NAMESPACE, MESSAGE_LIST_TAG);

    doc.appendChild(messages);

    if (msgs != null) {
      for (Message msg : msgs) {
        Element root = serializeMessage(doc, msg);
        messages.appendChild(root);
      }
    }
    String xml = XmlUtil.xmlNodeToString(doc, encoding);

    return new ByteArrayInputStream(xml.getBytes(encoding));
  }
}
