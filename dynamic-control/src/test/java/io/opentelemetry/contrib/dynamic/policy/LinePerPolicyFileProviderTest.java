/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinePerPolicyFileProviderTest {

  private static final String TRACE_SAMPLING_TYPE = TraceSamplingRatePolicy.POLICY_TYPE;

  @TempDir Path tempDir;

  @Test
  void fetchPoliciesReturnsEmptyWhenFileMissing() throws Exception {
    Path missingFile = tempDir.resolve("missing-policies.txt");
    LinePerPolicyFileProvider provider =
        new LinePerPolicyFileProvider(missingFile, Collections.singletonList(acceptingValidator()));

    List<TelemetryPolicy> policies = provider.fetchPolicies();

    assertThat(policies).isEmpty();
  }

  @Test
  void fetchPoliciesParsesJsonLines() throws Exception {
    Path file = writeLines("{\"trace-sampling\": 0.5}");
    LinePerPolicyFileProvider provider =
        new LinePerPolicyFileProvider(file, Collections.singletonList(acceptingValidator()));

    List<TelemetryPolicy> policies = provider.fetchPolicies();

    assertThat(policies).hasSize(1);
    assertThat(policies.get(0).getType()).isEqualTo(TRACE_SAMPLING_TYPE);
  }

  @Test
  void fetchPoliciesParsesKeyValueLines() throws Exception {
    Path file = writeLines("trace-sampling=0.5");
    LinePerPolicyFileProvider provider =
        new LinePerPolicyFileProvider(file, Collections.singletonList(acceptingValidator()));

    List<TelemetryPolicy> policies = provider.fetchPolicies();

    assertThat(policies).hasSize(1);
    assertThat(policies.get(0).getType()).isEqualTo(TRACE_SAMPLING_TYPE);
  }

  @Test
  void fetchPoliciesSkipsBlankLinesAndComments() throws Exception {
    Path file = writeLines("", "   ", "# comment line", "trace-sampling=0.25");
    LinePerPolicyFileProvider provider =
        new LinePerPolicyFileProvider(file, Collections.singletonList(acceptingValidator()));

    List<TelemetryPolicy> policies = provider.fetchPolicies();

    assertThat(policies).hasSize(1);
    assertThat(policies.get(0).getType()).isEqualTo(TRACE_SAMPLING_TYPE);
  }

  @Test
  void fetchPoliciesSkipsUnknownOrRejectedPolicies() throws Exception {
    PolicyValidator rejectingValidator =
        new TestPolicyValidator(/* acceptJson= */ false, /* acceptKeyValue= */ false);
    Path file = writeLines("{\"trace-sampling\": 0.5}", "{\"other-policy\": 0.5}", "other.key=1");
    LinePerPolicyFileProvider provider =
        new LinePerPolicyFileProvider(file, Collections.singletonList(rejectingValidator));

    List<TelemetryPolicy> policies = provider.fetchPolicies();

    assertThat(policies).isEmpty();
  }

  private Path writeLines(String... lines) throws IOException {
    Path file = tempDir.resolve("policies.txt");
    Files.write(file, Arrays.asList(lines));
    return file;
  }

  private static PolicyValidator acceptingValidator() {
    return new TestPolicyValidator(/* acceptJson= */ true, /* acceptKeyValue= */ true);
  }

  private static class TestPolicyValidator implements PolicyValidator {
    private final boolean acceptJson;
    private final boolean acceptKeyValue;

    private TestPolicyValidator(boolean acceptJson, boolean acceptKeyValue) {
      this.acceptJson = acceptJson;
      this.acceptKeyValue = acceptKeyValue;
    }

    @Override
    public String getPolicyType() {
      return TRACE_SAMPLING_TYPE;
    }

    @Override
    public TelemetryPolicy validate(SourceWrapper source) {
      if (source.getFormat() == SourceFormat.JSONKEYVALUE) {
        if (!acceptJson) {
          return null;
        }
        return new TelemetryPolicy(TRACE_SAMPLING_TYPE);
      }
      if (source.getFormat() == SourceFormat.KEYVALUE) {
        if (!acceptKeyValue) {
          return null;
        }
        return new TelemetryPolicy(TRACE_SAMPLING_TYPE);
      }
      return null;
    }
  }
}
