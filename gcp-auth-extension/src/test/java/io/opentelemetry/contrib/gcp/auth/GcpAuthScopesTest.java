/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.auth.oauth2.GoogleCredentials;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GcpAuthScopesTest {
  static Stream<Arguments> targetSignals() {
    String traces = "https://www.googleapis.com/auth/trace.append";
    String metrics = "https://www.googleapis.com/auth/monitoring.write";
    String logs = "https://www.googleapis.com/auth/logging.write";
    return Stream.of(
        Arguments.of("traces", Collections.singletonList(traces)),
        Arguments.of("metrics", Collections.singletonList(metrics)),
        Arguments.of("logs", Collections.singletonList(logs)),
        Arguments.of(" traces , metrics , traces ", Arrays.asList(traces, metrics)),
        Arguments.of("all", Arrays.asList(traces, metrics, logs)),
        Arguments.of(null, Arrays.asList(traces, metrics, logs)),
        Arguments.of("none", Collections.emptyList()),
        Arguments.of("none,traces", Collections.emptyList()));
  }

  @ParameterizedTest
  @MethodSource("targetSignals")
  @SuppressWarnings("CannotMockMethod")
  void appliesOnlyTargetedScopes(String signals, List<String> scopes) {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString("google.otel.auth.target.signals")).thenReturn(signals);
    GoogleCredentials credentials = mock(GoogleCredentials.class);
    when(credentials.createScopedRequired()).thenReturn(true);
    GoogleCredentials scoped = mock(GoogleCredentials.class);
    if (!scopes.isEmpty()) {
      when(credentials.createScoped(scopes)).thenReturn(scoped);
    }
    assertThat(
            GcpAuthAutoConfigurationCustomizerProvider.scopeCredentialsForTargetSignals(
                credentials, config))
        .isSameAs(scopes.isEmpty() ? credentials : scoped);
    verify(credentials).createScopedRequired();
    if (!scopes.isEmpty()) {
      verify(credentials).createScoped(scopes);
    }
    verifyNoMoreInteractions(credentials);
  }

  @Test
  @SuppressWarnings("CannotMockMethod")
  void preservesCredentialsThatDoNotRequireScopes() {
    GoogleCredentials credentials = mock(GoogleCredentials.class);
    when(credentials.createScopedRequired()).thenReturn(false);
    assertThat(
            GcpAuthAutoConfigurationCustomizerProvider.scopeCredentialsForTargetSignals(
                credentials, mock(ConfigProperties.class)))
        .isSameAs(credentials);
    verify(credentials).createScopedRequired();
    verifyNoMoreInteractions(credentials);
  }
}
