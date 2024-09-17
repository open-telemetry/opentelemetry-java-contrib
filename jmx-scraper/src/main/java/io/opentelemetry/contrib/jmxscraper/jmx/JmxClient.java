/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.jmx;

import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig;
import io.opentelemetry.contrib.jmxscraper.util.StringUtils;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

@SuppressWarnings("unused") // TODO: Temporary
public class JmxClient {
  private static final Logger logger = Logger.getLogger(JmxClient.class.getName());

  private final JMXServiceURL url;
  private final String username;
  private final String password;
  private final String realm;
  private final String remoteProfile;
  private final boolean registrySsl;
  @Nullable private JMXConnector jmxConn;

  public JmxClient(JmxScraperConfig config) throws MalformedURLException {
    this.url = new JMXServiceURL(config.getServiceUrl());
    this.username = config.getUsername();
    this.password = config.getPassword();
    this.realm = config.getRealm();
    this.remoteProfile = config.getRemoteProfile();
    this.registrySsl = config.isRegistrySsl();
  }

  @Nullable
  public MBeanServerConnection getConnection() {
    if (jmxConn != null) {
      try {
        return jmxConn.getMBeanServerConnection();
      } catch (IOException e) {
        // Attempt to connect with authentication below.
      }
    }
    try {
      @SuppressWarnings("ModifiedButNotUsed") // TODO: Temporary
      Map<String, Object> env = new HashMap<>();
      if (!StringUtils.isBlank(username)) {
        env.put(JMXConnector.CREDENTIALS, new String[] {this.username, this.password});
      }
      try {
        // Not all supported versions of Java contain this Provider
        Class<?> klass = Class.forName("com.sun.security.sasl.Provider");
        Provider provider = (Provider) klass.getDeclaredConstructor().newInstance();
        Security.addProvider(provider);

        env.put("jmx.remote.profile", this.remoteProfile);
        env.put(
            "jmx.remote.sasl.callback.handler",
            new ClientCallbackHandler(this.username, this.password, this.realm));
      } catch (ReflectiveOperationException e) {
        logger.warning("SASL unsupported in current environment: " + e.getMessage());
      }

      //      jmxConn = JmxConnectorHelper.connect(url, env, registrySsl);
      //      return jmxConn.getMBeanServerConnection();
      return jmxConn == null ? null : jmxConn.getMBeanServerConnection(); // Temporary

    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not connect to remote JMX server: ", e);
      return null;
    }
  }

  /**
   * Query the MBean server for a given ObjectName.
   *
   * @param objectName ObjectName to query
   * @return the sorted list of applicable ObjectName instances found by server
   */
  public List<ObjectName> query(ObjectName objectName) {
    MBeanServerConnection mBeanServerConnection = getConnection();
    if (mBeanServerConnection == null) {
      return Collections.emptyList();
    }

    try {
      List<ObjectName> objectNames =
          new ArrayList<>(mBeanServerConnection.queryNames(objectName, null));
      Collections.sort(objectNames);
      return Collections.unmodifiableList(objectNames);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not query remote JMX server: ", e);
      return Collections.emptyList();
    }
  }
}
