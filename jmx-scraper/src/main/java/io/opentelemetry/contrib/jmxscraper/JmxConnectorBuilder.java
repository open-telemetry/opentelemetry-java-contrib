/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

public class JmxConnectorBuilder {

  private static final Logger logger = Logger.getLogger(JmxConnectorBuilder.class.getName());

  private final JMXServiceURL url;
  @Nullable private String userName;
  @Nullable private String password;
  @Nullable private String profile;
  @Nullable private String realm;
  private boolean sslRegistry;

  private JmxConnectorBuilder(JMXServiceURL url) {
    this.url = url;
  }

  public static JmxConnectorBuilder createNew(String host, int port) {
    return new JmxConnectorBuilder(buildUrl(host, port));
  }

  public static JmxConnectorBuilder createNew(String url) {
    return new JmxConnectorBuilder(buildUrl(url));
  }

  @CanIgnoreReturnValue
  public JmxConnectorBuilder userCredentials(String userName, String password) {
    this.userName = userName;
    this.password = password;
    return this;
  }

  @CanIgnoreReturnValue
  public JmxConnectorBuilder withRemoteProfile(String profile) {
    this.profile = profile;
    return this;
  }

  @CanIgnoreReturnValue
  public JmxConnectorBuilder withRealm(String realm) {
    this.realm = realm;
    return this;
  }

  @CanIgnoreReturnValue
  public JmxConnectorBuilder withSslRegistry() {
    this.sslRegistry = true;
    return this;
  }

  /**
   * Builds JMX connector instance by connecting to the remote JMX endpoint
   *
   * @return JMX connector
   * @throws IOException in case of communication error
   */
  public JMXConnector build() throws IOException {
    Map<String, Object> env = buildEnv();

    try {
      if (sslRegistry) {
        return doConnectSslRegistry(url, env);
      }

      return doConnect(url, env);

    } catch (IOException e) {
      throw new IOException("Unable to connect to " + url.getHost() + ":" + url.getPort(), e);
    }
  }

  private Map<String, Object> buildEnv() {
    Map<String, Object> env = new HashMap<>();
    if (userName != null && password != null) {
      env.put(JMXConnector.CREDENTIALS, new String[] {userName, password});
    }

    if (profile != null) {
      env.put("jmx.remote.profile", profile);
    }

    try {
      // Not all supported versions of Java contain this Provider
      // Also it might not be accessible due to java.security.sasl module not accessible
      Class<?> klass = Class.forName("com.sun.security.sasl.Provider");
      Provider provider = (Provider) klass.getDeclaredConstructor().newInstance();
      Security.addProvider(provider);

      env.put(
          "jmx.remote.sasl.callback.handler",
          (CallbackHandler)
              callbacks -> {
                for (Callback callback : callbacks) {
                  if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(userName);
                  } else if (callback instanceof PasswordCallback) {
                    char[] pwd = password == null ? null : password.toCharArray();
                    ((PasswordCallback) callback).setPassword(pwd);
                  } else if (callback instanceof RealmCallback) {
                    ((RealmCallback) callback).setText(realm);
                  } else {
                    throw new UnsupportedCallbackException(callback);
                  }
                }
              });
    } catch (ReflectiveOperationException e) {
      logger.log(Level.WARNING, "SASL unsupported in current environment: " + e.getMessage());
    }
    return env;
  }

  @SuppressWarnings("BanJNDI")
  private static JMXConnector doConnect(JMXServiceURL url, Map<String, Object> env)
      throws IOException {
    return JMXConnectorFactory.connect(url, env);
  }

  public JMXConnector doConnectSslRegistry(JMXServiceURL url, Map<String, Object> env) {
    throw new IllegalStateException("TODO");
  }

  private static JMXServiceURL buildUrl(String host, int port) {
    return buildUrl(
        String.format(
            Locale.getDefault(), "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", host, port));
  }

  private static JMXServiceURL buildUrl(String url) {
    try {
      return new JMXServiceURL(url);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("invalid url", e);
    }
  }
}
