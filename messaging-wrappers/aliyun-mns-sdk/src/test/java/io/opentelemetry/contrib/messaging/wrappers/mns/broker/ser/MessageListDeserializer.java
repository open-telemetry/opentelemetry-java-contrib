/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser;

import static com.aliyun.mns.common.MNSConstants.MESSAGE_TAG;

import com.aliyun.mns.model.Message;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MessageListDeserializer extends XMLDeserializer<List<Message>> {
  @Override
  public List<Message> deserialize(InputStream stream) throws Exception {
    Document doc = getDocumentBuilder().parse(stream);
    return deserialize(doc);
  }

  public List<Message> deserialize(Document doc) {
    NodeList list = doc.getElementsByTagName(MESSAGE_TAG);
    if (list != null && list.getLength() > 0) {
      List<Message> results = new ArrayList<Message>();

      for (int i = 0; i < list.getLength(); i++) {
        Message msg = (Message) parseMessage((Element) list.item(i));
        results.add(msg);
      }
      return results;
    }
    return new ArrayList<>();
  }
}
