/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.cloudfoundry.resources;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.SchemaUrls;
import io.opentelemetry.semconv.incubating.CloudfoundryIncubatingAttributes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CloudFoundryResourceTest {

  private static Map<String, String> createVcapApplicationEnv(String value) {
    Map<String, String> environment = new HashMap<>();
    environment.put("VCAP_APPLICATION", value);
    return environment;
  }

  private static String loadVcapApplicationSample(String filename) {
    try (InputStream is =
        CloudFoundryResourceTest.class.getClassLoader().getResourceAsStream(filename)) {
      if (is != null) {
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining());
      }
      Assertions.fail("Cannot load resource " + filename);
    } catch (IOException e) {
      Assertions.fail("Error reading " + filename);
    }
    return "";
  }

  @Test
  void noVcapApplication() {
    Map<String, String> env = Collections.emptyMap();
    Resource resource = CloudFoundryResource.buildResource(env::get);
    assertThat(resource).isEqualTo(Resource.empty());
  }

  @Test
  void emptyVcapApplication() {
    Map<String, String> env = createVcapApplicationEnv("");
    Resource resource = CloudFoundryResource.buildResource(env::get);
    assertThat(resource).isEqualTo(Resource.empty());
  }

  @Test
  void fullVcapApplication() {
    String json = loadVcapApplicationSample("vcap_application.json");
    Map<String, String> env = createVcapApplicationEnv(json);

    Resource resource = CloudFoundryResource.buildResource(env::get);

    assertThat(resource.getSchemaUrl()).isEqualTo(SchemaUrls.V1_24_0);
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_APP_ID))
        .isEqualTo("0193a038-e615-7e5e-92ca-f4bcd7ba0a25");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_APP_INSTANCE_ID))
        .isEqualTo("1");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_APP_NAME))
        .isEqualTo("cf-app-name");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_ORG_ID))
        .isEqualTo("0193a375-8d8e-7e0c-a832-01ce9ded40dc");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_ORG_NAME))
        .isEqualTo("cf-org-name");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_PROCESS_ID))
        .isEqualTo("0193a4e3-8fd3-71b9-9fe3-5640c53bf1e2");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_PROCESS_TYPE))
        .isEqualTo("web");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_SPACE_ID))
        .isEqualTo("0193a7e7-da17-7ea4-8940-b1e07b401b16");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_SPACE_NAME))
        .isEqualTo("cf-space-name");
  }
}
