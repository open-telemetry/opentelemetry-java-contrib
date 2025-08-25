/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static java.util.logging.Level.FINE;
import static java.util.stream.Collectors.toList;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingTargetDocument;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

final class XrayRulesSampler implements Sampler {

  private static final Logger logger = Logger.getLogger(XrayRulesSampler.class.getName());

  private final String clientId;
  private final Resource resource;
  private final Clock clock;
  private final Sampler fallbackSampler;
  private final SamplingRuleApplier[] ruleAppliers;

  XrayRulesSampler(
      String clientId,
      Resource resource,
      Clock clock,
      Sampler fallbackSampler,
      List<GetSamplingRulesResponse.SamplingRule> rules) {
    this(
        clientId,
        resource,
        clock,
        fallbackSampler,
        rules.stream()
            // Lower priority value takes precedence so normal ascending sort.
            .sorted(Comparator.comparingInt(GetSamplingRulesResponse.SamplingRule::getPriority))
            .map(rule -> new SamplingRuleApplier(clientId, rule, clock))
            .toArray(SamplingRuleApplier[]::new));
  }

  private XrayRulesSampler(
      String clientId,
      Resource resource,
      Clock clock,
      Sampler fallbackSampler,
      SamplingRuleApplier[] ruleAppliers) {
    this.clientId = clientId;
    this.resource = resource;
    this.clock = clock;
    this.fallbackSampler = fallbackSampler;
    this.ruleAppliers = ruleAppliers;
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    for (SamplingRuleApplier applier : ruleAppliers) {
      if (applier.matches(attributes, resource)) {
        return applier.shouldSample(
            parentContext, traceId, name, spanKind, attributes, parentLinks);
      }
    }

    // In practice, X-Ray always returns a Default rule that matches all requests so it is a bug in
    // our code or X-Ray to reach here, fallback just in case.
    logger.log(
        FINE,
        "No sampling rule matched the request. "
            + "This is a bug in either the OpenTelemetry SDK or X-Ray.");
    return fallbackSampler.shouldSample(
        parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return "XrayRulesSampler{" + Arrays.toString(ruleAppliers) + "}";
  }

  List<GetSamplingTargetsRequest.SamplingStatisticsDocument> snapshot(Date now) {
    return Arrays.stream(ruleAppliers)
        .map(rule -> rule.snapshot(now))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  long nextTargetFetchTimeNanos() {
    return Arrays.stream(ruleAppliers)
        .mapToLong(SamplingRuleApplier::getNextSnapshotTimeNanos)
        .min()
        // There is always at least one rule in practice so this should never be exercised.
        .orElseGet(() -> clock.nanoTime() + AwsXrayRemoteSampler.DEFAULT_TARGET_INTERVAL_NANOS);
  }

  XrayRulesSampler withTargets(
      Map<String, SamplingTargetDocument> ruleTargets,
      Set<String> requestedTargetRuleNames,
      Date now) {
    long defaultNextSnapshotTimeNanos =
        clock.nanoTime() + AwsXrayRemoteSampler.DEFAULT_TARGET_INTERVAL_NANOS;
    SamplingRuleApplier[] newAppliers =
        Arrays.stream(ruleAppliers)
            .map(
                rule -> {
                  SamplingTargetDocument target = ruleTargets.get(rule.getRuleName());
                  if (target != null) {
                    return rule.withTarget(target, now);
                  }
                  if (requestedTargetRuleNames.contains(rule.getRuleName())) {
                    // In practice X-Ray should return a target for any rule we requested but
                    // do a defensive check here in case. If we requested a target but got nothing
                    // back assume the default interval.
                    return rule.withNextSnapshotTimeNanos(defaultNextSnapshotTimeNanos);
                  }
                  // Target not requested, will be updated in a future target fetch.
                  return rule;
                })
            .toArray(SamplingRuleApplier[]::new);
    return new XrayRulesSampler(clientId, resource, clock, fallbackSampler, newAppliers);
  }
}
