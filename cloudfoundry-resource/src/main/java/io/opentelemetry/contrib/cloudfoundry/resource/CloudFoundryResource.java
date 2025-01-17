/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.cloudfoundry.resource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.SchemaUrls;
import java.io.IOException;
import java.util.function.Function;
import java.util.logging.Logger;

public final class CloudFoundryResource {

  private static final String ENV_VCAP_APPLICATION = "VCAP_APPLICATION";

  // copied from CloudfoundryIncubatingAttributes
  private static final AttributeKey<String> CLOUDFOUNDRY_APP_ID =
      AttributeKey.stringKey("cloudfoundry.app.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_APP_INSTANCE_ID =
      AttributeKey.stringKey("cloudfoundry.app.instance.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_APP_NAME =
      AttributeKey.stringKey("cloudfoundry.app.name");
  private static final AttributeKey<String> CLOUDFOUNDRY_ORG_ID =
      AttributeKey.stringKey("cloudfoundry.org.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_ORG_NAME =
      AttributeKey.stringKey("cloudfoundry.org.name");
  private static final AttributeKey<String> CLOUDFOUNDRY_PROCESS_ID =
      AttributeKey.stringKey("cloudfoundry.process.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_PROCESS_TYPE =
      AttributeKey.stringKey("cloudfoundry.process.type");
  private static final AttributeKey<String> CLOUDFOUNDRY_SPACE_ID =
      AttributeKey.stringKey("cloudfoundry.space.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_SPACE_NAME =
      AttributeKey.stringKey("cloudfoundry.space.name");
  private static final Logger LOG = Logger.getLogger(CloudFoundryResource.class.getName());
  private static final JsonFactory JSON_FACTORY = new JsonFactory();
  private static final Resource INSTANCE = buildResource(System::getenv);

  private CloudFoundryResource() {}

  public static Resource get() {
    return INSTANCE;
  }

  static Resource buildResource(Function<String, String> getenv) {
    String vcapAppRaw = getenv.apply(ENV_VCAP_APPLICATION);
    // If there is no VCAP_APPLICATION in the environment, we are likely not running in CloudFoundry
    if (vcapAppRaw == null || vcapAppRaw.isEmpty()) {
      return Resource.empty();
    }

    AttributesBuilder builder = Attributes.builder();
    try (JsonParser parser = JSON_FACTORY.createParser(vcapAppRaw)) {
      parser.nextToken();
      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String name = parser.currentName();
        parser.nextToken();
        String value = parser.getValueAsString();
        switch (name) {
          case "application_id":
            builder.put(CLOUDFOUNDRY_APP_ID, value);
            break;
          case "application_name":
            builder.put(CLOUDFOUNDRY_APP_NAME, value);
            break;
          case "instance_index":
            builder.put(CLOUDFOUNDRY_APP_INSTANCE_ID, value);
            break;
          case "organization_id":
            builder.put(CLOUDFOUNDRY_ORG_ID, value);
            break;
          case "organization_name":
            builder.put(CLOUDFOUNDRY_ORG_NAME, value);
            break;
          case "process_id":
            builder.put(CLOUDFOUNDRY_PROCESS_ID, value);
            break;
          case "process_type":
            builder.put(CLOUDFOUNDRY_PROCESS_TYPE, value);
            break;
          case "space_id":
            builder.put(CLOUDFOUNDRY_SPACE_ID, value);
            break;
          case "space_name":
            builder.put(CLOUDFOUNDRY_SPACE_NAME, value);
            break;
          default:
            parser.skipChildren();
            break;
        }
      }
    } catch (IOException e) {
      LOG.warning("Cannot parse contents of environment variable VCAP_APPLICATION. Invalid JSON");
    }

    return Resource.create(builder.build(), SchemaUrls.V1_24_0);
  }
}
