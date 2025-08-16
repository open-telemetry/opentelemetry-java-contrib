/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.broker;

import static com.aliyun.mns.common.MNSConstants.DEFAULT_CHARSET;
import static com.aliyun.mns.common.MNSConstants.X_HEADER_MNS_REQUEST_ID;
import static com.aliyun.mns.common.utils.HttpHeaders.CONTENT_TYPE;
import static io.opentelemetry.contrib.messaging.wrappers.mns.broker.SmqUtils.calculateMessageBodyMD5;
import static io.opentelemetry.contrib.messaging.wrappers.mns.broker.SmqUtils.createErrorMessage;
import static io.opentelemetry.contrib.messaging.wrappers.mns.broker.SmqUtils.generateRandomBase64String;
import static io.opentelemetry.contrib.messaging.wrappers.mns.broker.SmqUtils.generateRandomMessageId;
import static io.opentelemetry.contrib.messaging.wrappers.mns.broker.SmqUtils.generateRandomRequestId;
import static io.opentelemetry.contrib.messaging.wrappers.mns.broker.SmqUtils.inputStreamToByteArray;

import com.aliyun.mns.model.ErrorMessage;
import com.aliyun.mns.model.Message;
import io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser.ErrorMessageSerializer;
import io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser.MessageDeserializer;
import io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser.MessageListDeserializer;
import io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser.MessageListSerializer;
import io.opentelemetry.contrib.messaging.wrappers.mns.broker.ser.MessageSerializer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@SpringBootApplication
@SuppressWarnings({
  "JavaUtilDate",
  "LockNotBeforeTry",
  "SystemOut",
  "PrivateConstructorForUtilityClass"
})
public class SmqMockedBroker {

  public static void main(String[] args) {
    SpringApplication.run(SmqMockedBroker.class, args);
  }

  @Controller
  @Scope("singleton")
  public static class BrokerController {

    Lock queuesLock = new ReentrantLock();

    Map<String, Deque<Message>> queues = new HashMap<>();

    @PostMapping(
        value = "/queues/{queueName}/messages",
        consumes = MediaType.TEXT_XML_VALUE,
        produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<byte[]> serveSendMessage(
        @PathVariable String queueName, @RequestBody byte[] requestBody) throws Exception {
      String requestId = generateRandomRequestId();

      boolean isBatch = true;

      MessageListDeserializer listDeserializer = new MessageListDeserializer();
      List<Message> msgList = listDeserializer.deserialize(new ByteArrayInputStream(requestBody));

      if (msgList == null) {
        isBatch = false;
        MessageDeserializer deserializer = new MessageDeserializer();
        Message msg = deserializer.deserialize(new ByteArrayInputStream(requestBody));
        msgList = Collections.singletonList(msg);
      }

      List<Message> responses = new ArrayList<>(msgList.size());

      for (Message msg : msgList) {
        String mid = generateRandomMessageId();
        msg.setMessageId(mid);
        msg.setEnqueueTime(new Date());
        msg.setPriority(8);
        queuesLock.lock();
        Deque<Message> queue = queues.computeIfAbsent(queueName, (k) -> new ArrayDeque<>());
        queue.offerFirst(msg);
        queuesLock.unlock();

        Message response = new Message();
        response.setMessageId(mid);
        response.setMessageBodyMD5(calculateMessageBodyMD5(msg.getMessageBody()));
        responses.add(response);
      }

      if (isBatch) {
        MessageListSerializer listSerializer = new MessageListSerializer();
        InputStream stream = listSerializer.serialize(responses, DEFAULT_CHARSET);
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(CONTENT_TYPE, "text/xml;charset=UTF-8")
            .header(X_HEADER_MNS_REQUEST_ID, requestId)
            .body(inputStreamToByteArray(stream));
      } else {
        MessageSerializer serializer = new MessageSerializer();
        InputStream stream = serializer.serialize(responses.get(0), DEFAULT_CHARSET);
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(CONTENT_TYPE, "text/xml;charset=UTF-8")
            .header(X_HEADER_MNS_REQUEST_ID, requestId)
            .body(inputStreamToByteArray(stream));
      }
    }

    @PostMapping(
        value = "/topics/{topicName}/messages",
        consumes = MediaType.TEXT_XML_VALUE,
        produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<byte[]> servePublishMessage(
        @PathVariable String topicName, @RequestBody byte[] requestBody) throws Exception {
      return serveSendMessage(topicName.replaceAll("topic", "queue"), requestBody);
    }

    @GetMapping(
        value = "/queues/{queueName}/messages",
        consumes = MediaType.TEXT_XML_VALUE,
        produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<byte[]> serveReceiveMessage(
        @PathVariable String queueName,
        @RequestParam(value = "numOfMessages", required = false, defaultValue = "-1")
            int numOfMessages,
        @RequestParam(value = "waitseconds", defaultValue = "10") int waitSec)
        throws Exception {
      String requestId = generateRandomRequestId();
      long timeout = System.currentTimeMillis() + waitSec * 1000L;

      queuesLock.lock();
      Deque<Message> queue = queues.computeIfAbsent(queueName, (k) -> new ArrayDeque<>());
      queuesLock.unlock();

      List<Message> msgList = null;
      while (System.currentTimeMillis() < timeout) {
        if (!queue.isEmpty()) {
          queuesLock.lock();
          if (!queue.isEmpty()) {
            if (numOfMessages == -1) {
              // receive single message
              Message msg = queue.pollLast();
              msg.setFirstDequeueTime(new Date());
              msg.setNextVisibleTime(new Date(System.currentTimeMillis() + 24 * 3600 * 1000L));
              msg.setDequeueCount((msg.getDequeueCount() == null ? 0 : msg.getDequeueCount()) + 1);
              msg.setReceiptHandle(msg.getPriority() + "-" + generateRandomBase64String(32));
              msgList = Collections.singletonList(msg);
            } else {
              msgList = new ArrayList<>();
              for (int i = 0; i < numOfMessages; i++) {
                if (queue.isEmpty()) {
                  break;
                }
                Message msg = queue.pollLast();
                msg.setFirstDequeueTime(new Date());
                msg.setNextVisibleTime(new Date(System.currentTimeMillis() + 24 * 3600 * 1000L));
                msg.setDequeueCount(
                    (msg.getDequeueCount() == null ? 0 : msg.getDequeueCount()) + 1);
                msg.setReceiptHandle(msg.getPriority() + "-" + generateRandomBase64String(32));
                msgList.add(msg);
              }
            }
            queuesLock.unlock();
            break;
          }
          queuesLock.unlock();
        }

        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          break;
        }
      }

      if (msgList != null) {
        if (numOfMessages == -1) {
          // receive single message
          Message msg = msgList.get(0);
          MessageSerializer serializer = new MessageSerializer();
          return ResponseEntity.status(HttpStatus.OK)
              .header(CONTENT_TYPE, "text/xml;charset=UTF-8")
              .header(X_HEADER_MNS_REQUEST_ID, requestId)
              .body(inputStreamToByteArray(serializer.serialize(msg, DEFAULT_CHARSET)));
        } else {
          MessageListSerializer listSerializer = new MessageListSerializer();
          return ResponseEntity.status(HttpStatus.OK)
              .header(CONTENT_TYPE, "text/xml;charset=UTF-8")
              .header(X_HEADER_MNS_REQUEST_ID, requestId)
              .body(inputStreamToByteArray(listSerializer.serialize(msgList, DEFAULT_CHARSET)));
        }
      } else {
        ErrorMessage errorMessage = createErrorMessage(requestId);
        ErrorMessageSerializer serializer = new ErrorMessageSerializer();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .header(CONTENT_TYPE, "text/xml;charset=UTF-8")
            .header(X_HEADER_MNS_REQUEST_ID, requestId)
            .body(inputStreamToByteArray(serializer.serialize(errorMessage, DEFAULT_CHARSET)));
      }
    }

    @DeleteMapping(value = "/queues/{queueName}/messages", consumes = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<Void> serveDeleteMessage(
        @PathVariable String queueName,
        @RequestParam(value = "ReceiptHandle", defaultValue = "") String receiptHandle,
        @RequestBody(required = false) byte[] requestBody)
        throws Exception {
      String requestId = generateRandomRequestId();
      return ResponseEntity.status(HttpStatus.NO_CONTENT)
          .header(X_HEADER_MNS_REQUEST_ID, requestId)
          .build();
    }
  }
}
