/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import groovy.lang.Closure;
import groovy.util.Eval;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = SECONDS)
class MBeanHelperTest {

  // private static final Logger logger = Logger.getLogger(MBeanHelperTest.class.getName());
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

  @BeforeEach
  void confirmServerIsActive() {
    assertThat(jmxServer.isActive()).isTrue();
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

  @Test
  void transform() throws Exception {
    String thingName = "io.opentelemetry.contrib.jmxmetrics:type=transform";
    Thing thing = new Thing("someValue");
    mbeanServer.registerMBean(thing, new ObjectName(thingName));
    Map<String, Closure<?>> map =
        Stream.of(
                new Object[][] {
                  {
                    "SomeAttribute",
                    Eval.me(
                        "{mbean -> mbean.getProperty(\"SomeAttribute\") == 'someValue' ? 'otherValue' : 'someValue'}")
                  },
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (Closure<?>) data[1]));
    MBeanHelper mBeanHelper = new MBeanHelper(jmxClient, thingName + ",*", true, map);
    mBeanHelper.fetch();

    assertThat(mBeanHelper.getAttribute("SomeAttribute"))
        .hasSameElementsAs(Stream.of(new String[] {"otherValue"}).collect(Collectors.toList()));
  }

  @Test
  void transformMultipleAttributes() throws Exception {
    String thingName = "io.opentelemetry.contrib.jmxmetrics:type=transformMultiple";
    Thing thing1 = new Thing("someValue", "anotherValue");
    ObjectName mbeanName = new ObjectName(thingName);
    mbeanServer.registerMBean(thing1, mbeanName);
    Map<String, Closure<?>> map =
        Stream.of(
                new Object[][] {
                  {
                    "SomeAttribute",
                    Eval.me(
                        "{mbean -> mbean.getProperty(\"SomeAttribute\") == 'someValue' ? 'newValue' : 'someValue'}")
                  },
                  {
                    "AnotherAttribute",
                    Eval.me(
                        "{mbean -> mbean.getProperty(\"AnotherAttribute\") == 'anotherValue' ? 'anotherNewValue' : 'anotherValue'}")
                  },
                })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (Closure<?>) data[1]));
    MBeanHelper mBeanHelper = new MBeanHelper(jmxClient, thingName + ",*", true, map);
    mBeanHelper.fetch();

    assertThat(mBeanHelper.getAttribute("SomeAttribute"))
        .hasSameElementsAs(Stream.of(new String[] {"newValue"}).collect(Collectors.toList()));
    assertThat(mBeanHelper.getAttribute("AnotherAttribute"))
        .hasSameElementsAs(
            Stream.of(new String[] {"anotherNewValue"}).collect(Collectors.toList()));
  }

  private static void registerThings(String thingName) throws Exception {
    for (int i = 0; i < 100; i++) {
      Thing thing = new Thing(Integer.toString(i));
      String name = thingName + ",thing=" + i;
      mbeanServer.registerMBean(thing, new ObjectName(name));
    }
  }

  public interface ThingMBean {

    String getSomeAttribute();

    String getAnotherAttribute();
  }

  static class Thing implements ThingMBean {

    private final String attrValue1;

    private final String attrValue2;

    Thing(String attrValue) {
      this.attrValue1 = attrValue;
      this.attrValue2 = "";
    }

    Thing(String attrValue1, String attrValue2) {
      this.attrValue1 = attrValue1;
      this.attrValue2 = attrValue2;
    }

    @Override
    public String getSomeAttribute() {
      return attrValue1;
    }

    @Override
    public String getAnotherAttribute() {
      return attrValue2;
    }
  }
}
