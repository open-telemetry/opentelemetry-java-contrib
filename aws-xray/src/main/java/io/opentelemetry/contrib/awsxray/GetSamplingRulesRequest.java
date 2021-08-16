/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.awsxray;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoValue
@JsonSerialize(as = GetSamplingRulesRequest.class)
abstract class GetSamplingRulesRequest {

  static GetSamplingRulesRequest create(@Nullable String nextToken) {
    return new AutoValue_GetSamplingRulesRequest(nextToken);
  }

  @JsonProperty("NextToken")
  @Nullable
  abstract String getNextToken();
}
