/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.spans;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.trace.StatusCode;
import javax.annotation.Nullable;

public final class StatusDataJson {

  @Nullable
  @JsonAttribute(name = "message")
  public String description;

  @JsonAttribute(name = "code")
  public Integer statusCode = StatusCode.UNSET.ordinal();
}
