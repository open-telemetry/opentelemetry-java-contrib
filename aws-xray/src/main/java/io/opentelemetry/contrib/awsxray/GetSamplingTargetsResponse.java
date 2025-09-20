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
import javax.annotation.Nullable;

@AutoValue
abstract class GetSamplingTargetsResponse {

  @JsonCreator
  static GetSamplingTargetsResponse create(
      @JsonProperty("LastRuleModification") Date lastRuleModification,
      @JsonProperty("SamplingTargetDocuments") List<SamplingTargetDocument> documents,
      @JsonProperty("UnprocessedStatistics") List<UnprocessedStatistics> unprocessedStatistics,
      @JsonProperty("UnprocessedBoostStatistics") @Nullable
          List<UnprocessedStatistics> unprocessedBoostStatistics) {
    return new AutoValue_GetSamplingTargetsResponse(
        lastRuleModification, documents, unprocessedStatistics, unprocessedBoostStatistics);
  }

  abstract Date getLastRuleModification();

  abstract List<SamplingTargetDocument> getDocuments();

  abstract List<UnprocessedStatistics> getUnprocessedStatistics();

  @Nullable
  abstract List<UnprocessedStatistics> getUnprocessedBoostStatistics();

  @AutoValue
  abstract static class SamplingTargetDocument {

    @JsonCreator
    static SamplingTargetDocument create(
        @JsonProperty("FixedRate") double fixedRate,
        @JsonProperty("Interval") @Nullable Integer intervalSecs,
        @JsonProperty("ReservoirQuota") @Nullable Integer reservoirQuota,
        @JsonProperty("ReservoirQuotaTTL") @Nullable Date reservoirQuotaTtl,
        @JsonProperty("SamplingBoost") @Nullable SamplingBoost samplingBoost,
        @JsonProperty("RuleName") String ruleName) {
      return new AutoValue_GetSamplingTargetsResponse_SamplingTargetDocument(
          fixedRate, intervalSecs, reservoirQuota, reservoirQuotaTtl, samplingBoost, ruleName);
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

    @Nullable
    abstract SamplingBoost getSamplingBoost();

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

  @AutoValue
  abstract static class SamplingBoost {
    @JsonCreator
    static SamplingBoost create(
        @JsonProperty("BoostRate") double boostRate,
        @JsonProperty("BoostRateTTL") Date boostRateTtl) {
      return new AutoValue_GetSamplingTargetsResponse_SamplingBoost(boostRate, boostRateTtl);
    }

    abstract double getBoostRate();

    abstract Date getBoostRateTtl();
  }
}
