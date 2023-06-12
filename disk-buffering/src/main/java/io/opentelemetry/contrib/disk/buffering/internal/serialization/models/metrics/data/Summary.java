/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data;

import com.dslplatform.json.CompiledJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.SummaryDataPoint;

@CompiledJson
public final class Summary extends DataJson<SummaryDataPoint> {}
