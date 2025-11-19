/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser;

import com.aliyun.mns.model.Message;
import com.aliyun.mns.model.serialize.XmlUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MessageSerializer extends XMLSerializer<Message> {

  public MessageSerializer() {
    super();
  }

  @Override
  public InputStream serialize(Message msg, String encoding) throws Exception {
    Document doc = getDocumentBuilder().newDocument();

    Element root = serializeMessage(doc, msg);
    doc.appendChild(root);

    String xml = XmlUtil.xmlNodeToString(doc, encoding);

    return new ByteArrayInputStream(xml.getBytes(encoding));
  }
}
