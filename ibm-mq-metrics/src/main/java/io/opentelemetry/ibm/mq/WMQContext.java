/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq;

import com.ibm.mq.constants.CMQC;
import io.opentelemetry.ibm.mq.config.QueueManager;
import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes care of websphere mq connection, authentication, SSL, Cipher spec, certificate based
 * authorization.<br>
 * It also validates the arguments passed for various scenarios.
 */
public class WMQContext {
  private static final String TRANSPORT_TYPE_CLIENT = "Client";
  private static final String TRANSPORT_TYPE_BINDINGS = "Bindings";

  public static final Logger logger = LoggerFactory.getLogger(WMQContext.class);
  private final QueueManager queueManager;

  public WMQContext(QueueManager queueManager) {
    this.queueManager = queueManager;
    validateArgs();
  }

  /** Note: This Hashtable type is needed for IBM client classes. */
  @SuppressWarnings("JdkObsolete")
  public Hashtable<String, ?> getMQEnvironment() {
    Hashtable<String, ?> env = new Hashtable<>();
    addEnvProperty(env, CMQC.HOST_NAME_PROPERTY, queueManager.getHost());
    addEnvProperty(env, CMQC.PORT_PROPERTY, queueManager.getPort());
    addEnvProperty(env, CMQC.CHANNEL_PROPERTY, queueManager.getChannelName());
    addEnvProperty(env, CMQC.USER_ID_PROPERTY, queueManager.getUsername());
    addEnvProperty(env, CMQC.PASSWORD_PROPERTY, queueManager.getPassword());
    addEnvProperty(env, CMQC.SSL_CERT_STORE_PROPERTY, queueManager.getSslKeyRepository());
    addEnvProperty(env, CMQC.SSL_CIPHER_SUITE_PROPERTY, queueManager.getCipherSuite());
    // TODO: investigate on CIPHER_SPEC property No Available in MQ 7.5 Jar

    if (TRANSPORT_TYPE_CLIENT.equalsIgnoreCase(queueManager.getTransportType())) {
      addEnvProperty(env, CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES_CLIENT);
    } else if (TRANSPORT_TYPE_BINDINGS.equalsIgnoreCase(queueManager.getTransportType())) {
      addEnvProperty(env, CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES_BINDINGS);
    } else {
      addEnvProperty(env, CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Transport property is {}", env.get(CMQC.TRANSPORT_PROPERTY));
    }
    return env;
  }

  @SuppressWarnings({"unused", "unchecked", "rawtypes"})
  private void addEnvProperty(Hashtable env, String propName, Object propVal) {
    if (null != propVal) {
      if (propVal instanceof String) {
        String propString = (String) propVal;
        if (propString.isEmpty()) {
          return;
        }
      }
      env.put(propName, propVal);
    }
  }

  private void validateArgs() {
    boolean validArgs = true;
    StringBuilder errorMsg = new StringBuilder();
    if (queueManager == null) {
      validArgs = false;
      errorMsg.append("Queue manager cannot be null");
    } else {
      if (TRANSPORT_TYPE_CLIENT.equalsIgnoreCase(queueManager.getTransportType())) {
        if (queueManager.getHost() == null || queueManager.getHost().trim().isEmpty()) {
          validArgs = false;
          errorMsg.append("Host cannot be null or empty for client type connection. ");
        }
        if (queueManager.getPort() == -1) {
          validArgs = false;
          errorMsg.append("port should be set for client type connection. ");
        }
        if (queueManager.getChannelName() == null
            || queueManager.getChannelName().trim().isEmpty()) {
          validArgs = false;
          errorMsg.append("Channel cannot be null or empty for client type connection. ");
        }
      }
      if (TRANSPORT_TYPE_BINDINGS.equalsIgnoreCase(queueManager.getTransportType())) {
        if (queueManager.getName() == null || queueManager.getName().trim().isEmpty()) {
          validArgs = false;
          errorMsg.append("queuemanager cannot be null or empty for bindings type connection. ");
        }
      }
    }

    if (!validArgs) {
      throw new IllegalArgumentException(errorMsg.toString());
    }
  }
}
