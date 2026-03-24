/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * A {@link PolicyProvider} that reads policies from a local file, where each line represents a
 * separate policy configuration.
 *
 * <p>Each non-empty line is parsed using one of two {@link SourceFormat}s:
 *
 * <ul>
 *   <li><b>{@link SourceFormat#JSONKEYVALUE JSONKEYVALUE}:</b> Lines starting with <code>{</code>
 *       use {@link SourceFormat#JSONKEYVALUE}: JSON text for a single top-level object with exactly
 *       one key (the policy type) and one value (the policy payload).
 *   <li><b>{@link SourceFormat#KEYVALUE KEYVALUE}:</b> Lines containing <code>=</code> are parsed
 *       as {@code policyType=value} and validated against the registered {@link PolicyValidator}s.
 * </ul>
 *
 * <p>Empty lines and lines starting with <code>#</code> are ignored.
 */
final class LinePerPolicyFileProvider implements PolicyProvider {
  private static final Logger logger = Logger.getLogger(LinePerPolicyFileProvider.class.getName());
  private final Path file;
  private final List<PolicyValidator> validators;

  public LinePerPolicyFileProvider(Path file, List<PolicyValidator> validators) {
    Objects.requireNonNull(file, "file cannot be null");
    this.file = file;
    this.validators = new ArrayList<>(validators);
  }

  @Override
  public List<TelemetryPolicy> fetchPolicies() throws IOException {
    List<TelemetryPolicy> policies = new ArrayList<>();
    if (!Files.exists(file)) {
      logger.info("Policy file does not exist: " + file);
      return policies;
    }

    try (Stream<String> lines = Files.lines(file)) {
      lines.forEach(
          line -> {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
              return;
            }

            SourceFormat format;
            if (trimmedLine.startsWith("{")) {
              format = SourceFormat.JSONKEYVALUE;
            } else if (trimmedLine.indexOf('=') >= 0) {
              format = SourceFormat.KEYVALUE;
            } else {
              logger.info("Unsupported policy line format: " + trimmedLine);
              return;
            }
            List<SourceWrapper> parsedSources = format.parse(trimmedLine);
            if (parsedSources == null || parsedSources.size() != 1) {
              logger.info("Invalid " + format.configValue() + " policy line: " + trimmedLine);
              return;
            }

            SourceWrapper parsedSource = parsedSources.get(0);
            String policyType = parsedSource.getPolicyType();
            if (policyType == null || policyType.isEmpty()) {
              logger.info("Policy type not found in line: " + trimmedLine);
              return;
            }
            TelemetryPolicy policy = null;
            for (PolicyValidator validator : validators) {
              if (!policyType.equals(validator.getPolicyType())) {
                continue;
              }
              policy = validator.validate(parsedSource);
              break;
            }
            if (policy == null) {
              logger.info("Validator not found or rejected for line: " + trimmedLine);
              return;
            }
            policies.add(policy);
          });
    }
    return policies;
  }
}
