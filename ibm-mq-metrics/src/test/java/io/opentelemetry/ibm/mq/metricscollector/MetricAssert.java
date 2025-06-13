/*
 * Copyright Splunk Inc.
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
package io.opentelemetry.ibm.mq.metricscollector;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import org.assertj.core.api.Assertions;

public class MetricAssert {

  private final MetricData metric;
  private final int pointOffset;

  public MetricAssert(MetricData metric, int pointOffset) {
    this.metric = metric;
    this.pointOffset = pointOffset;
  }

  static MetricAssert assertThatMetric(MetricData metric, int pointOffset) {
    return new MetricAssert(metric, pointOffset);
  }

  MetricAssert hasName(String name) {
    Assertions.assertThat(metric.getName()).isEqualTo(name);
    return this;
  }

  MetricAssert hasValue(long value) {
    Assertions.assertThat(
            ((LongPointData) metric.getLongGaugeData().getPoints().toArray()[this.pointOffset])
                .getValue())
        .isEqualTo(value);
    return this;
  }
}
