/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

import static io.opentelemetry.contrib.gcp.auth.GcpAuthCustomizerProvider.SIGNAL_TARGET_WARNING_FIX_SUGGESTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.auth.oauth2.GoogleCredentials;
import io.opentelemetry.contrib.sdk.autoconfigure.ConfigPropertiesUtil;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class GcpAuthCustomizerProviderTest {

  @Test
  void declarativeConfig() throws IOException {
    String yaml =
        "file_format: 0.4\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - simple:\n"
            + "        exporter:\n"
            + "          otlp_http:\n"
            + "meter_provider:\n"
            + "  readers:\n"
            + "    - periodic:\n"
            + "        exporter:\n"
            + "          otlp_http:\n"
            + "instrumentation/development:\n"
            + "  java:\n"
            + "    google:\n"
            + "      cloud:\n"
            + "        project: p\n"
            + "        quota:\n"
            + "          project: qp\n"
            + "      otel:\n"
            + "        auth:\n"
            + "          target:\n"
            + "            signals: [metrics, traces]\n";

    OpenTelemetryConfigurationModel model =
        DeclarativeConfiguration.parse(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    ConfigProperties properties = ConfigPropertiesUtil.resolveModel(model);

    assertThat(GcpAuthAutoConfigurationCustomizerProvider.targetSignals(properties))
        .containsExactly("metrics", "traces");
    assertThat(GcpAuthAutoConfigurationCustomizerProvider.getProjectId(properties)).isEqualTo("p");
    assertThat(GcpAuthAutoConfigurationCustomizerProvider.getQuotaProjectId(properties))
        .contains("qp");

    GoogleCredentials credentials = mock(GoogleCredentials.class);
    when(credentials.getRequestMetadata())
        .thenReturn(
            Collections.singletonMap("x-goog-user-project", Collections.singletonList("qp")));

    GcpAuthCustomizerProvider.customize(model, credentials, properties);

    String header =
        "headers=\\[io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.NameStringValuePairModel@.*\\[name=x-goog-user-project,value=qp]";
    // both metrics and traces should have the header
    assertThat(model.toString()).matches(String.format(".*%s.*%s.*", header, header));
  }

  @Test
  void fixSuggestion() {
    assertThat(SIGNAL_TARGET_WARNING_FIX_SUGGESTION)
        .isEqualTo(
            "You may safely ignore this warning if it is intentional, "
                + "otherwise please configure the 'Target Signals for Google Authentication Extension' "
                + "by setting "
                + "'instrumentation/development' / 'java' / 'google' / 'otel' / 'auth' / 'target' / "
                + "'signals' in the configuration file.");
  }
}
