/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.metrics.common.InstrumentType;
import io.opentelemetry.sdk.metrics.common.InstrumentValueType;

@AutoValue
abstract class InstrumentDescriptor {

  static InstrumentDescriptor create(
      String name,
      String description,
      String unit,
      InstrumentType instrumentType,
      InstrumentValueType valueType) {
    return new AutoValue_InstrumentDescriptor(name, description, unit, instrumentType, valueType);
  }

  abstract String getName();

  abstract String getDescription();

  abstract String getUnit();

  abstract InstrumentType getInstrumentType();

  abstract InstrumentValueType getValueType();
}
