/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.integration.tests;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import com.ibm.msg.client.jakarta.jms.JmsConnectionFactory;
import com.ibm.msg.client.jakarta.jms.JmsFactoryFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.util.WmqUtil;
import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.TextMessage;

/**
 * This code was adapted from https://github.com/ibm-messaging/mq-dev-samples/.
 *
 * <p>A minimal and simple application for Point-to-point messaging.
 *
 * <p>Application makes use of fixed literals, any customisations will require re-compilation of
 * this source file. Application assumes that the named queue is empty prior to a run.
 *
 * <p>Notes:
 *
 * <p>API type: Jakarta API (JMS v3.0, simplified domain)
 *
 * <p>Messaging domain: Point-to-point
 *
 * <p>Provider type: IBM MQ
 *
 * <p>Connection mode: Client connection
 *
 * <p>JNDI in use: No
 */
public final class JakartaPutGet {

  private JakartaPutGet(){
  }

  public static void createQueue(QueueManager manager, String name, int maxDepth) {
    MQQueueManager ibmQueueManager = WmqUtil.connectToQueueManager(manager);
    PCFMessageAgent agent = WmqUtil.initPcfMessageAgent(manager, ibmQueueManager);
    PCFMessage request = new PCFMessage(CMQCFC.MQCMD_CREATE_Q);
    request.addParameter(com.ibm.mq.constants.CMQC.MQCA_Q_NAME, name);
    request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);

    request.addParameter(CMQC.MQIA_MAX_Q_DEPTH, maxDepth);
    // these parameters are indicated in percentage of max depth.
    request.addParameter(CMQC.MQIA_Q_DEPTH_HIGH_LIMIT, 75);
    request.addParameter(CMQC.MQIA_Q_DEPTH_LOW_LIMIT, 20);
    request.addParameter(CMQC.MQIA_Q_DEPTH_HIGH_EVENT, CMQCFC.MQEVR_ENABLED);
    request.addParameter(CMQC.MQIA_Q_DEPTH_LOW_EVENT, CMQCFC.MQEVR_ENABLED);
    request.addParameter(CMQC.MQIA_Q_DEPTH_MAX_EVENT, CMQCFC.MQEVR_ENABLED);
    try {
      agent.send(request);
    } catch (PCFException e) {
      if (e.reasonCode == CMQCFC.MQRCCF_OBJECT_ALREADY_EXISTS) {
        return;
      }
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param manager Queue manager configuration
   * @param queueName Queue that the application uses to put and get messages to and from
   * @param numberOfMessages Number of messages to send
   * @param sleepIntervalMs Sleep interval in ms
   */
  public static void runPutGet(
      QueueManager manager, String queueName, int numberOfMessages, int sleepIntervalMs) {

    createQueue(manager, queueName, 100000);
    JMSContext context = null;
    JMSContext senderContext = null;
    try {
      // Create a connection factory
      JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.JAKARTA_WMQ_PROVIDER);
      JmsConnectionFactory cf = ff.createConnectionFactory();

      // Set the properties
      cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, manager.getHost());
      cf.setIntProperty(WMQConstants.WMQ_PORT, manager.getPort());
      cf.setStringProperty(WMQConstants.WMQ_CHANNEL, manager.getChannelName());
      cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, manager.getName());
      cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "JakartaPutGet (Jakarta)");
      cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
      cf.setStringProperty(WMQConstants.USERID, manager.getUsername());
      cf.setStringProperty(WMQConstants.PASSWORD, manager.getPassword());
      // cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");
      // cf.setIntProperty(MQConstants.CERTIFICATE_VALIDATION_POLICY,
      // MQConstants.MQ_CERT_VAL_POLICY_NONE);

      // Create Jakarta objects
      context = cf.createContext();
      Destination destination = context.createQueue("queue:///" + queueName);

      JMSConsumer consumer = context.createConsumer(destination);
      consumer.setMessageListener(message -> {});

      senderContext = cf.createContext();
      Destination senderDestination = senderContext.createQueue("queue:///" + queueName);

      for (int i = 0; i < numberOfMessages; i++) {
        long uniqueNumber = System.currentTimeMillis() % 1000;
        TextMessage message =
            senderContext.createTextMessage("Your lucky number today is " + uniqueNumber);
        message.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 37);
        JMSProducer producer = senderContext.createProducer();
        producer.send(senderDestination, message);

        Thread.sleep(sleepIntervalMs);
      }

    } catch (JMSException | InterruptedException jmsex) {
      throw new RuntimeException(jmsex);
    } finally {
      if (context != null) {
        context.close();
      }
      if (senderContext != null) {
        senderContext.close();
      }
    }
  }

  /**
   * Send a number of messages to the queue.
   *
   * @param manager Queue manager configuration
   * @param queueName Queue that the application uses to put and get messages to and from
   * @param numberOfMessages Number of messages to send
   */
  public static void sendMessages(QueueManager manager, String queueName, int numberOfMessages) {

    createQueue(manager, queueName, 1000);
    JMSContext context = null;
    try {
      // Create a connection factory
      JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.JAKARTA_WMQ_PROVIDER);
      JmsConnectionFactory cf = ff.createConnectionFactory();

      // Set the properties
      cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, manager.getHost());
      cf.setIntProperty(WMQConstants.WMQ_PORT, manager.getPort());
      cf.setStringProperty(WMQConstants.WMQ_CHANNEL, manager.getChannelName());
      cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, manager.getName());
      cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "Message Sender");
      cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
      cf.setStringProperty(WMQConstants.USERID, manager.getUsername());
      cf.setStringProperty(WMQConstants.PASSWORD, manager.getPassword());
      // cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");
      // cf.setIntProperty(MQConstants.CERTIFICATE_VALIDATION_POLICY,
      // MQConstants.MQ_CERT_VAL_POLICY_NONE);

      // Create Jakarta objects
      context = cf.createContext();
      Destination destination = context.createQueue("queue:///" + queueName);

      for (int i = 0; i < numberOfMessages; i++) {
        long uniqueNumber = System.currentTimeMillis() % 1000;
        TextMessage message =
            context.createTextMessage("Your lucky number today is " + uniqueNumber);
        message.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 37);
        JMSProducer producer = context.createProducer();
        producer.send(destination, message);
      }

    } catch (JMSException e) {
      throw new RuntimeException(e);
    } catch (JMSRuntimeException e) {
      if (e.getCause() instanceof MQException) {
        MQException mqe = (MQException) e.getCause();
        if (mqe.getReason() == 2053) { // queue is full
          return;
        }
      }
      throw new RuntimeException(e);
    } finally {
      if (context != null) {
        context.close();
      }
    }
  }

  /**
   * Reads all the messages of the queue.
   *
   * @param manager Queue manager configuration
   * @param queueName Queue that the application uses to put and get messages to and from
   */
  public static void readMessages(QueueManager manager, String queueName) {
    JMSContext context = null;
    try {
      // Create a connection factory
      JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.JAKARTA_WMQ_PROVIDER);
      JmsConnectionFactory cf = ff.createConnectionFactory();

      // Set the properties
      cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, manager.getHost());
      cf.setIntProperty(WMQConstants.WMQ_PORT, manager.getPort());
      cf.setStringProperty(WMQConstants.WMQ_CHANNEL, manager.getChannelName());
      cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, manager.getName());
      cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "Message Receiver");
      cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
      cf.setStringProperty(WMQConstants.USERID, manager.getUsername());
      cf.setStringProperty(WMQConstants.PASSWORD, manager.getPassword());
      // cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");
      // cf.setIntProperty(MQConstants.CERTIFICATE_VALIDATION_POLICY,
      // MQConstants.MQ_CERT_VAL_POLICY_NONE);

      // Create Jakarta objects
      context = cf.createContext();
      Destination destination = context.createQueue("queue:///" + queueName);

      JMSConsumer consumer = context.createConsumer(destination); // autoclosable
      while (consumer.receiveBody(String.class, 100) != null) {}

    } catch (JMSException e) {
      throw new RuntimeException(e);
    } catch (JMSRuntimeException e) {
      if (e.getCause() instanceof MQException) {
        MQException mqe = (MQException) e.getCause();
        if (mqe.getReason() == CMQC.MQRC_NO_MSG_AVAILABLE) { // out of messages, we read them all.
          return;
        }
      }
      throw new RuntimeException(e);
    } finally {
      if (context != null) {
        context.close();
      }
    }
  }

  public static void tryLoginWithBadPassword(QueueManager manager) {

    JMSContext context = null;
    try {
      // Create a connection factory
      JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.JAKARTA_WMQ_PROVIDER);
      JmsConnectionFactory cf = ff.createConnectionFactory();

      // Set the properties
      cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, manager.getHost());
      cf.setIntProperty(WMQConstants.WMQ_PORT, manager.getPort());
      cf.setStringProperty(WMQConstants.WMQ_CHANNEL, manager.getChannelName());
      cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, manager.getName());
      cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "Bad Password");
      cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
      cf.setStringProperty(WMQConstants.USERID, manager.getUsername());
      cf.setStringProperty(WMQConstants.PASSWORD, "badpassword");
      // cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");
      // cf.setIntProperty(MQConstants.CERTIFICATE_VALIDATION_POLICY,
      // MQConstants.MQ_CERT_VAL_POLICY_NONE);

      // Create Jakarta objects
      context = cf.createContext();
    } catch (JMSException e) {
      throw new RuntimeException(e);
    } catch (JMSRuntimeException e) {
      if (e.getCause() instanceof MQException) {
        MQException mqe = (MQException) e.getCause();
        if (mqe.getReason() == 2035) { // bad password
          return;
        }
      }
      throw new RuntimeException(e);
    } finally {
      if (context != null) {
        context.close();
      }
    }
  }
}
