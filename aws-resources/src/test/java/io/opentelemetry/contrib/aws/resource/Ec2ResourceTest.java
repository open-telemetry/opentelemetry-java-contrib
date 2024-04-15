/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_ACCOUNT_ID;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_ID;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_IMAGE_ID;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_NAME;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_TYPE;
import static org.assertj.core.api.Assertions.entry;

import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.testing.junit5.server.mock.MockWebServerExtension;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ServiceLoader;
import io.opentelemetry.semconv.SchemaUrls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class Ec2ResourceTest {

  // From https://docs.amazonaws.cn/en_us/AWSEC2/latest/UserGuide/instance-identity-documents.html
  private static final String IDENTITY_DOCUMENT =
      "{\n"
          + "    \"devpayProductCodes\" : null,\n"
          + "    \"marketplaceProductCodes\" : [ \"1abc2defghijklm3nopqrs4tu\" ], \n"
          + "    \"availabilityZone\" : \"us-west-2b\",\n"
          + "    \"privateIp\" : \"10.158.112.84\",\n"
          + "    \"version\" : \"2017-09-30\",\n"
          + "    \"instanceId\" : \"i-1234567890abcdef0\",\n"
          + "    \"billingProducts\" : null,\n"
          + "    \"instanceType\" : \"t2.micro\",\n"
          + "    \"accountId\" : \"123456789012\",\n"
          + "    \"imageId\" : \"ami-5fb8c835\",\n"
          + "    \"pendingTime\" : \"2016-11-19T16:32:11Z\",\n"
          + "    \"architecture\" : \"x86_64\",\n"
          + "    \"kernelId\" : null,\n"
          + "    \"ramdiskId\" : null,\n"
          + "    \"region\" : \"us-west-2\"\n"
          + "}";

  @RegisterExtension
  public static final MockWebServerExtension server = new MockWebServerExtension();

  @Test
  void imdsv2() {
    server.enqueue(HttpResponse.of("token"));
    server.enqueue(HttpResponse.of(MediaType.JSON_UTF_8, IDENTITY_DOCUMENT));
    server.enqueue(HttpResponse.of("ec2-1-2-3-4"));

    Resource resource = Ec2Resource.buildResource("localhost:" + server.httpPort());
    assertThat(resource.getSchemaUrl()).isEqualTo(SchemaUrls.V1_25_0);
    Attributes attributes = resource.getAttributes();

    assertThat(attributes)
        .containsOnly(
            entry(CLOUD_PROVIDER, "aws"),
            entry(CLOUD_PLATFORM, "aws_ec2"),
            entry(HOST_ID, "i-1234567890abcdef0"),
            entry(CLOUD_AVAILABILITY_ZONE, "us-west-2b"),
            entry(HOST_TYPE, "t2.micro"),
            entry(HOST_IMAGE_ID, "ami-5fb8c835"),
            entry(CLOUD_ACCOUNT_ID, "123456789012"),
            entry(CLOUD_REGION, "us-west-2"),
            entry(HOST_NAME, "ec2-1-2-3-4"));

    AggregatedHttpRequest request1 = server.takeRequest().request();
    assertThat(request1.path()).isEqualTo("/latest/api/token");
    assertThat(request1.headers().get("X-aws-ec2-metadata-token-ttl-seconds")).isEqualTo("60");

    AggregatedHttpRequest request2 = server.takeRequest().request();
    assertThat(request2.path()).isEqualTo("/latest/dynamic/instance-identity/document");
    assertThat(request2.headers().get("X-aws-ec2-metadata-token")).isEqualTo("token");

    AggregatedHttpRequest request3 = server.takeRequest().request();
    assertThat(request3.path()).isEqualTo("/latest/meta-data/hostname");
    assertThat(request3.headers().get("X-aws-ec2-metadata-token")).isEqualTo("token");
  }

  @Test
  void imdsv1() {
    server.enqueue(HttpResponse.of(HttpStatus.NOT_FOUND));
    server.enqueue(HttpResponse.of(MediaType.JSON_UTF_8, IDENTITY_DOCUMENT));
    server.enqueue(HttpResponse.of("ec2-1-2-3-4"));

    Resource resource = Ec2Resource.buildResource("localhost:" + server.httpPort());
    assertThat(resource.getSchemaUrl()).isEqualTo(SchemaUrls.V1_25_0);
    Attributes attributes = resource.getAttributes();

    assertThat(attributes)
        .containsOnly(
            entry(CLOUD_PROVIDER, "aws"),
            entry(CLOUD_PLATFORM, "aws_ec2"),
            entry(HOST_ID, "i-1234567890abcdef0"),
            entry(CLOUD_AVAILABILITY_ZONE, "us-west-2b"),
            entry(HOST_TYPE, "t2.micro"),
            entry(HOST_IMAGE_ID, "ami-5fb8c835"),
            entry(CLOUD_ACCOUNT_ID, "123456789012"),
            entry(CLOUD_REGION, "us-west-2"),
            entry(HOST_NAME, "ec2-1-2-3-4"));

    AggregatedHttpRequest request1 = server.takeRequest().request();
    assertThat(request1.path()).isEqualTo("/latest/api/token");
    assertThat(request1.headers().get("X-aws-ec2-metadata-token-ttl-seconds")).isEqualTo("60");

    AggregatedHttpRequest request2 = server.takeRequest().request();
    assertThat(request2.path()).isEqualTo("/latest/dynamic/instance-identity/document");
    assertThat(request2.headers().get("X-aws-ec2-metadata-token")).isNull();
  }

  @Test
  void badJson() {
    server.enqueue(HttpResponse.of(HttpStatus.NOT_FOUND));
    server.enqueue(HttpResponse.of(MediaType.JSON_UTF_8, "I'm not JSON"));

    Attributes attributes =
        Ec2Resource.buildResource("localhost:" + server.httpPort()).getAttributes();
    assertThat(attributes).isEmpty();

    AggregatedHttpRequest request1 = server.takeRequest().request();
    assertThat(request1.path()).isEqualTo("/latest/api/token");
    assertThat(request1.headers().get("X-aws-ec2-metadata-token-ttl-seconds")).isEqualTo("60");

    AggregatedHttpRequest request2 = server.takeRequest().request();
    assertThat(request2.path()).isEqualTo("/latest/dynamic/instance-identity/document");
    assertThat(request2.headers().get("X-aws-ec2-metadata-token")).isNull();
  }

  @Test
  void inServiceLoader() {
    // No practical way to test the attributes themselves so at least check the service loader picks
    // it up.
    assertThat(ServiceLoader.load(ResourceProvider.class))
        .anyMatch(Ec2ResourceProvider.class::isInstance);
  }
}
