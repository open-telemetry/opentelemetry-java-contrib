/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser;

import com.aliyun.mns.model.Message;
import java.io.InputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MessageDeserializer extends XMLDeserializer<Message> {

  @Override
  public Message deserialize(InputStream stream) throws Exception {
    Document doc = getDocumentBuilder().parse(stream);

    Element root = doc.getDocumentElement();
    return (Message) parseMessage(root);
  }
}
