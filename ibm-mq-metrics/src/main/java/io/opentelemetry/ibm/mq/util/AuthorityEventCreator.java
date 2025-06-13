/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentelemetry.ibm.mq.util;

import com.ibm.mq.MQException;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;

/**
 * Application that tries to log in to a queue manager to create authority events.
 *
 * <p>You can run it with
 *
 * <pre>
 * <code>java -cp ibm-mq-monitoring-<version>-all.jar:com.ibm.mq.allclient.jar com.splunk.ibm.mq.util.AuthorityEventCreator <host> <port> <queueManagerName> <channelName> <username> <password> </code>
 * </pre>
 */
public class AuthorityEventCreator {

  public static void main(String[] args) {
    String host, port, queueManagerName, channelName, username, password;
    if (args.length < 6) {
      throw new IllegalArgumentException("Need 6 arguments");
    }
    host = args[0];
    port = args[1];
    queueManagerName = args[2];
    channelName = args[3];
    username = args[4];
    password = args[5];

    JMSContext context = null;
    try {
      // Create a connection factory
      JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.JAKARTA_WMQ_PROVIDER);
      JmsConnectionFactory cf = ff.createConnectionFactory();

      // Set the properties
      cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
      cf.setIntProperty(WMQConstants.WMQ_PORT, Integer.parseInt(port));
      cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channelName);
      cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName);
      cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "Bad Password");
      cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
      cf.setStringProperty(WMQConstants.USERID, username);
      cf.setStringProperty(WMQConstants.PASSWORD, password);
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
          System.out.println("Error 2035 returned");
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
