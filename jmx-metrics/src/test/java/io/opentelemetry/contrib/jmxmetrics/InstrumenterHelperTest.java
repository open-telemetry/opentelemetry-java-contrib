/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.attributeEntry;
import static io.opentelemetry.sdk.testing.assertj.metrics.MetricAssertions.assertThat;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

import groovy.lang.Closure;
import groovy.util.Eval;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class InstrumenterHelperTest {

  private static final MBeanServer mbeanServer = getPlatformMBeanServer();

  private static final Set<ObjectInstance> registeredBeans = new HashSet<>();

  private static JMXConnectorServer jmxServer;
  private static JmxClient jmxClient;

  // Will eventually be replaced with Jupiter extension in sdk-testing
  private SdkMeterProvider meterProvider;

  private OtelHelper otel;

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
  void setupOtel() {
    meterProvider = SdkMeterProvider.builder().build();

    otel = new OtelHelper(jmxClient, new GroovyMetricEnvironment(meterProvider, "otel.test"));
  }

  @AfterEach
  void unregisterBeans() throws Exception {
    for (ObjectInstance bean : registeredBeans) {
      mbeanServer.unregisterMBean(bean.getObjectName());
    }
    registeredBeans.clear();
  }

  @Nested
  class Single {
    @ParameterizedTest
    @ValueSource(
        strings = {
          "doubleCounter",
          "doubleUpDownCounter",
          "doubleCounterCallback",
          "doubleUpDownCounterCallback"
        })
    void doubleSum(String instrumentMethod) throws Exception {
      String thingName = "single:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "single." + instrumentMethod + ".counter";
      String description = "single double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Double");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleSum()
                      .points()
                      .satisfiesExactly(this::assertDoublePoint));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "longCounter",
          "longUpDownCounter",
          "longCounterCallback",
          "longUpDownCounterCallback"
        })
    void longSum(String instrumentMethod) throws Exception {
      String thingName = "single:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "single." + instrumentMethod + ".counter";
      String description = "single long counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasLongSum()
                      .points()
                      .satisfiesExactly(this::assertLongPoint));
    }

    @ParameterizedTest
    @ValueSource(strings = {"doubleHistogram", "longHistogram"})
    void histogram(String instrumentMethod) throws Exception {
      String thingName = "single:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "single." + instrumentMethod + ".counter";
      String description = "single long counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleHistogram()
                      .points()
                      .satisfiesExactly(
                          point ->
                              assertThat(point)
                                  .hasSum(234)
                                  .hasCount(1)
                                  .attributes()
                                  .containsOnly(
                                      attributeEntry("labelOne", "labelOneValue"),
                                      attributeEntry("labelTwo", "0"))));
    }

    @Test
    void doubleValueCallback() throws Exception {
      String instrumentMethod = "doubleValueCallback";
      String thingName = "single:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "single." + instrumentMethod + ".counter";
      String description = "single double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Double");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleGauge()
                      .points()
                      .satisfiesExactly(this::assertDoublePoint));
    }

    @Test
    void longValueCallback() throws Exception {
      String instrumentMethod = "longValueCallback";
      String thingName = "single:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "single." + instrumentMethod + ".counter";
      String description = "single double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasLongGauge()
                      .points()
                      .satisfiesExactly(this::assertLongPoint));
    }

    private void assertDoublePoint(DoublePointData point) {
      assertThat(point)
          .hasValue(123.456)
          .attributes()
          .containsOnly(
              attributeEntry("labelOne", "labelOneValue"), attributeEntry("labelTwo", "0"));
    }

    private void assertLongPoint(LongPointData point) {
      assertThat(point)
          .hasValue(234)
          .attributes()
          .containsOnly(
              attributeEntry("labelOne", "labelOneValue"), attributeEntry("labelTwo", "0"));
    }
  }

  @Nested
  class Multiple {
    @ParameterizedTest
    @ValueSource(
        strings = {
          "doubleCounter",
          "doubleUpDownCounter",
          "doubleCounterCallback",
          "doubleUpDownCounterCallback"
        })
    void doubleSum(String instrumentMethod) throws Exception {
      String thingName = "multiple:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Double");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleSum()
                      .points()
                      .satisfiesExactlyInAnyOrder(assertDoublePoints()));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "longCounter",
          "longUpDownCounter",
          "longCounterCallback",
          "longUpDownCounterCallback"
        })
    void longSum(String instrumentMethod) throws Exception {
      String thingName = "multiple:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple long counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasLongSum()
                      .points()
                      .satisfiesExactlyInAnyOrder(assertLongPoints()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"doubleHistogram", "longHistogram"})
    void histogram(String instrumentMethod) throws Exception {
      String thingName = "multiple:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple long counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleHistogram()
                      .points()
                      .satisfiesExactlyInAnyOrder(
                          point ->
                              assertThat(point)
                                  .hasSum(234)
                                  .hasCount(1)
                                  .attributes()
                                  .containsOnly(
                                      attributeEntry("labelOne", "labelOneValue"),
                                      attributeEntry("labelTwo", "0")),
                          point ->
                              assertThat(point)
                                  .hasSum(234)
                                  .hasCount(1)
                                  .attributes()
                                  .containsOnly(
                                      attributeEntry("labelOne", "labelOneValue"),
                                      attributeEntry("labelTwo", "1")),
                          point ->
                              assertThat(point)
                                  .hasSum(234)
                                  .hasCount(1)
                                  .attributes()
                                  .containsOnly(
                                      attributeEntry("labelOne", "labelOneValue"),
                                      attributeEntry("labelTwo", "2")),
                          point ->
                              assertThat(point)
                                  .hasSum(234)
                                  .hasCount(1)
                                  .attributes()
                                  .containsOnly(
                                      attributeEntry("labelOne", "labelOneValue"),
                                      attributeEntry("labelTwo", "3"))));
    }

    @Test
    void doubleValueCallback() throws Exception {
      String instrumentMethod = "doubleValueCallback";
      String thingName = "multiple:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Double");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleGauge()
                      .points()
                      .satisfiesExactlyInAnyOrder(assertDoublePoints()));
    }

    @Test
    void longValueCallback() throws Exception {
      String instrumentMethod = "longValueCallback";
      String thingName = "multiple:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(meterProvider.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasLongGauge()
                      .points()
                      .satisfiesExactlyInAnyOrder(assertLongPoints()));
    }

    @SuppressWarnings("unchecked")
    private Consumer<DoublePointData>[] assertDoublePoints() {
      return Stream.<Consumer<DoublePointData>>of(
              point ->
                  assertThat(point)
                      .hasValue(123.456)
                      .attributes()
                      .containsOnly(
                          attributeEntry("labelOne", "labelOneValue"),
                          attributeEntry("labelTwo", "0")),
              point ->
                  assertThat(point)
                      .hasValue(123.456)
                      .attributes()
                      .containsOnly(
                          attributeEntry("labelOne", "labelOneValue"),
                          attributeEntry("labelTwo", "1")),
              point ->
                  assertThat(point)
                      .hasValue(123.456)
                      .attributes()
                      .containsOnly(
                          attributeEntry("labelOne", "labelOneValue"),
                          attributeEntry("labelTwo", "2")),
              point ->
                  assertThat(point)
                      .hasValue(123.456)
                      .attributes()
                      .containsOnly(
                          attributeEntry("labelOne", "labelOneValue"),
                          attributeEntry("labelTwo", "3")))
          .toArray(Consumer[]::new);
    }

    @SuppressWarnings("unchecked")
    private Consumer<LongPointData>[] assertLongPoints() {
      return Stream.<Consumer<LongPointData>>of(
              point ->
                  assertThat(point)
                      .hasValue(234)
                      .attributes()
                      .containsOnly(
                          attributeEntry("labelOne", "labelOneValue"),
                          attributeEntry("labelTwo", "0")),
              point ->
                  assertThat(point)
                      .hasValue(234)
                      .attributes()
                      .containsOnly(
                          attributeEntry("labelOne", "labelOneValue"),
                          attributeEntry("labelTwo", "1")),
              point ->
                  assertThat(point)
                      .hasValue(234)
                      .attributes()
                      .containsOnly(
                          attributeEntry("labelOne", "labelOneValue"),
                          attributeEntry("labelTwo", "2")),
              point ->
                  assertThat(point)
                      .hasValue(234)
                      .attributes()
                      .containsOnly(
                          attributeEntry("labelOne", "labelOneValue"),
                          attributeEntry("labelTwo", "3")))
          .toArray(Consumer[]::new);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void handlesNulls(boolean isSingle) throws Exception {
    String instrumentMethod = "longValueCallback";
    String quantity = isSingle ? "single" : "multiple";
    String thingName = quantity + ":type=" + instrumentMethod + ".Thing";
    MBeanHelper mBeanHelper = registerThings(thingName);

    String instrumentName = quantity + "." + instrumentMethod + ".counter";
    String description = quantity + " double counter description";

    updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Missing");

    assertThat(meterProvider.collectAllMetrics()).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "doubleCounter,false,true",
    "longCounter,false,true",
    "doubleCounterCallback,true,false",
    "longCounterCallback,true,false",
    "doubleUpDownCounter,false,true",
    "longUpDownCounter,false,true",
    "doubleUpDownCounterCallback,true,false",
    "longUpDownCounterCallback,true,false",
    "doubleValueCallback,true,false",
    "longValueCallback,true,false",
    "doubleHistogram,false,false",
    "longHistogram,false,false",
  })
  void correctlyClassified(String instrumentMethod, boolean isObserver, boolean isCounter) {
    Closure<?> instrument = (Closure<?>) Eval.me("otel", otel, "otel.&" + instrumentMethod);
    assertThat(InstrumentHelper.instrumentIsObserver(instrument)).isEqualTo(isObserver);
    assertThat(InstrumentHelper.instrumentIsCounter(instrument)).isEqualTo(isCounter);
  }

  MBeanHelper registerThings(String thingName) throws Exception {
    for (int i = 0; i < 4; i++) {
      Thing thing = new Thing();
      String name = thingName + ",thing=" + i;
      registeredBeans.add(mbeanServer.registerMBean(thing, new ObjectName(name)));
    }

    MBeanHelper mBeanHelper =
        new MBeanHelper(jmxClient, thingName + ",*", thingName.startsWith("single:"));
    mBeanHelper.fetch();
    return mBeanHelper;
  }

  void updateWithHelper(
      MBeanHelper mBeanHelper,
      String instrumentMethod,
      String instrumentName,
      String description,
      String attribute) {
    Closure<?> instrument = (Closure<?>) Eval.me("otel", otel, "otel.&" + instrumentMethod);
    Map<String, Closure> labelFuncs = new HashMap<>();
    labelFuncs.put("labelOne", (Closure<?>) Eval.me("{ unused -> 'labelOneValue' }"));
    labelFuncs.put(
        "labelTwo", (Closure<?>) Eval.me("{ mbean -> mbean.name().getKeyProperty('thing') }"));
    InstrumentHelper instrumentHelper =
        new InstrumentHelper(
            mBeanHelper, instrumentName, description, "1", labelFuncs, attribute, instrument);
    instrumentHelper.update();
  }

  public interface ThingMBean {
    double getDouble();

    long getLong();
  }

  static class Thing implements ThingMBean {
    @Override
    public double getDouble() {
      return 123.456;
    }

    @Override
    public long getLong() {
      return 234;
    }
  }
}
