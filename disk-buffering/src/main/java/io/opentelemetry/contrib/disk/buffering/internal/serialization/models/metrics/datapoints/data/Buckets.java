/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.data;

import com.dslplatform.json.JsonAttribute;
import java.util.ArrayList;
import java.util.List;

public final class Buckets {

  @JsonAttribute(name = "offset")
  public Integer offset = 0;

  @JsonAttribute(name = "bucketCounts")
  public List<String> bucketCounts = new ArrayList<>();
}
