/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;

/**
 * A Sampler that uses the sampled flag of the span links if present. If at least one span link is
 * sampled, then this span will be sampled. Otherwise, it is not sampled. If the span has no span
 * links, this Sampler will use the "root" sampler that it is built with.
 */
public final class LinksBasedSampler implements Sampler {
    private final Sampler root;

    private LinksBasedSampler(Sampler root) {
        this.root = root;
    }

    public static LinksBasedSampler create(
            Sampler root) {
        return new LinksBasedSampler(root);
    }

    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {
        if (parentLinks.size() > 0) {
            for (LinkData linkData : parentLinks) {
                if (linkData.getSpanContext().isSampled()) {
                    return SamplingResult.recordAndSample();
                }
            }
            return SamplingResult.drop();
        }

        return this.root.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return String.format("LinksBased{root:%s}", this.root.getDescription());
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
