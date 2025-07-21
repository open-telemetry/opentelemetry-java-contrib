/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.AwsIncubatingAttributes.AWS_ECS_CONTAINER_ARN;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_RESOURCE_ID;
import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_URL;
import static io.opentelemetry.semconv.incubating.NetIncubatingAttributes.NET_HOST_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingTargetDocument;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.time.TestClock;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"JavaUtilDate", "deprecation"}) // uses deprecated semantic conventions
class SamplingRuleApplierTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String CLIENT_ID = "test-client-id";

  @Nested
  @SuppressWarnings("ClassCanBeStatic")
  class ExactMatch {

    private final SamplingRuleApplier applier =
        new SamplingRuleApplier(
            CLIENT_ID, readSamplingRule("/sampling-rule-exactmatch.json"), Clock.getDefault());

    private final Resource resource =
        Resource.builder()
            .put(SERVICE_NAME, "test-service-foo-bar")
            .put(CLOUD_PLATFORM, CloudIncubatingAttributes.CloudPlatformIncubatingValues.AWS_EKS)
            .put(AWS_ECS_CONTAINER_ARN, "arn:aws:xray:us-east-1:595986152929:my-service")
            .build();

    private final Attributes attributes =
        Attributes.builder()
            .put(HTTP_METHOD, "GET")
            .put(NET_HOST_NAME, "opentelemetry.io")
            .put(HTTP_TARGET, "/instrument-me")
            .put(AttributeKey.stringKey("animal"), "cat")
            .put(AttributeKey.longKey("speed"), 10)
            .build();

    private final Attributes stableSemConvAttributes =
        Attributes.builder()
            .put(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
            .put(ServerAttributes.SERVER_ADDRESS, "opentelemetry.io")
            .put(UrlAttributes.URL_PATH, "/instrument-me")
            .put(AttributeKey.stringKey("animal"), "cat")
            .put(AttributeKey.longKey("speed"), 10)
            .build();

    // FixedRate set to 1.0 in rule and no reservoir
    @Test
    void fixedRateAlwaysSample() {
      assertThat(doSample(applier))
          .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));

      Date now = new Date();
      GetSamplingTargetsRequest.SamplingStatisticsDocument statistics = applier.snapshot(now);
      assertThat(statistics.getClientId()).isEqualTo(CLIENT_ID);
      assertThat(statistics.getRuleName()).isEqualTo("Test");
      assertThat(statistics.getTimestamp()).isEqualTo(now);
      assertThat(statistics.getRequestCount()).isEqualTo(1);
      assertThat(statistics.getSampledCount()).isEqualTo(1);
      assertThat(statistics.getBorrowCount()).isEqualTo(0);

      // Reset
      statistics = applier.snapshot(now);
      assertThat(statistics.getRequestCount()).isEqualTo(0);
      assertThat(statistics.getSampledCount()).isEqualTo(0);
      assertThat(statistics.getBorrowCount()).isEqualTo(0);

      doSample(applier);
      doSample(applier);
      now = new Date();
      statistics = applier.snapshot(now);
      assertThat(statistics.getClientId()).isEqualTo(CLIENT_ID);
      assertThat(statistics.getRuleName()).isEqualTo("Test");
      assertThat(statistics.getTimestamp()).isEqualTo(now);
      assertThat(statistics.getRequestCount()).isEqualTo(2);
      assertThat(statistics.getSampledCount()).isEqualTo(2);
      assertThat(statistics.getBorrowCount()).isEqualTo(0);
    }

    @Test
    void matches() {
      assertThat(applier.matches(attributes, resource)).isTrue();

      // http.url works too
      assertThat(
              applier.matches(
                  attributes.toBuilder()
                      .remove(HTTP_TARGET)
                      .put(HTTP_URL, "scheme://host:port/instrument-me")
                      .build(),
                  resource))
          .isTrue();
    }

    @Test
    void matchesURLFullStableSemConv() {
      assertThat(applier.matches(stableSemConvAttributes, resource)).isTrue();

      // url.full works too
      assertThat(
              applier.matches(
                  attributes.toBuilder()
                      .remove(HTTP_TARGET)
                      .put(UrlAttributes.URL_FULL, "scheme://host:port/instrument-me")
                      .build(),
                  resource))
          .isTrue();
    }

    @Test
    void serviceNameNotMatch() {
      assertThat(
              applier.matches(
                  attributes,
                  resource.toBuilder().put(SERVICE_NAME, "test-service-foo-baz").build()))
          .isFalse();
    }

    @Test
    void serviceNameNullNotMatch() {
      assertThat(applier.matches(attributes, Resource.empty())).isFalse();
    }

    @Test
    void methodNotMatch() {
      Attributes attributes = this.attributes.toBuilder().put(HTTP_METHOD, "POST").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void methodStableSemConvNotMatch() {
      Attributes attributes =
          this.stableSemConvAttributes.toBuilder()
              .put(HttpAttributes.HTTP_REQUEST_METHOD, "POST")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void hostNotMatch() {
      // Replacing dot with character makes sure we're not accidentally treating dot as regex
      // wildcard.
      Attributes attributes =
          this.attributes.toBuilder().put(NET_HOST_NAME, "opentelemetryfio").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void pathNotMatch() {
      Attributes attributes =
          this.attributes.toBuilder().put(HTTP_TARGET, "/instrument-you").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes =
          this.attributes.toBuilder()
              .remove(HTTP_TARGET)
              .put(HTTP_URL, "scheme://host:port/instrument-you")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes =
          this.attributes.toBuilder()
              .remove(HTTP_TARGET)
              .put(HTTP_URL, "scheme://host:port")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();

      // Correct path, but we ignore anyways since the URL is malformed per spec, scheme is always
      // present.
      attributes =
          this.attributes.toBuilder()
              .remove(HTTP_TARGET)
              .put(HTTP_URL, "host:port/instrument-me")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void pathStableSemConvNotMatch() {
      Attributes attributes =
          this.stableSemConvAttributes.toBuilder()
              .put(UrlAttributes.URL_PATH, "/instrument-you")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes =
          this.stableSemConvAttributes.toBuilder()
              .remove(UrlAttributes.URL_PATH)
              .put(UrlAttributes.URL_FULL, "scheme://host:port/instrument-you")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes =
          this.stableSemConvAttributes.toBuilder()
              .remove(UrlAttributes.URL_PATH)
              .put(UrlAttributes.URL_FULL, "scheme://host:port")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();

      // Correct path, but we ignore anyways since the URL is malformed per spec, scheme is always
      // present.
      attributes =
          this.stableSemConvAttributes.toBuilder()
              .remove(UrlAttributes.URL_PATH)
              .put(UrlAttributes.URL_FULL, "host:port/instrument-me")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void attributeNotMatch() {
      Attributes attributes =
          this.attributes.toBuilder().put(AttributeKey.stringKey("animal"), "dog").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void attributeMissing() {
      Attributes attributes = removeAttribute(this.attributes, AttributeKey.stringKey("animal"));
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void serviceTypeNotMatch() {
      Resource resource =
          this.resource.toBuilder()
              .put(CLOUD_PLATFORM, CloudIncubatingAttributes.CloudPlatformIncubatingValues.AWS_EC2)
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      resource = Resource.create(removeAttribute(this.resource.getAttributes(), CLOUD_PLATFORM));
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void arnNotMatch() {
      Resource resource =
          this.resource.toBuilder()
              .put(AWS_ECS_CONTAINER_ARN, "arn:aws:xray:us-east-1:595986152929:my-service2")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
    }
  }

  @Nested
  @SuppressWarnings("ClassCanBeStatic")
  class WildcardMatch {

    private final SamplingRuleApplier applier =
        new SamplingRuleApplier(
            CLIENT_ID, readSamplingRule("/sampling-rule-wildcards.json"), Clock.getDefault());

    private final Resource resource =
        Resource.builder()
            .put(SERVICE_NAME, "test-service-foo-bar")
            .put(CLOUD_PLATFORM, CloudIncubatingAttributes.CloudPlatformIncubatingValues.AWS_EKS)
            .put(AWS_ECS_CONTAINER_ARN, "arn:aws:xray:us-east-1:595986152929:my-service")
            .build();

    private final Attributes attributes =
        Attributes.builder()
            .put(HTTP_METHOD, "GET")
            .put(NET_HOST_NAME, "opentelemetry.io")
            .put(HTTP_TARGET, "/instrument-me?foo=bar&cat=meow")
            .put(AttributeKey.stringKey("animal"), "cat")
            .put(AttributeKey.longKey("speed"), 10)
            .build();

    private final Attributes stableSemConvAttributes =
        Attributes.builder()
            .put(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
            .put(ServerAttributes.SERVER_ADDRESS, "opentelemetry.io")
            .put(UrlAttributes.URL_PATH, "/instrument-me?foo=bar&cat=meow")
            .put(AttributeKey.stringKey("animal"), "cat")
            .put(AttributeKey.longKey("speed"), 10)
            .build();

    // FixedRate set to 0.0 in rule and no reservoir
    @Test
    void fixedRateNeverSample() {
      assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));

      Date now = new Date();
      GetSamplingTargetsRequest.SamplingStatisticsDocument statistics = applier.snapshot(now);
      assertThat(statistics.getClientId()).isEqualTo(CLIENT_ID);
      assertThat(statistics.getRuleName()).isEqualTo("Test");
      assertThat(statistics.getTimestamp()).isEqualTo(now);
      assertThat(statistics.getRequestCount()).isEqualTo(1);
      assertThat(statistics.getSampledCount()).isEqualTo(0);
      assertThat(statistics.getBorrowCount()).isEqualTo(0);

      // Reset
      statistics = applier.snapshot(now);
      assertThat(statistics.getRequestCount()).isEqualTo(0);
      assertThat(statistics.getSampledCount()).isEqualTo(0);
      assertThat(statistics.getBorrowCount()).isEqualTo(0);

      doSample(applier);
      doSample(applier);
      now = new Date();
      statistics = applier.snapshot(now);
      assertThat(statistics.getClientId()).isEqualTo(CLIENT_ID);
      assertThat(statistics.getRuleName()).isEqualTo("Test");
      assertThat(statistics.getTimestamp()).isEqualTo(now);
      assertThat(statistics.getRequestCount()).isEqualTo(2);
      assertThat(statistics.getSampledCount()).isEqualTo(0);
      assertThat(statistics.getBorrowCount()).isEqualTo(0);
    }

    @Test
    void serviceNameMatches() {
      assertThat(
              applier.matches(
                  attributes,
                  resource.toBuilder().put(SERVICE_NAME, "test-service-foo-bar").build()))
          .isTrue();
      assertThat(
              applier.matches(
                  attributes,
                  resource.toBuilder().put(SERVICE_NAME, "test-service-foo-baz").build()))
          .isTrue();
      assertThat(
              applier.matches(
                  attributes, resource.toBuilder().put(SERVICE_NAME, "test-service-foo-").build()))
          .isTrue();
    }

    @Test
    void serviceNameNotMatch() {
      assertThat(
              applier.matches(
                  attributes, resource.toBuilder().put(SERVICE_NAME, "test-service-foo").build()))
          .isFalse();
      assertThat(
              applier.matches(
                  attributes,
                  resource.toBuilder().put(SERVICE_NAME, "prod-service-foo-bar").build()))
          .isFalse();
      assertThat(applier.matches(attributes, Resource.empty())).isFalse();
    }

    @Test
    void methodMatches() {
      Attributes attributes = this.attributes.toBuilder().put(HTTP_METHOD, "BADGETGOOD").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.attributes.toBuilder().put(HTTP_METHOD, "BADGET").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.attributes.toBuilder().put(HTTP_METHOD, "GETGET").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void methodNotMatch() {
      Attributes attributes = this.attributes.toBuilder().put(HTTP_METHOD, "POST").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = removeAttribute(this.attributes, HTTP_METHOD);
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void stableSemConvMethodMatches() {
      Attributes attributes =
          this.stableSemConvAttributes.toBuilder()
              .put(HttpAttributes.HTTP_REQUEST_METHOD, "BADGETGOOD")
              .build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes =
          stableSemConvAttributes.toBuilder().put(HttpAttributes.HTTP_REQUEST_METHOD, "BADGET").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes =
          stableSemConvAttributes.toBuilder().put(HttpAttributes.HTTP_REQUEST_METHOD, "GETGET").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void stableSemConvMethodNotMatch() {
      Attributes attributes =
          stableSemConvAttributes.toBuilder().put(HttpAttributes.HTTP_REQUEST_METHOD, "POST").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = removeAttribute(stableSemConvAttributes, HttpAttributes.HTTP_REQUEST_METHOD);
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void hostMatches() {
      Attributes attributes =
          this.attributes.toBuilder().put(NET_HOST_NAME, "alpha.opentelemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.attributes.toBuilder().put(NET_HOST_NAME, "opfdnqtelemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.attributes.toBuilder().put(NET_HOST_NAME, "opentglemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.attributes.toBuilder().put(NET_HOST_NAME, "opentglemry.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.attributes.toBuilder().put(NET_HOST_NAME, "opentglemrz.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void hostNotMatch() {
      Attributes attributes =
          this.attributes.toBuilder().put(NET_HOST_NAME, "opentelemetryfio").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = this.attributes.toBuilder().put(NET_HOST_NAME, "opentgalemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = this.attributes.toBuilder().put(NET_HOST_NAME, "alpha.oentelemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = removeAttribute(this.attributes, NET_HOST_NAME);
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void stableSemConvHostMatches() {
      Attributes attributes =
          this.stableSemConvAttributes.toBuilder().put(ServerAttributes.SERVER_ADDRESS, "alpha.opentelemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.stableSemConvAttributes.toBuilder().put(ServerAttributes.SERVER_ADDRESS, "opfdnqtelemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.stableSemConvAttributes.toBuilder().put(ServerAttributes.SERVER_ADDRESS, "opentglemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.stableSemConvAttributes.toBuilder().put(ServerAttributes.SERVER_ADDRESS, "opentglemry.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.stableSemConvAttributes.toBuilder().put(ServerAttributes.SERVER_ADDRESS, "opentglemrz.io").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void stableSemConvHostNotMatch() {
      Attributes attributes =
          this.stableSemConvAttributes.toBuilder().put(ServerAttributes.SERVER_ADDRESS, "opentelemetryfio").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = this.stableSemConvAttributes.toBuilder().put(ServerAttributes.SERVER_ADDRESS, "opentgalemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = this.stableSemConvAttributes.toBuilder().put(ServerAttributes.SERVER_ADDRESS, "alpha.oentelemetry.io").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = removeAttribute(this.stableSemConvAttributes, ServerAttributes.SERVER_ADDRESS);
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void pathMatches() {
      Attributes attributes =
          this.attributes.toBuilder().put(HTTP_TARGET, "/instrument-me?foo=bar&cat=").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      // Deceptive question mark, it's actually a wildcard :-)
      attributes =
          this.attributes.toBuilder().put(HTTP_TARGET, "/instrument-meafoo=bar&cat=").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void pathNotMatch() {
      Attributes attributes =
          this.attributes.toBuilder().put(HTTP_TARGET, "/instrument-mea?foo=bar&cat=").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes =
          this.attributes.toBuilder().put(HTTP_TARGET, "foo/instrument-meafoo=bar&cat=").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = removeAttribute(this.attributes, HTTP_TARGET);
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void pathStableSemConvMatches() {
      Attributes attributes =
          stableSemConvAttributes.toBuilder()
              .put(UrlAttributes.URL_PATH, "/instrument-me?foo=bar&cat=")
              .build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      // Deceptive question mark, it's actually a wildcard :-)
      attributes =
          stableSemConvAttributes.toBuilder()
              .put(UrlAttributes.URL_PATH, "/instrument-meafoo=bar&cat=")
              .build();
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void pathStableSemConvNotMatch() {
      Attributes attributes =
          stableSemConvAttributes.toBuilder()
              .put(UrlAttributes.URL_PATH, "/instrument-mea?foo=bar&cat=")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes =
          stableSemConvAttributes.toBuilder()
              .put(UrlAttributes.URL_PATH, "foo/instrument-meafoo=bar&cat=")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = removeAttribute(stableSemConvAttributes, UrlAttributes.URL_PATH);
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void attributeMatches() {
      Attributes attributes =
          this.attributes.toBuilder().put(AttributeKey.stringKey("animal"), "catman").build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      attributes = this.attributes.toBuilder().put(AttributeKey.longKey("speed"), 20).build();
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void attributeNotMatch() {
      Attributes attributes =
          this.attributes.toBuilder().put(AttributeKey.stringKey("animal"), "dog").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes =
          this.attributes.toBuilder().put(AttributeKey.stringKey("animal"), "mancat").build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      attributes = this.attributes.toBuilder().put(AttributeKey.longKey("speed"), 21).build();
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void attributeMissing() {
      Attributes attributes = removeAttribute(this.attributes, AttributeKey.stringKey("animal"));
      assertThat(applier.matches(attributes, resource)).isFalse();
    }

    @Test
    void serviceTypeMatches() {
      Resource resource =
          this.resource.toBuilder()
              .put(CLOUD_PLATFORM, CloudIncubatingAttributes.CloudPlatformIncubatingValues.AWS_EC2)
              .build();
      assertThat(applier.matches(attributes, resource)).isTrue();
      resource = Resource.create(removeAttribute(this.resource.getAttributes(), CLOUD_PLATFORM));
      // null matches for pattern '*'
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void arnMatches() {
      Resource resource =
          this.resource.toBuilder()
              .put(AWS_ECS_CONTAINER_ARN, "arn:aws:opentelemetry:us-east-3:52929:my-service")
              .build();
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void arnNotMatch() {
      Resource resource =
          this.resource.toBuilder()
              .put(AWS_ECS_CONTAINER_ARN, "arn:aws:xray:us-east-1:595986152929:my-service2")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      resource =
          this.resource.toBuilder()
              .put(AWS_ECS_CONTAINER_ARN, "frn:aws:xray:us-east-1:595986152929:my-service")
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      resource =
          Resource.create(removeAttribute(this.resource.getAttributes(), AWS_ECS_CONTAINER_ARN));
      assertThat(applier.matches(attributes, resource)).isFalse();
    }
  }

  @Nested
  @SuppressWarnings("ClassCanBeStatic")
  class AwsLambdaTest {

    private final SamplingRuleApplier applier =
        new SamplingRuleApplier(
            CLIENT_ID, readSamplingRule("/sampling-rule-awslambda.json"), Clock.getDefault());

    private final Resource resource =
        Resource.builder()
            .put(CLOUD_PLATFORM, CloudIncubatingAttributes.CloudPlatformIncubatingValues.AWS_LAMBDA)
            .put(CLOUD_RESOURCE_ID, "arn:aws:xray:us-east-1:595986152929:my-service")
            .build();

    private final Attributes attributes =
        Attributes.builder()
            .put(HTTP_METHOD, "GET")
            .put(NET_HOST_NAME, "opentelemetry.io")
            .put(HTTP_TARGET, "/instrument-me")
            .put(AttributeKey.stringKey("animal"), "cat")
            .put(AttributeKey.longKey("speed"), 10)
            .build();

    @Test
    void resourceFaasIdMatches() {
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void spanFaasIdMatches() {
      Resource resource =
          Resource.create(removeAttribute(this.resource.getAttributes(), CLOUD_RESOURCE_ID));
      Attributes attributes =
          this.attributes.toBuilder()
              .put(CLOUD_RESOURCE_ID, "arn:aws:xray:us-east-1:595986152929:my-service")
              .build();
      assertThat(applier.matches(attributes, resource)).isTrue();
    }

    @Test
    void notLambdaNotMatches() {
      Resource resource =
          this.resource.toBuilder()
              .put(
                  CLOUD_PLATFORM,
                  CloudIncubatingAttributes.CloudPlatformIncubatingValues.GCP_CLOUD_FUNCTIONS)
              .build();
      assertThat(applier.matches(attributes, resource)).isFalse();
      resource = Resource.create(removeAttribute(this.resource.getAttributes(), CLOUD_PLATFORM));
      assertThat(applier.matches(attributes, resource)).isFalse();
    }
  }

  @Test
  void borrowing() {
    SamplingRuleApplier applier =
        new SamplingRuleApplier(
            CLIENT_ID, readSamplingRule("/sampling-rule-reservoir.json"), Clock.getDefault());

    // Borrow
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    // Can only borrow one per second. If a second passes between these two lines of code, the test
    // will be flaky. Revisit if we ever see it, it's unlikely but can be fixed by injecting a
    // a clock.
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));

    Date now = new Date();
    GetSamplingTargetsRequest.SamplingStatisticsDocument statistics = applier.snapshot(now);
    assertThat(statistics.getClientId()).isEqualTo(CLIENT_ID);
    assertThat(statistics.getRuleName()).isEqualTo("Test");
    assertThat(statistics.getTimestamp()).isEqualTo(now);
    assertThat(statistics.getRequestCount()).isEqualTo(2);
    assertThat(statistics.getSampledCount()).isEqualTo(1);
    assertThat(statistics.getBorrowCount()).isEqualTo(1);

    // Reset
    statistics = applier.snapshot(now);
    assertThat(statistics.getRequestCount()).isEqualTo(0);
    assertThat(statistics.getSampledCount()).isEqualTo(0);
    assertThat(statistics.getBorrowCount()).isEqualTo(0);

    AtomicInteger numRequests = new AtomicInteger();
    // Wait for reservoir to fill.
    await()
        .untilAsserted(
            () -> {
              numRequests.incrementAndGet();
              assertThat(doSample(applier))
                  .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
            });

    now = new Date();
    statistics = applier.snapshot(now);
    assertThat(statistics.getClientId()).isEqualTo(CLIENT_ID);
    assertThat(statistics.getRuleName()).isEqualTo("Test");
    assertThat(statistics.getTimestamp()).isEqualTo(now);
    assertThat(statistics.getRequestCount()).isEqualTo(numRequests.get());
    assertThat(statistics.getSampledCount()).isEqualTo(1);
    assertThat(statistics.getBorrowCount()).isEqualTo(1);
  }

  @Test
  void ruleWithTarget() {
    TestClock clock = TestClock.create();
    SamplingRuleApplier applier =
        new SamplingRuleApplier(
            CLIENT_ID, readSamplingRule("/sampling-rule-reservoir.json"), clock);
    // No target yet, borrows from reservoir every second.
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    clock.advance(Duration.ofSeconds(1));
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));

    Instant now = Instant.ofEpochSecond(0, clock.now());
    // No target so always have a snapshot.
    assertThat(applier.snapshot(Date.from(now))).isNotNull();
    assertThat(applier.snapshot(Date.from(now.plus(Duration.ofMinutes(30))))).isNotNull();

    // Got a target!
    SamplingTargetDocument target =
        SamplingTargetDocument.create(0.0, 5, 2, Date.from(now.plusSeconds(10)), "test");
    applier = applier.withTarget(target, Date.from(now));
    // Statistics not expired yet
    assertThat(applier.snapshot(Date.from(now))).isNull();

    // Got 2 requests per second quota.
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    clock.advance(Duration.ofSeconds(2));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(applier.snapshot(Date.from(now))).isNull();
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    clock.advance(Duration.ofSeconds(3));
    now = Instant.ofEpochSecond(0, clock.now());
    // Statistics expired, snapshot requests are served.
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(applier.snapshot(Date.from(now))).isNotNull();
    clock.advance(Duration.ofSeconds(5));
    // No more reservoir quota, back to fixed rate (0.0)
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
  }

  @Test
  void ruleWithTargetWithoutQuota() {
    TestClock clock = TestClock.create();
    SamplingRuleApplier applier =
        new SamplingRuleApplier(
            CLIENT_ID, readSamplingRule("/sampling-rule-reservoir.json"), clock);
    // No target yet, borrows from reservoir every second.
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    clock.advance(Duration.ofSeconds(1));
    Instant now = Instant.ofEpochSecond(0, clock.now());
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));

    // No target so always have a snapshot.
    assertThat(applier.snapshot(Date.from(now))).isNotNull();
    assertThat(applier.snapshot(Date.from(now.plus(Duration.ofMinutes(30))))).isNotNull();

    // Got a target!
    SamplingTargetDocument target = SamplingTargetDocument.create(0.0, 5, null, null, "test");
    applier = applier.withTarget(target, Date.from(now));
    // No reservoir, always use fixed rate (drop)
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(applier.snapshot(Date.from(now))).isNull();
    clock.advance(Duration.ofSeconds(5));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(applier.snapshot(Date.from(now))).isNotNull();
  }

  @Test
  void withNextSnapshotTime() {
    TestClock clock = TestClock.create();
    SamplingRuleApplier applier =
        new SamplingRuleApplier(
            CLIENT_ID, readSamplingRule("/sampling-rule-reservoir.json"), clock);

    Instant now = Instant.ofEpochSecond(0, clock.now());
    assertThat(applier.snapshot(Date.from(now))).isNotNull();
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));

    applier = applier.withNextSnapshotTimeNanos(clock.now() + TimeUnit.SECONDS.toNanos(10));
    assertThat(applier.snapshot(Date.from(now))).isNull();
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    clock.advance(Duration.ofSeconds(10));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(applier.snapshot(Date.from(now))).isNotNull();
    assertThat(doSample(applier))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(applier)).isEqualTo(SamplingResult.create(SamplingDecision.DROP));
  }

  private static SamplingResult doSample(SamplingRuleApplier applier) {
    return applier.shouldSample(
        Context.current(),
        TraceId.fromLongs(1, 2),
        "span",
        SpanKind.CLIENT,
        Attributes.empty(),
        Collections.emptyList());
  }

  private static GetSamplingRulesResponse.SamplingRule readSamplingRule(String resourcePath) {
    try {
      return OBJECT_MAPPER.readValue(
          SamplingRuleApplierTest.class.getResource(resourcePath),
          GetSamplingRulesResponse.SamplingRule.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Attributes removeAttribute(Attributes attributes, AttributeKey<?> removedKey) {
    AttributesBuilder builder = Attributes.builder();
    // TODO(anuraaga): Replace with AttributeBuilder.remove
    attributes.forEach(
        (key, value) -> {
          if (!key.equals(removedKey)) {
            builder.put((AttributeKey) key, value);
          }
        });
    return builder.build();
  }
}
