/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * A {@link PolicyProvider} that reads policies from a local file, where each line represents a
 * separate policy configuration.
 *
 * <p>The file format supports two types of lines:
 *
 * <ul>
 *   <li><b>JSON Objects:</b> Lines starting with <code>{</code> are treated as JSON objects and
 *       validated against the registered {@link PolicyValidator}s.
 *   <li><b>Key-Value Pairs:</b> Lines in the format <code>key=value</code> are treated as aliases,
 *       where the key matches a validator's {@link PolicyValidator#getAlias()} and the value is
 *       parsed accordingly.
 * </ul>
 *
 * <p>Empty lines and lines starting with <code>#</code> are ignored.
 */
public final class LinePerPolicyFileProvider implements PolicyProvider {
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
      return policies;
    }

    try (Stream<String> lines = Files.lines(file)) {
      lines.forEach(
          line -> {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
              return;
            }

            TelemetryPolicy policy = null;

            if (trimmedLine.startsWith("{")) {
              for (PolicyValidator validator : validators) {
                if (trimmedLine.contains("\"" + validator.getPolicyType() + "\"")) {
                  policy = validator.validate(trimmedLine);
                  if (policy != null) {
                    break;
                  }
                }
              }
            } else {
              int idx = trimmedLine.indexOf('=');
              if (idx > 0) {
                String key = trimmedLine.substring(0, idx).trim();
                String valueStr = trimmedLine.substring(idx + 1).trim();

                for (PolicyValidator validator : validators) {
                  String alias = validator.getAlias();
                  if (alias != null && alias.equals(key)) {
                    policy = validator.validateAlias(key, valueStr);
                    if (policy != null) {
                      break;
                    }
                  }
                }
              }
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
