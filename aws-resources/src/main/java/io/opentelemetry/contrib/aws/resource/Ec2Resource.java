/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_ACCOUNT_ID;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_AVAILABILITY_ZONE;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CloudPlatformIncubatingValues.AWS_EC2;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.HOST_ID;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.HOST_IMAGE_ID;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.HOST_NAME;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.HOST_TYPE;
import static java.util.logging.Level.WARNING;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.SchemaUrls;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A factory for a {@link Resource} which provides information about the current EC2 instance if
 * running on AWS EC2.
 */
public final class Ec2Resource {

  private static final Logger logger = Logger.getLogger(Ec2Resource.class.getName());

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private static final String DEFAULT_IMDS_ENDPOINT = "169.254.169.254";

  private static final Resource INSTANCE = buildResource();

  /**
   * Returns a @link Resource} which provides information about the current EC2 instance if running
   * on AWS EC2.
   */
  public static Resource get() {
    return INSTANCE;
  }

  private static Resource buildResource() {
    // This property is only for testing e.g., with a mock IMDS server and never in production so we
    // just
    // read from a system property. This is similar to the AWS SDK.
    return buildResource(
        System.getProperty("otel.aws.imds.endpointOverride", DEFAULT_IMDS_ENDPOINT));
  }

  // Visible for testing
  static Resource buildResource(String endpoint) {
    String urlBase = "http://" + endpoint;
    URL identityDocumentUrl;
    URL hostnameUrl;
    URL tokenUrl;
    try {
      identityDocumentUrl = new URL(urlBase + "/latest/dynamic/instance-identity/document");
      hostnameUrl = new URL(urlBase + "/latest/meta-data/hostname");
      tokenUrl = new URL(urlBase + "/latest/api/token");
    } catch (MalformedURLException e) {
      // Can only happen when overriding the endpoint in testing so just throw.
      throw new IllegalArgumentException("Illegal endpoint: " + endpoint, e);
    }

    String token = fetchToken(tokenUrl);

    // If token is empty, either IMDSv2 isn't enabled or an unexpected failure happened. We can
    // still get data if IMDSv1 is enabled.
    String identity = fetchIdentity(identityDocumentUrl, token);
    if (identity.isEmpty()) {
      // If no identity document, assume we are not actually running on EC2.
      return Resource.empty();
    }

    String hostname = fetchHostname(hostnameUrl, token);

    AttributesBuilder attrBuilders = Attributes.builder();
    attrBuilders.put(CLOUD_PROVIDER, IncubatingAttributes.CloudProviderIncubatingValues.AWS);
    attrBuilders.put(CLOUD_PLATFORM, AWS_EC2);

    try (JsonParser parser = JSON_FACTORY.createParser(identity)) {
      parser.nextToken();

      if (!parser.isExpectedStartObjectToken()) {
        throw new IOException("Invalid JSON:" + identity);
      }

      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String value = parser.nextTextValue();
        switch (parser.currentName()) {
          case "instanceId":
            attrBuilders.put(HOST_ID, value);
            break;
          case "availabilityZone":
            attrBuilders.put(CLOUD_AVAILABILITY_ZONE, value);
            break;
          case "instanceType":
            attrBuilders.put(HOST_TYPE, value);
            break;
          case "imageId":
            attrBuilders.put(HOST_IMAGE_ID, value);
            break;
          case "accountId":
            attrBuilders.put(CLOUD_ACCOUNT_ID, value);
            break;
          case "region":
            attrBuilders.put(CLOUD_REGION, value);
            break;
          default:
            parser.skipChildren();
        }
      }
    } catch (IOException e) {
      logger.log(WARNING, "Could not parse identity document, resource not filled.", e);
      return Resource.empty();
    }

    attrBuilders.put(HOST_NAME, hostname);

    return Resource.create(attrBuilders.build(), SchemaUrls.V1_25_0);
  }

  private static String fetchToken(URL tokenUrl) {
    return fetchString("PUT", tokenUrl, "", /* includeTtl= */ true);
  }

  private static String fetchIdentity(URL identityDocumentUrl, String token) {
    return fetchString("GET", identityDocumentUrl, token, /* includeTtl= */ false);
  }

  private static String fetchHostname(URL hostnameUrl, String token) {
    return fetchString("GET", hostnameUrl, token, /* includeTtl= */ false);
  }

  // Generic HTTP fetch function for IMDS.
  private static String fetchString(String httpMethod, URL url, String token, boolean includeTtl) {
    SimpleHttpClient client = new SimpleHttpClient();
    Map<String, String> headers = new HashMap<>();

    if (includeTtl) {
      headers.put("X-aws-ec2-metadata-token-ttl-seconds", "60");
    }
    if (!token.isEmpty()) {
      headers.put("X-aws-ec2-metadata-token", token);
    }

    return client.fetchString(httpMethod, url.toString(), headers, null);
  }

  private Ec2Resource() {}
}
