/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Date;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoValue
abstract class GetSamplingTargetsResponse {

  @JsonCreator
  static GetSamplingTargetsResponse create(
      @JsonProperty("LastRuleModification") Date lastRuleModification,
      @JsonProperty("SamplingTargetDocuments") List<SamplingTargetDocument> documents,
      @JsonProperty("UnprocessedStatistics") List<UnprocessedStatistics> unprocessedStatistics) {
    return new AutoValue_GetSamplingTargetsResponse(
        lastRuleModification, documents, unprocessedStatistics);
  }

  abstract Date getLastRuleModification();

  abstract List<SamplingTargetDocument> getDocuments();

  abstract List<UnprocessedStatistics> getUnprocessedStatistics();

  @AutoValue
  abstract static class SamplingTargetDocument {

    @JsonCreator
    static SamplingTargetDocument create(
        @JsonProperty("FixedRate") double fixedRate,
        @JsonProperty("Interval") @Nullable Integer intervalSecs,
        @JsonProperty("ReservoirQuota") @Nullable Integer reservoirQuota,
        @JsonProperty("ReservoirQuotaTTL") @Nullable Date reservoirQuotaTtl,
        @JsonProperty("RuleName") String ruleName) {
      return new AutoValue_GetSamplingTargetsResponse_SamplingTargetDocument(
          fixedRate, intervalSecs, reservoirQuota, reservoirQuotaTtl, ruleName);
    }

    abstract double getFixedRate();

    @Nullable
    abstract Integer getIntervalSecs();

    @Nullable
    abstract Integer getReservoirQuota();

    // Careful that this is a timestamp when the quota expires, not a duration as we'd normally
    // expect for a Time to live.
    @Nullable
    abstract Date getReservoirQuotaTtl();

    abstract String getRuleName();
  }

  @AutoValue
  abstract static class UnprocessedStatistics {

    @JsonCreator
    static UnprocessedStatistics create(
        @JsonProperty("ErrorCode") String errorCode,
        @JsonProperty("Message") String message,
        @JsonProperty("RuleName") String ruleName) {
      return new AutoValue_GetSamplingTargetsResponse_UnprocessedStatistics(
          errorCode, message, ruleName);
    }

    abstract String getErrorCode();

    abstract String getMessage();

    abstract String getRuleName();
  }
}
