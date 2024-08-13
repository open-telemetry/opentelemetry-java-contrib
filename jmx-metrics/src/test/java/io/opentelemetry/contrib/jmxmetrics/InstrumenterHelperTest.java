/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.concurrent.TimeUnit.SECONDS;

import groovy.lang.Closure;
import groovy.util.Eval;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.assertj.DoublePointAssert;
import io.opentelemetry.sdk.testing.assertj.LongPointAssert;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(value = 10, unit = SECONDS)
class InstrumenterHelperTest {

  private static final MBeanServer mbeanServer = getPlatformMBeanServer();

  private static final Set<ObjectInstance> registeredBeans = new HashSet<>();

  @SuppressWarnings("NonFinalStaticField") // https://github.com/google/error-prone/issues/4239
  private static JMXConnectorServer jmxServer;

  @SuppressWarnings("NonFinalStaticField") // https://github.com/google/error-prone/issues/4239
  private static JmxClient jmxClient;

  // Will eventually be replaced with Jupiter extension in sdk-testing
  private SdkMeterProvider meterProvider;
  private InMemoryMetricReader metricReader;
  private GroovyMetricEnvironment metricEnvironment;

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
  void confirmServerIsActive() {
    assertThat(jmxServer.isActive()).isTrue();
  }

  @BeforeEach
  void setupOtel() {
    metricReader = InMemoryMetricReader.create();
    meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
    metricEnvironment = new GroovyMetricEnvironment(meterProvider, "otel.test");
    otel = new OtelHelper(jmxClient, metricEnvironment, false);
  }

  @AfterEach
  void unregisterBeans() throws Exception {
    for (ObjectInstance bean : registeredBeans) {
      mbeanServer.unregisterMBean(bean.getObjectName());
    }
    registeredBeans.clear();
  }

  @Nested
  @DisplayName("InstrumenterHelperTest - Single Metric")
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

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleSumSatisfying(
                          sum -> sum.hasPointsSatisfying(this::assertDoublePoint)));
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

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasLongSumSatisfying(sum -> sum.hasPointsSatisfying(this::assertLongPoint)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"doubleHistogram", "longHistogram"})
    void histogram(String instrumentMethod) throws Exception {
      String thingName = "single:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "single." + instrumentMethod + ".counter";
      String description = "single long counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasHistogramSatisfying(
                          histogram ->
                              histogram.hasPointsSatisfying(
                                  point ->
                                      point
                                          .hasSum(234)
                                          .hasCount(1)
                                          .hasAttributesSatisfying(
                                              equalTo(
                                                  AttributeKey.stringKey("labelOne"),
                                                  "labelOneValue"),
                                              equalTo(AttributeKey.stringKey("labelTwo"), "0")))));
    }

    @Test
    void doubleValueCallback() throws Exception {
      String instrumentMethod = "doubleValueCallback";
      String thingName = "single:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "single." + instrumentMethod + ".counter";
      String description = "single double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Double");

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleGaugeSatisfying(
                          gauge -> gauge.hasPointsSatisfying(this::assertDoublePoint)));
    }

    @Test
    void longValueCallback() throws Exception {
      String instrumentMethod = "longValueCallback";
      String thingName = "single:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "single." + instrumentMethod + ".counter";
      String description = "single double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasLongGaugeSatisfying(
                          gauge -> gauge.hasPointsSatisfying(this::assertLongPoint)));
    }

    private void assertDoublePoint(DoublePointAssert point) {
      point
          .hasValue(123.456)
          .hasAttributesSatisfying(
              equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
              equalTo(AttributeKey.stringKey("labelTwo"), "0"));
    }

    private void assertLongPoint(LongPointAssert point) {
      point
          .hasValue(234)
          .hasAttributesSatisfying(
              equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
              equalTo(AttributeKey.stringKey("labelTwo"), "0"));
    }
  }

  @Nested
  @DisplayName("InstrumenterHelperTest - Multiple Metrics")
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

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleSumSatisfying(
                          sum -> sum.hasPointsSatisfying(assertDoublePoints())));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "doubleCounter",
          "doubleUpDownCounter",
          "doubleCounterCallback",
          "doubleUpDownCounterCallback"
        })
    void doubleSumMultipleMBeans(String instrumentMethod) throws Exception {
      ArrayList<String> thingNames = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
        thingNames.add("multiple:type=" + instrumentMethod + ".Thing,thing=" + i);
      }
      MBeanHelper mBeanHelper = registerMultipleThings(thingNames);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Double");

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleSumSatisfying(
                          sum -> sum.hasPointsSatisfying(assertDoublePoints())));
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

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasLongSumSatisfying(sum -> sum.hasPointsSatisfying(assertLongPoints())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"doubleHistogram", "longHistogram"})
    void histogram(String instrumentMethod) throws Exception {
      String thingName = "multiple:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple long counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasHistogramSatisfying(
                          histogram ->
                              histogram.hasPointsSatisfying(
                                  point ->
                                      point
                                          .hasSum(234)
                                          .hasCount(1)
                                          .hasAttributesSatisfying(
                                              equalTo(
                                                  AttributeKey.stringKey("labelOne"),
                                                  "labelOneValue"),
                                              equalTo(AttributeKey.stringKey("labelTwo"), "0")),
                                  point ->
                                      point
                                          .hasSum(234)
                                          .hasCount(1)
                                          .hasAttributesSatisfying(
                                              equalTo(
                                                  AttributeKey.stringKey("labelOne"),
                                                  "labelOneValue"),
                                              equalTo(AttributeKey.stringKey("labelTwo"), "1")),
                                  point ->
                                      point
                                          .hasSum(234)
                                          .hasCount(1)
                                          .hasAttributesSatisfying(
                                              equalTo(
                                                  AttributeKey.stringKey("labelOne"),
                                                  "labelOneValue"),
                                              equalTo(AttributeKey.stringKey("labelTwo"), "2")),
                                  point ->
                                      point
                                          .hasSum(234)
                                          .hasCount(1)
                                          .hasAttributesSatisfying(
                                              equalTo(
                                                  AttributeKey.stringKey("labelOne"),
                                                  "labelOneValue"),
                                              equalTo(AttributeKey.stringKey("labelTwo"), "3")))));
    }

    @Test
    void doubleValueCallback() throws Exception {
      String instrumentMethod = "doubleValueCallback";
      String thingName = "multiple:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Double");

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleGaugeSatisfying(
                          gauge -> gauge.hasPointsSatisfying(assertDoublePoints())));
    }

    @Test
    void doubleValueCallbackMBeans() throws Exception {
      String instrumentMethod = "doubleValueCallback";
      String thingName = "multiple:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "multiple." + instrumentMethod + ".gauge";
      String description = "multiple double gauge description";

      updateWithHelper(
          mBeanHelper,
          instrumentMethod,
          instrumentName,
          description,
          "Double",
          new HashMap<>(),
          /* aggregateAcrossMBeans= */ true);

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleGaugeSatisfying(
                          gauge -> gauge.hasPointsSatisfying(assertDoublePoint())));
    }

    @Test
    void doubleValueCallbackListMBeans() throws Exception {
      String instrumentMethod = "doubleValueCallback";
      ArrayList<String> thingNames = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
        thingNames.add("multiple:type=" + instrumentMethod + ".Thing,thing=" + i);
      }
      MBeanHelper mBeanHelper = registerMultipleThings(thingNames);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Double");

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleGaugeSatisfying(
                          gauge -> gauge.hasPointsSatisfying(assertDoublePoints())));
    }

    @Test
    void doubleValueCallbackMultipleAttributes() throws Exception {
      String instrumentMethod = "doubleValueCallback";
      MBeanHelper mBeanHelper =
          registerThingsOnOneObject("multiple:type=" + instrumentMethod + ".Thing,multi=1");

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple double counter description";

      Map<String, Map<String, Closure<?>>> attributes = new HashMap<>();
      attributes.put(
          "FirstAttribute",
          Collections.singletonMap("Thing", (Closure<?>) Eval.me("{ mbean -> 1 }")));
      attributes.put(
          "SecondAttribute",
          Collections.singletonMap("Thing", (Closure<?>) Eval.me("{ mbean -> 2 }")));
      attributes.put(
          "ThirdAttribute",
          Collections.singletonMap("Thing", (Closure<?>) Eval.me("{ mbean -> 3 }")));
      attributes.put(
          "FourthAttribute",
          Collections.singletonMap("Thing", (Closure<?>) Eval.me("{ mbean -> 4 }")));
      attributes.put(
          "nonExsistentAttribute",
          Collections.singletonMap("unused", (Closure<?>) Eval.me("{ mbean -> unused }")));

      updateWithHelperMultiAttribute(
          mBeanHelper, instrumentMethod, instrumentName, description, attributes);

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasDoubleGaugeSatisfying(
                          gauge -> gauge.hasPointsSatisfying(assertAttributeDoublePoints())));
    }

    @Test
    void longValueCallback() throws Exception {
      String instrumentMethod = "longValueCallback";
      String thingName = "multiple:type=" + instrumentMethod + ".Thing";
      MBeanHelper mBeanHelper = registerThings(thingName);

      String instrumentName = "multiple." + instrumentMethod + ".counter";
      String description = "multiple double counter description";

      updateWithHelper(mBeanHelper, instrumentMethod, instrumentName, description, "Long");

      assertThat(metricReader.collectAllMetrics())
          .satisfiesExactly(
              metric ->
                  assertThat(metric)
                      .hasName(instrumentName)
                      .hasDescription(description)
                      .hasUnit("1")
                      .hasLongGaugeSatisfying(
                          gauge -> gauge.hasPointsSatisfying(assertLongPoints())));
    }

    @SuppressWarnings("unchecked")
    private Consumer<DoublePointAssert>[] assertDoublePoint() {
      return Stream.<Consumer<DoublePointAssert>>of(point -> point.hasValue(123.456 * 4))
          .toArray(Consumer[]::new);
    }

    @SuppressWarnings("unchecked")
    private Consumer<DoublePointAssert>[] assertDoublePoints() {
      return Stream.<Consumer<DoublePointAssert>>of(
              point ->
                  point
                      .hasValue(123.456)
                      .hasAttributesSatisfying(
                          equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
                          equalTo(AttributeKey.stringKey("labelTwo"), "0")),
              point ->
                  point
                      .hasValue(123.456)
                      .hasAttributesSatisfying(
                          equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
                          equalTo(AttributeKey.stringKey("labelTwo"), "1")),
              point ->
                  point
                      .hasValue(123.456)
                      .hasAttributesSatisfying(
                          equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
                          equalTo(AttributeKey.stringKey("labelTwo"), "2")),
              point ->
                  point
                      .hasValue(123.456)
                      .hasAttributesSatisfying(
                          equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
                          equalTo(AttributeKey.stringKey("labelTwo"), "3")))
          .toArray(Consumer[]::new);
    }

    @SuppressWarnings("unchecked")
    private Consumer<DoublePointAssert>[] assertAttributeDoublePoints() {
      return Stream.<Consumer<DoublePointAssert>>of(
              point ->
                  point
                      .hasValue(11.0)
                      .hasAttributesSatisfying(equalTo(AttributeKey.stringKey("Thing"), "1")),
              point ->
                  point
                      .hasValue(10.0)
                      .hasAttributesSatisfying(equalTo(AttributeKey.stringKey("Thing"), "2")),
              point ->
                  point
                      .hasValue(9.0)
                      .hasAttributesSatisfying(equalTo(AttributeKey.stringKey("Thing"), "3")),
              point ->
                  point
                      .hasValue(8.0)
                      .hasAttributesSatisfying(equalTo(AttributeKey.stringKey("Thing"), "4")))
          .toArray(Consumer[]::new);
    }

    @SuppressWarnings("unchecked")
    private Consumer<LongPointAssert>[] assertLongPoints() {
      return Stream.<Consumer<LongPointAssert>>of(
              point ->
                  point
                      .hasValue(234)
                      .hasAttributesSatisfying(
                          equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
                          equalTo(AttributeKey.stringKey("labelTwo"), "0")),
              point ->
                  point
                      .hasValue(234)
                      .hasAttributesSatisfying(
                          equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
                          equalTo(AttributeKey.stringKey("labelTwo"), "1")),
              point ->
                  point
                      .hasValue(234)
                      .hasAttributesSatisfying(
                          equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
                          equalTo(AttributeKey.stringKey("labelTwo"), "2")),
              point ->
                  point
                      .hasValue(234)
                      .hasAttributesSatisfying(
                          equalTo(AttributeKey.stringKey("labelOne"), "labelOneValue"),
                          equalTo(AttributeKey.stringKey("labelTwo"), "3")))
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

    assertThat(metricReader.collectAllMetrics()).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "doubleCounter,false,false,true",
    "longCounter,false,false,true",
    "doubleCounterCallback,true,false,false",
    "longCounterCallback,false,true,false",
    "doubleUpDownCounter,false,false,true",
    "longUpDownCounter,false,false,true",
    "doubleUpDownCounterCallback,true,false,false",
    "longUpDownCounterCallback,false,true,false",
    "doubleValueCallback,true,false,false",
    "longValueCallback,false,true,false",
    "doubleHistogram,false,false,false",
    "longHistogram,false,false,false",
  })
  void correctlyClassified(
      String instrumentMethod,
      boolean isDoubleObserver,
      boolean isLongObserver,
      boolean isCounter) {
    Closure<?> instrument = (Closure<?>) Eval.me("otel", otel, "otel.&" + instrumentMethod);
    assertThat(InstrumentHelper.instrumentIsDoubleObserver(instrument)).isEqualTo(isDoubleObserver);
    assertThat(InstrumentHelper.instrumentIsLongObserver(instrument)).isEqualTo(isLongObserver);
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

  MBeanHelper registerThingsOnOneObject(String thingName) throws Exception {
    SystemStatus thing = new SystemStatus();
    registeredBeans.add(mbeanServer.registerMBean(thing, new ObjectName(thingName)));

    MBeanHelper mBeanHelper = new MBeanHelper(jmxClient, thingName, false);
    mBeanHelper.fetch();
    return mBeanHelper;
  }

  MBeanHelper registerMultipleThings(List<String> thingNames) throws Exception {
    for (String thingName : thingNames) {
      Thing thing = new Thing();
      registeredBeans.add(mbeanServer.registerMBean(thing, new ObjectName(thingName)));
    }

    MBeanHelper mBeanHelper = new MBeanHelper(jmxClient, thingNames);
    mBeanHelper.fetch();
    return mBeanHelper;
  }

  void updateWithHelper(
      MBeanHelper mBeanHelper,
      String instrumentMethod,
      String instrumentName,
      String description,
      String attribute) {
    Map<String, Closure<?>> labelFuncs = new HashMap<>();
    labelFuncs.put("labelOne", (Closure<?>) Eval.me("{ unused -> 'labelOneValue' }"));
    labelFuncs.put(
        "labelTwo", (Closure<?>) Eval.me("{ mbean -> mbean.name().getKeyProperty('thing') }"));
    updateWithHelper(
        mBeanHelper,
        instrumentMethod,
        instrumentName,
        description,
        attribute,
        labelFuncs,
        /* aggregateAcrossMBeans= */ false);
  }

  void updateWithHelper(
      MBeanHelper mBeanHelper,
      String instrumentMethod,
      String instrumentName,
      String description,
      String attribute,
      Map<String, Closure<?>> labelFuncs,
      boolean aggregateAcrossMBeans) {
    Closure<?> instrument = (Closure<?>) Eval.me("otel", otel, "otel.&" + instrumentMethod);
    InstrumentHelper instrumentHelper =
        new InstrumentHelper(
            mBeanHelper,
            instrumentName,
            description,
            "1",
            labelFuncs,
            Collections.singletonMap(attribute, null),
            instrument,
            metricEnvironment,
            aggregateAcrossMBeans);
    instrumentHelper.update();
  }

  void updateWithHelperMultiAttribute(
      MBeanHelper mBeanHelper,
      String instrumentMethod,
      String instrumentName,
      String description,
      Map<String, Map<String, Closure<?>>> attributes) {
    Closure<?> instrument = (Closure<?>) Eval.me("otel", otel, "otel.&" + instrumentMethod);
    Map<String, Closure<?>> labelFuncs = new HashMap<>();
    InstrumentHelper instrumentHelper =
        new InstrumentHelper(
            mBeanHelper,
            instrumentName,
            description,
            "1",
            labelFuncs,
            attributes,
            instrument,
            metricEnvironment,
            false);
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

  public interface SystemStatusMBean {
    Double getFirstAttribute();

    Double getSecondAttribute();

    Double getThirdAttribute();

    Double getFourthAttribute();
  }

  public static class SystemStatus implements SystemStatusMBean {
    @Override
    public Double getFirstAttribute() {
      return 11.0;
    }

    @Override
    public Double getSecondAttribute() {
      return 10.0;
    }

    @Override
    public Double getThirdAttribute() {
      return 9.0;
    }

    @Override
    public Double getFourthAttribute() {
      return 8.0;
    }
  }
}
