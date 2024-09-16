/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import groovy.jmx.GroovyMBean;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = SECONDS)
class OtelHelperJmxTest {

  private static final String thingName =
      "io.opentelemetry.extensions.metrics.jmx:type=OtelHelperJmxTest.Thing";

  private static final MBeanServer mbeanServer = getPlatformMBeanServer();

  private static final Set<ObjectInstance> registeredBeans = new HashSet<>();

  @SuppressWarnings("NonFinalStaticField") // https://github.com/google/error-prone/issues/4239
  private static JMXConnectorServer jmxServer;

  @BeforeAll
  static void setUp() throws Exception {
    Thing thing = new Thing();
    mbeanServer.registerMBean(thing, new ObjectName(thingName));
  }

  @AfterAll
  static void tearDown() throws Exception {
    mbeanServer.unregisterMBean(new ObjectName(thingName));
  }

  @AfterEach
  void stopServer() throws Exception {
    // each test stands up its own server
    jmxServer.stop();
  }

  @AfterEach
  void unregisterBeans() throws Exception {
    for (ObjectInstance bean : registeredBeans) {
      mbeanServer.unregisterMBean(bean.getObjectName());
    }
    registeredBeans.clear();
  }

  @Test
  void noAuth() throws Exception {
    JMXServiceURL serverAddr = setupServer(Collections.emptyMap());
    Properties props = new Properties();
    props.setProperty(JmxConfig.SERVICE_URL, serverAddr.toString());
    verifyClient(props);
  }

  @Test
  void passwordAuth() throws Exception {
    String pwFile = ClassLoader.getSystemClassLoader().getResource("jmxremote.password").getPath();
    JMXServiceURL serverAddr =
        setupServer(Collections.singletonMap("jmx.remote.x.password.file", pwFile));

    Properties props = new Properties();
    props.setProperty("otel.jmx.service.url", serverAddr.toString());
    props.setProperty(JmxConfig.JMX_USERNAME, "wrongUsername");
    props.setProperty(JmxConfig.JMX_PASSWORD, "wrongPassword");

    assertThatThrownBy(() -> verifyClient(props)).isInstanceOf(SecurityException.class);

    Properties props2 = new Properties();
    props2.setProperty("otel.jmx.service.url", serverAddr.toString());
    props2.setProperty(JmxConfig.JMX_USERNAME, "correctUsername");
    props2.setProperty(JmxConfig.JMX_PASSWORD, "correctPassword");

    assertThatCode(() -> verifyClient(props2)).doesNotThrowAnyException();
  }

  @Test
  void sortedQueryResults() throws Exception {
    JMXServiceURL serverAddr = setupServer(Collections.emptyMap());
    Properties props = new Properties();
    props.setProperty(JmxConfig.SERVICE_URL, serverAddr.toString());
    props.setProperty(JmxConfig.GROOVY_SCRIPT, "myscript.groovy");
    JmxConfig config = new JmxConfig(props);
    config.validate();
    OtelHelper otel = setupHelper(config);

    for (int i = 0; i < 100; i++) {
      Thing thing = new Thing();
      registeredBeans.add(
          mbeanServer.registerMBean(
              thing, new ObjectName("sorted.query.results:type=Thing,thing=" + i)));
    }

    List<GroovyMBean> mbeans = otel.queryJmx("sorted.query.results:type=Thing,*");
    assertThat(mbeans).hasSize(100);

    List<String> names =
        mbeans.stream().map(bean -> bean.name().toString()).collect(Collectors.toList());
    List<String> sortedNames = names.stream().sorted().collect(Collectors.toList());
    assertThat(names).containsExactlyElementsOf(sortedNames);
  }

  private static JMXServiceURL setupServer(Map<String, String> env) throws Exception {
    JMXServiceURL serviceURL = new JMXServiceURL("rmi", "localhost", 0);
    jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, env, mbeanServer);
    jmxServer.start();
    return jmxServer.getAddress();
  }

  private static OtelHelper setupHelper(JmxConfig config) throws Exception {
    return new OtelHelper(new JmxClient(config), new GroovyMetricEnvironment(config), false);
  }

  private static void verifyClient(Properties props) throws Exception {
    props.setProperty(JmxConfig.GROOVY_SCRIPT, "myscript.groovy");
    JmxConfig config = new JmxConfig(props);
    config.validate();
    OtelHelper otel = setupHelper(config);
    List<GroovyMBean> mbeans = otel.queryJmx(thingName);

    assertThat(mbeans)
        .singleElement()
        .satisfies(
            bean ->
                assertThat(bean.getProperty("SomeAttribute")).isEqualTo("This is the attribute"));
  }

  public interface ThingMBean {

    String getSomeAttribute();
  }

  static class Thing implements ThingMBean {

    @Override
    public String getSomeAttribute() {
      return "This is the attribute";
    }
  }
}
