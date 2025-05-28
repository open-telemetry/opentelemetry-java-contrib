/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser;

import com.aliyun.mns.model.ErrorMessage;
import com.aliyun.mns.model.serialize.XmlUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ErrorMessageSerializer extends XMLSerializer<ErrorMessage> {

  @Override
  public InputStream serialize(ErrorMessage msg, String encoding) throws Exception {
    Document doc = getDocumentBuilder().newDocument();

    Element root = serializeError(doc, msg);
    doc.appendChild(root);

    String xml = XmlUtil.xmlNodeToString(doc, encoding);

    return new ByteArrayInputStream(xml.getBytes(encoding));
  }
}
