/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PolicyProviderConfigTest {

  @Test
  void copiesAndProtectsOpampHeaders() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer token");

    PolicyProviderConfig config = PolicyProviderConfig.createWithOpampHeaders(properties, headers);
    headers.put("X-Changed", "after-construction");

    assertThat(config.getProperties()).isSameAs(properties);
    assertThat(config.getOpampHeaders()).containsEntry("Authorization", "Bearer token");
    assertThat(config.getOpampHeaders()).doesNotContainKey("X-Changed");
    assertThatThrownBy(() -> config.getOpampHeaders().put("X-New", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void defaultsToNoOpampHeaders() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);

    assertThat(PolicyProviderConfig.create(properties).getOpampHeaders()).isEmpty();
  }
}
