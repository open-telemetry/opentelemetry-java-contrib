/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
@JsonSerialize(as = AwsXrayAdaptiveSamplingConfig.class)
@JsonDeserialize(builder = AutoValue_AwsXrayAdaptiveSamplingConfig.Builder.class)
public abstract class AwsXrayAdaptiveSamplingConfig {

  @JsonProperty("version")
  public abstract double getVersion();

  @JsonProperty("anomalyConditions")
  @Nullable
  public abstract List<AnomalyConditions> getAnomalyConditions();

  @JsonProperty("anomalyCaptureLimit")
  @Nullable
  public abstract AnomalyCaptureLimit getAnomalyCaptureLimit();

  public static Builder builder() {
    return new AutoValue_AwsXrayAdaptiveSamplingConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("version")
    public abstract Builder setVersion(double value);

    @JsonProperty("anomalyConditions")
    public abstract Builder setAnomalyConditions(List<AnomalyConditions> value);

    @JsonProperty("anomalyCaptureLimit")
    public abstract Builder setAnomalyCaptureLimit(AnomalyCaptureLimit value);

    public abstract AwsXrayAdaptiveSamplingConfig build();
  }

  @AutoValue
  @JsonDeserialize(
      builder = AutoValue_AwsXrayAdaptiveSamplingConfig_AnomalyConditions.Builder.class)
  public abstract static class AnomalyConditions {
    @JsonProperty("errorCodeRegex")
    @Nullable
    public abstract String getErrorCodeRegex();

    @JsonProperty("operations")
    @Nullable
    public abstract List<String> getOperations();

    @JsonProperty("highLatencyMs")
    @Nullable
    public abstract Long getHighLatencyMs();

    @JsonProperty("usage")
    @Nullable
    public abstract UsageType getUsage();

    public static Builder builder() {
      return new AutoValue_AwsXrayAdaptiveSamplingConfig_AnomalyConditions.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      @JsonProperty("errorCodeRegex")
      public abstract Builder setErrorCodeRegex(String value);

      @JsonProperty("operations")
      public abstract Builder setOperations(List<String> value);

      @JsonProperty("highLatencyMs")
      public abstract Builder setHighLatencyMs(Long value);

      @JsonProperty("usage")
      public abstract Builder setUsage(UsageType value);

      public abstract AnomalyConditions build();
    }
  }

  public enum UsageType {
    BOTH("both"),
    SAMPLING_BOOST("sampling-boost"),
    ANOMALY_TRACE_CAPTURE("anomaly-trace-capture"),
    NEITHER("neither"); // Not meant to be used by customers

    private final String value;

    UsageType(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @JsonCreator
    public static UsageType fromValue(String value) {
      for (UsageType type : values()) {
        if (type.value.equals(value)) {
          return type;
        }
      }
      throw new IllegalArgumentException("Invalid usage value: " + value);
    }

    public static boolean isUsedForBoost(UsageType usage) {
      return BOTH.equals(usage) || SAMPLING_BOOST.equals(usage);
    }

    public static boolean isUsedForAnomalyTraceCapture(UsageType usage) {
      return BOTH.equals(usage) || ANOMALY_TRACE_CAPTURE.equals(usage);
    }
  }

  @AutoValue
  @JsonDeserialize(
      builder = AutoValue_AwsXrayAdaptiveSamplingConfig_AnomalyCaptureLimit.Builder.class)
  public abstract static class AnomalyCaptureLimit {
    @JsonProperty("anomalyTracesPerSecond")
    public abstract int getAnomalyTracesPerSecond();

    public static Builder builder() {
      return new AutoValue_AwsXrayAdaptiveSamplingConfig_AnomalyCaptureLimit.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      @JsonProperty("anomalyTracesPerSecond")
      public abstract Builder setAnomalyTracesPerSecond(int value);

      public abstract AnomalyCaptureLimit build();
    }
  }
}
