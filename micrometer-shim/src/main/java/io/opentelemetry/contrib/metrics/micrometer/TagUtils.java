/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.api.common.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class TagUtils {
  public static Tags attributesToTags(Attributes attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return Tags.empty();
    }

    List<Tag> list = new ArrayList<>(attributes.size());
    attributes.forEach((key, value) -> list.add(Tag.of(key.getKey(), Objects.toString(value))));
    return Tags.of(list);
  }
}
