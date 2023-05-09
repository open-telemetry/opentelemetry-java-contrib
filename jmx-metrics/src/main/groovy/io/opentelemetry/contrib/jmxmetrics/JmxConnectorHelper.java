/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import java.io.IOException;
import java.net.URI;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;

public class JmxConnectorHelper {

  private static final Logger logger = Logger.getLogger(JmxConnectorHelper.class.getName());

  private static RMIServer stub = null;
  private static final SslRMIClientSocketFactory sslRMIClientSocketFactory =
      new SslRMIClientSocketFactory();

  private JmxConnectorHelper() {}

  /**
   * To use SSL, the {@link RMIServer} stub used by the {@link RMIConnector} must be built
   * separately. As a result, we have to unwind the {@link JMXConnectorFactory#connect} method and
   * reimplement pieces.
   */
  public static JMXConnector connect(
      JMXServiceURL serviceURL, Map<String, Object> env, boolean registrySsl) throws IOException {

    // Different connection logic is needed when SSL is enabled on the RMI registry
    if (!registrySsl) {
      return JMXConnectorFactory.connect(serviceURL, env);
    }

    logger.log(Level.INFO, "Attempting to connect to an SSL-protected RMI registry");

    String hostName;
    int port;

    if (serviceURL.getURLPath().startsWith("/jndi/")) {
      final String[] components = serviceURL.getURLPath().split("/", 3);
      final URI uri = URI.create(components[2]);
      hostName = uri.getHost();
      port = uri.getPort();
    } else {
      hostName = serviceURL.getHost();
      port = serviceURL.getPort();
    }

    if (stub == null) {
      stub = getStub(hostName, port);
    }
    JMXConnector jmxConn = new RMIConnector(stub, null);
    jmxConn.connect(env);
    return jmxConn;
  }

  private static RMIServer getStub(String hostName, int port) throws IOException {
    try {
      Registry registry = LocateRegistry.getRegistry(hostName, port, sslRMIClientSocketFactory);
      return (RMIServer) registry.lookup("jmxrmi");
    } catch (NotBoundException nbe) {
      throw new IOException(nbe);
    }
  }
}
