/*
 * Copyright The OpenTelemetry Authors
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

package io.opentelemetry.contrib.jmxmetrics;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JmxClient {
  private static final Logger logger = Logger.getLogger(JmxClient.class.getName());
  private static final List<ObjectName> EMPTY_LIST = Collections.emptyList();

  private final JMXServiceURL url;
  private final String username;
  private final String password;
  private final String realm;
  private final String remoteProfile;
  @Nullable private JMXConnector jmxConn;

  JmxClient(final JmxConfig config) throws MalformedURLException {
    this.url = new JMXServiceURL(config.serviceUrl);
    this.username = config.username;
    this.password = config.password;
    this.realm = config.realm;
    this.remoteProfile = config.remoteProfile;
  }

  public MBeanServerConnection getConnection() {
    if (jmxConn != null) {
      try {
        return jmxConn.getMBeanServerConnection();
      } catch (IOException e) {
        // Attempt to connect with authentication below.
      }
    }
    try {
      Map<String, Object> env = new HashMap<>();
      if (!JmxConfig.isBlank(username)) {
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
      } catch (final ReflectiveOperationException e) {
        logger.warning("SASL unsupported in current environment: " + e.getMessage());
      }

      jmxConn = JMXConnectorFactory.connect(url, env);
      return jmxConn.getMBeanServerConnection();
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
  public List<ObjectName> query(final ObjectName objectName) {
    MBeanServerConnection mbsc = getConnection();
    if (mbsc == null) {
      return EMPTY_LIST;
    }

    try {
      List<ObjectName> objectNames = new ArrayList<>(mbsc.queryNames(objectName, null));
      Collections.sort(
          objectNames,
          new Comparator<ObjectName>() {
            @Override
            public int compare(ObjectName o1, ObjectName o2) {
              return o1.compareTo(o2);
            }
          });
      return objectNames;
    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not query remote JMX server: ", e);
      return EMPTY_LIST;
    }
  }
}
