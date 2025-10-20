/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.util.Date;
import java.util.List;

@AutoValue
@JsonSerialize(as = GetSamplingTargetsRequest.class)
abstract class GetSamplingTargetsRequest {

  static GetSamplingTargetsRequest create(
      List<SamplingStatisticsDocument> documents,
      List<SamplingBoostStatisticsDocument> boostDocuments) {
    return new AutoValue_GetSamplingTargetsRequest(documents, boostDocuments);
  }

  // Limit of 25 items
  @JsonProperty("SamplingStatisticsDocuments")
  abstract List<SamplingStatisticsDocument> getDocuments();

  // Limit of 25 items
  @JsonProperty("SamplingBoostStatisticsDocuments")
  abstract List<SamplingBoostStatisticsDocument> getBoostDocuments();

  @AutoValue
  @JsonSerialize(as = SamplingStatisticsDocument.class)
  abstract static class SamplingStatisticsDocument {

    static SamplingStatisticsDocument.Builder newBuilder() {
      return new AutoValue_GetSamplingTargetsRequest_SamplingStatisticsDocument.Builder();
    }

    @JsonProperty("BorrowCount")
    abstract long getBorrowCount();

    @JsonProperty("ClientID")
    abstract String getClientId();

    @JsonProperty("RequestCount")
    abstract long getRequestCount();

    @JsonProperty("RuleName")
    abstract String getRuleName();

    @JsonProperty("SampledCount")
    abstract long getSampledCount();

    @JsonProperty("Timestamp")
    abstract Date getTimestamp();

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setBorrowCount(long borrowCount);

      abstract Builder setClientId(String clientId);

      abstract Builder setRequestCount(long requestCount);

      abstract Builder setRuleName(String ruleName);

      abstract Builder setSampledCount(long sampledCount);

      abstract Builder setTimestamp(Date timestamp);

      abstract SamplingStatisticsDocument build();
    }
  }

  @AutoValue
  @JsonSerialize(as = SamplingBoostStatisticsDocument.class)
  abstract static class SamplingBoostStatisticsDocument {

    static SamplingBoostStatisticsDocument.Builder newBuilder() {
      return new AutoValue_GetSamplingTargetsRequest_SamplingBoostStatisticsDocument.Builder();
    }

    @JsonProperty("RuleName")
    abstract String getRuleName();

    @JsonProperty("ServiceName")
    abstract String getServiceName();

    @JsonProperty("Timestamp")
    abstract Date getTimestamp();

    @JsonProperty("AnomalyCount")
    abstract long getAnomalyCount();

    @JsonProperty("TotalCount")
    abstract long getTotalCount();

    @JsonProperty("SampledAnomalyCount")
    abstract long getSampledAnomalyCount();

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setRuleName(String ruleName);

      abstract Builder setServiceName(String serviceName);

      abstract Builder setTimestamp(Date timestamp);

      abstract Builder setAnomalyCount(long anomalyCount);

      abstract Builder setTotalCount(long totalCount);

      abstract Builder setSampledAnomalyCount(long sampledAnomalyCount);

      abstract SamplingBoostStatisticsDocument build();
    }
  }
}
