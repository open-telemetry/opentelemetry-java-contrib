/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.assertions;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Integers;
import org.assertj.core.internal.Iterables;
import org.assertj.core.internal.Maps;
import org.assertj.core.internal.Objects;

public class MetricAssert extends AbstractAssert<MetricAssert, Metric> {

  private static final Objects objects = Objects.instance();
  private static final Iterables iterables = Iterables.instance();
  private static final Integers integers = Integers.instance();
  private static final Maps maps = Maps.instance();

  private boolean descriptionChecked;
  private boolean unitChecked;
  private boolean typeChecked;
  private boolean dataPointAttributesChecked;

  MetricAssert(Metric actual) {
    super(actual, MetricAssert.class);
  }

  public void validateAssertions() {
    info.description("missing assertion on description for metric '%s'", actual.getName());
    objects.assertEqual(info, descriptionChecked, true);

    info.description("missing assertion on unit for metric '%s'", actual.getName());
    objects.assertEqual(info, unitChecked, true);

    info.description("missing assertion on type for metric '%s'", actual.getName());
    objects.assertEqual(info, typeChecked, true);

    info.description("missing assertion on data point attributes for metric '%s", actual.getName());
    objects.assertEqual(info, dataPointAttributesChecked, true);
  }

  /**
   * Verifies metric description
   *
   * @param description expected description
   * @return this
   */
  @CanIgnoreReturnValue
  public MetricAssert hasDescription(String description) {
    isNotNull();

    info.description("unexpected description for metric '%s'", actual.getName());
    objects.assertEqual(info, actual.getDescription(), description);
    descriptionChecked = true;
    return this;
  }

  /**
   * Verifies metric unit
   *
   * @param unit expected unit
   * @return this
   */
  @CanIgnoreReturnValue
  public MetricAssert hasUnit(String unit) {
    isNotNull();

    info.description("unexpected unit for metric '%s'", actual.getName());
    objects.assertEqual(info, actual.getUnit(), unit);
    unitChecked = true;
    return this;
  }

  /**
   * Verifies the metric to be a gauge
   *
   * @return this
   */
  @CanIgnoreReturnValue
  public MetricAssert isGauge() {
    isNotNull();

    info.description("gauge expected for metric '%s'", actual.getName());
    objects.assertEqual(info, actual.hasGauge(), true);
    typeChecked = true;
    return this;
  }

  @CanIgnoreReturnValue
  private MetricAssert hasSum(boolean monotonic) {
    isNotNull();

    info.description("sum expected for metric '%s'", actual.getName());
    objects.assertEqual(info, actual.hasSum(), true);

    String prefix = monotonic ? "monotonic" : "non-monotonic";
    info.description(prefix + " sum expected for metric '%s'", actual.getName());
    objects.assertEqual(info, actual.getSum().getIsMonotonic(), monotonic);
    return this;
  }

  /**
   * Verifies the metric is a counter
   *
   * @return this
   */
  @CanIgnoreReturnValue
  public MetricAssert isCounter() {
    // counters have a monotonic sum as their value can't decrease
    hasSum(true);
    typeChecked = true;
    return this;
  }

  @CanIgnoreReturnValue
  public MetricAssert isUpDownCounter() {
    // up down counters are non-monotonic as their value can increase & decrease
    hasSum(false);
    typeChecked = true;
    return this;
  }

  @CanIgnoreReturnValue
  public MetricAssert hasDataPointsWithoutAttributes() {
    isNotNull();

    return checkDataPoints(
        dataPoints -> {
          dataPointsCommonCheck(dataPoints);

          // all data points must not have any attribute
          for (NumberDataPoint dataPoint : dataPoints) {
            info.description(
                "no attribute expected on data point for metric '%s'", actual.getName());
            iterables.assertEmpty(info, dataPoint.getAttributesList());
          }
        });
  }

  @CanIgnoreReturnValue
  private MetricAssert checkDataPoints(Consumer<List<NumberDataPoint>> listConsumer) {
    // in practice usually one set of data points is provided but the
    // protobuf does not enforce that so we have to ensure checking at least one
    int count = 0;
    if (actual.hasGauge()) {
      count++;
      listConsumer.accept(actual.getGauge().getDataPointsList());
    }
    if (actual.hasSum()) {
      count++;
      listConsumer.accept(actual.getSum().getDataPointsList());
    }
    info.description("at least one set of data points expected for metric '%s'", actual.getName());
    integers.assertGreaterThan(info, count, 0);

    dataPointAttributesChecked = true;
    return this;
  }

  @CanIgnoreReturnValue
  public MetricAssert hasTypedDataPoints(Collection<String> types) {
    // TODO: we could replace this with 'hasDataPointsAttributes'
    return checkDataPoints(
        dataPoints -> {
          dataPointsCommonCheck(dataPoints);

          Set<String> foundValues = new HashSet<>();
          for (NumberDataPoint dataPoint : dataPoints) {
            List<KeyValue> attributes = dataPoint.getAttributesList();

            info.description(
                "expected exactly one 'name' attribute for typed data point in metric '%s'",
                actual.getName());
            iterables.assertHasSize(info, attributes, 1);

            objects.assertEqual(info, attributes.get(0).getKey(), "name");
            foundValues.add(attributes.get(0).getValue().getStringValue());
          }
          info.description(
              "missing or unexpected type attribute for metric '%s'", actual.getName());
          iterables.assertContainsExactlyInAnyOrder(info, foundValues, types.toArray());
        });
  }

  private void dataPointsCommonCheck(List<NumberDataPoint> dataPoints) {
    info.description("unable to retrieve data points from metric '%s'", actual.getName());
    objects.assertNotNull(info, dataPoints);

    // at least one data point must be reported
    info.description("at least one data point expected for metric '%s'", actual.getName());
    iterables.assertNotEmpty(info, dataPoints);
  }

  /**
   * Verifies that all data points have all the expected attributes
   *
   * @param attributes expected attributes
   * @return this
   */
  @SafeVarargs
  @CanIgnoreReturnValue
  public final MetricAssert hasDataPointsAttributes(Map.Entry<String, String>... attributes) {
    return checkDataPoints(
        dataPoints -> {
          dataPointsCommonCheck(dataPoints);

          Map<String, String> attributesMap = new HashMap<>();
          for (Map.Entry<String, String> attributeEntry : attributes) {
            attributesMap.put(attributeEntry.getKey(), attributeEntry.getValue());
          }
          for (NumberDataPoint dataPoint : dataPoints) {
            Map<String, String> dataPointAttributes = toMap(dataPoint.getAttributesList());

            // all attributes must match
            info.description(
                "missing/unexpected data points attributes for metric '%s'", actual.getName());
            containsExactly(dataPointAttributes, attributes);
            maps.assertContainsAllEntriesOf(info, dataPointAttributes, attributesMap);
          }
        });
  }

  /**
   * Verifies that all data points have their attributes match one of the attributes set and that
   * all provided attributes sets matched at least once.
   *
   * @param attributeSets sets of attributes as maps
   * @return this
   */
  @SafeVarargs
  @CanIgnoreReturnValue
  @SuppressWarnings("varargs") // required to avoid warning
  public final MetricAssert hasDataPointsAttributes(Map<String, String>... attributeSets) {
    return checkDataPoints(
        dataPoints -> {
          dataPointsCommonCheck(dataPoints);

          boolean[] matchedSets = new boolean[attributeSets.length];

          // validate each datapoint attributes match exactly one of the provided attributes set
          for (NumberDataPoint dataPoint : dataPoints) {
            Map<String, String> map = toMap(dataPoint.getAttributesList());

            int matchCount = 0;
            for (int i = 0; i < attributeSets.length; i++) {
              if (mapEquals(map, attributeSets[i])) {
                matchedSets[i] = true;
                matchCount++;
              }
            }

            info.description(
                "data point attributes '%s' for metric '%s' must match exactly one of the attribute sets '%s'",
                map, actual.getName(), Arrays.asList(attributeSets));
            integers.assertEqual(info, matchCount, 1);
          }

          // check that all attribute sets matched at least once
          for (int i = 0; i < matchedSets.length; i++) {
            info.description(
                "no data point matched attribute set '%s' for metric '%s'",
                attributeSets[i], actual.getName());
            objects.assertEqual(info, matchedSets[i], true);
          }
        });
  }

  /**
   * map equality utility
   *
   * @param m1 first map
   * @param m2 second map
   * @return true if the maps have exactly the same keys and values
   */
  private static boolean mapEquals(Map<String, String> m1, Map<String, String> m2) {
    if (m1.size() != m2.size()) {
      return false;
    }
    return m1.entrySet().stream().allMatch(e -> e.getValue().equals(m2.get(e.getKey())));
  }

  @SafeVarargs
  @SuppressWarnings("varargs") // required to avoid warning
  private final void containsExactly(
      Map<String, String> map, Map.Entry<String, String>... entries) {
    maps.assertContainsExactly(info, map, entries);
  }

  private static Map<String, String> toMap(List<KeyValue> list) {
    return list.stream()
        .collect(Collectors.toMap(KeyValue::getKey, kv -> kv.getValue().getStringValue()));
  }
}
