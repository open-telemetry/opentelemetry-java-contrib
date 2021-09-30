/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class OtelUtilsTest {

  @Test
  public void test_getCommaSeparatedMap_resources() {
    String resourceAttributes =
        "service.name=frontend,service.namespace=com-mycompany-mydomain,service.version=1.0,deployment.environment=production";
    final Map<String, String> actualResourceAttributes =
        OtelUtils.getCommaSeparatedMap(resourceAttributes);

    assertThat(actualResourceAttributes.get("service.name")).isEqualTo("frontend");
    assertThat(actualResourceAttributes.get("service.namespace"))
        .isEqualTo("com-mycompany-mydomain");
    assertThat(actualResourceAttributes.get("service.version")).isEqualTo("1.0");
    assertThat(actualResourceAttributes.get("deployment.environment")).isEqualTo("production");
  }
}
