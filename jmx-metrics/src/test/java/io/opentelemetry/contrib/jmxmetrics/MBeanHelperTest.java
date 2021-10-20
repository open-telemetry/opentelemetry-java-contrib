/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

class MBeanHelperTest {

  private static final MBeanServer mbeanServer = getPlatformMBeanServer();

  private static final Set<ObjectInstance> registeredBeans = new HashSet<>();

  private static JMXConnectorServer jmxServer;
  private static JmxClient jmxClient;

  @BeforeAll
  static void setUp() throws Exception {
    JMXServiceURL serviceUrl = new JMXServiceURL("rmi", "localhost", 0);
    jmxServer =
        JMXConnectorServerFactory.newJMXConnectorServer(
            serviceUrl, Collections.emptyMap(), mbeanServer);
    jmxServer.start();

    JMXServiceURL completeAddress = jmxServer.getAddress();

    Properties props = new Properties();
    props.setProperty(JmxConfig.SERVICE_URL, completeAddress.toString());

    jmxClient = new JmxClient(new JmxConfig(props));
  }

  @AfterAll
  static void tearDown() throws Exception {
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
  void multiObj() throws Exception {
    String thingName = "io.opentelemetry.contrib.jmxmetrics:type=multiObjThing";

    registerThings(thingName);
    MBeanHelper mBeanHelper =
        new MBeanHelper(jmxClient, Arrays.asList(thingName + ",thing=0", thingName + ",thing=1"));
    mBeanHelper.fetch();

    assertThat(mBeanHelper.getAttribute("SomeAttribute"))
        .hasSameElementsAs(
            IntStream.range(0, 2).mapToObj(Integer::toString).collect(Collectors.toList()));
    assertThat(mBeanHelper.getAttribute("MissingAttribute"))
        .hasSameElementsAs(
            IntStream.range(0, 2).mapToObj(unused -> null).collect(Collectors.toList()));
  }

  @Test
  void single() throws Exception {
    String thingName = "io.opentelemetry.contrib.jmxmetrics:type=singleThing";
    registerThings(thingName);
    MBeanHelper mBeanHelper = new MBeanHelper(jmxClient, thingName + ",*", true);
    mBeanHelper.fetch();

    assertThat(mBeanHelper.getAttribute("SomeAttribute")).containsOnly("0");
    assertThat(mBeanHelper.getAttribute("MissingAttribute")).singleElement().isNull();
  }

  @Test
  void multiple() throws Exception {
    String thingName = "io.opentelemetry.contrib.jmxmetrics:type=multipleThing";
    registerThings(thingName);
    MBeanHelper mBeanHelper = new MBeanHelper(jmxClient, thingName + ",*", false);
    mBeanHelper.fetch();

    assertThat(mBeanHelper.getAttribute("SomeAttribute"))
        .hasSameElementsAs(
            IntStream.range(0, 100).mapToObj(Integer::toString).collect(Collectors.toList()));
    assertThat(mBeanHelper.getAttribute("MissingAttribute"))
        .hasSameElementsAs(
            IntStream.range(0, 100).mapToObj(unused -> null).collect(Collectors.toList()));
  }

  private void registerThings(String thingName) throws Exception {
    for (int i = 0; i < 100; i++) {
      Thing thing = new Thing(Integer.toString(i));
      String name = thingName + ",thing=" + i;
      mbeanServer.registerMBean(thing, new ObjectName(name));
    }
  }

  public interface ThingMBean {

    String getSomeAttribute();
  }

  static class Thing implements ThingMBean {

    private final String attrValue;

    Thing(String attrValue) {
      this.attrValue = attrValue;
    }

    @Override
    public String getSomeAttribute() {
      return attrValue;
    }
  }
}
