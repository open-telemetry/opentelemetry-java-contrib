/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceMappingConfig;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** {@link PolicyProvider} implementation backed by an HTTP/HTTPS policy endpoint. */
public final class HttpPolicyProvider extends AbstractPolicyProvider {
  private static final Logger logger = Logger.getLogger(HttpPolicyProvider.class.getName());

  private final URI endpoint;
  private final SourceFormat format;
  private final MappedPolicySourceConverter sourceConverter;
  private final AtomicReference<Consumer<List<TelemetryPolicy>>> updateConsumer =
      new AtomicReference<>();
  private final AtomicReference<Closeable> pollRegistration = new AtomicReference<>();

  public HttpPolicyProvider(
      URI endpoint,
      SourceFormat format,
      List<PolicySourceMappingConfig> mappings,
      List<PolicyValidator> validators) {
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint cannot be null");
    validateEndpoint(endpoint);
    this.format = Objects.requireNonNull(format, "format cannot be null");
    Objects.requireNonNull(mappings, "mappings cannot be null");
    Objects.requireNonNull(validators, "validators cannot be null");
    if (mappings.isEmpty()) {
      throw new IllegalArgumentException("HTTP policy provider requires at least one mapping");
    }
    this.sourceConverter = MappedPolicySourceConverter.create(mappings, validators);
  }

  @Override
  public List<TelemetryPolicy> fetchPolicies() {
    return getCurrentPolicies();
  }

  @Override
  public Closeable startWatching(Consumer<List<TelemetryPolicy>> onUpdate) {
    updateConsumer.set(Objects.requireNonNull(onUpdate, "onUpdate cannot be null"));
    Closeable existingRegistration = pollRegistration.get();
    if (existingRegistration != null) {
      return this::stopWatching;
    }
    Closeable registration = PolicyProviderPoller.registerUrl(endpoint, this::reloadAndNotify);
    if (!pollRegistration.compareAndSet(null, registration)) {
      closeRegistration(registration);
    }
    return this::stopWatching;
  }

  /** Resets shared polling state used by tests. */
  public static void resetForTest() {
    PolicyProviderPoller.reset();
  }

  private void reloadAndNotify(URI modifiedEndpoint, byte[] responseBody) {
    List<TelemetryPolicy> policies =
        updateCurrentPolicies(parsePolicyText(modifiedEndpoint.toString(), responseBody));
    Consumer<List<TelemetryPolicy>> onUpdate = updateConsumer.get();
    if (onUpdate != null) {
      onUpdate.accept(policies);
    }
  }

  private List<TelemetryPolicy> parsePolicyText(String key, byte[] policyBytes) {
    String policyText = new String(policyBytes, StandardCharsets.UTF_8);
    logger.info("Received HTTP policy payload for key '" + key + "': " + policyText);
    List<SourceWrapper> parsedSources =
        format.parse(policyText, sourceConverter.getMappedPolicyIds());
    if (parsedSources == null) {
      logger.info("Ignoring invalid HTTP policy payload for key: " + key);
      return Collections.emptyList();
    }
    return sourceConverter.convert(parsedSources, SourceKind.HTTP);
  }

  private void stopWatching() {
    Closeable registration = pollRegistration.getAndSet(null);
    closeRegistration(registration);
    updateConsumer.set(null);
  }

  private static void closeRegistration(@Nullable Closeable registration) {
    if (registration != null) {
      try {
        registration.close();
      } catch (IOException e) {
        logger.log(Level.INFO, "Failed to close policy provider polling registration", e);
      }
    }
  }

  private static void validateEndpoint(URI endpoint) {
    String scheme = endpoint.getScheme();
    if (scheme == null) {
      throw new IllegalArgumentException("HTTP policy endpoint must use http or https scheme");
    }
    String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
    if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
      throw new IllegalArgumentException("HTTP policy endpoint must use http or https scheme");
    }
  }
}
