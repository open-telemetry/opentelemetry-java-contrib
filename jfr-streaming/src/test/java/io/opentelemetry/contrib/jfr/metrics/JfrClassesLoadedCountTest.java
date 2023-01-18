/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.UNIT_CLASSES;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class JfrClassesLoadedCountTest {

  @RegisterExtension JfrExtension jfrExtension = new JfrExtension();

  @Test
  void shouldHaveJfrLoadedClassesCountEvents() throws Exception {
    Thread.sleep(2000);

    jfrExtension.waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.classes.loaded")
                .hasDescription("Number of classes loaded since JVM start")
                .hasUnit(UNIT_CLASSES)
                .hasLongSumSatisfying(
                    sum ->
                        sum.hasPointsSatisfying(
                            point ->
                                point.satisfies(
                                    pointData -> Assertions.assertTrue(pointData.getValue() > 0)))),
        metric ->
            metric
                .hasName("process.runtime.jvm.classes.current_loaded")
                .hasDescription("Number of classes currently loaded")
                .hasUnit(UNIT_CLASSES)
                .hasLongSumSatisfying(
                    sum ->
                        sum.hasPointsSatisfying(
                            point ->
                                point.satisfies(
                                    pointData ->
                                        Assertions.assertTrue(pointData.getValue() >= 0)))),
        metric ->
            metric
                .hasName("process.runtime.jvm.classes.unloaded")
                .hasDescription("Number of classes unloaded since JVM start")
                .hasUnit(UNIT_CLASSES)
                .hasLongSumSatisfying(
                    sum ->
                        sum.hasPointsSatisfying(
                            point ->
                                point.satisfies(
                                    pointData ->
                                        Assertions.assertTrue(pointData.getValue() >= 0)))));
  }
}
