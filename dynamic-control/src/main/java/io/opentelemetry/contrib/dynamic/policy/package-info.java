/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Defines the Telemetry Policy API, a mechanism for intent-based specification of telemetry behavior.
 *
 * <p>A Telemetry Policy is a distinct rule that governs how telemetry data is collected, processed,
 * or exported. Unlike traditional configuration, which often describes a pipeline of specific
 * components, a policy describes the <em>desired outcome</em> (the intent) and is
 * implementation-agnostic.
 *
 * <h2>Key Characteristics</h2>
 *
 * <ul>
 *   <li><strong>Typed:</strong> Each policy has a type (e.g., {@code trace-sampling}, {@code
 *       log-filter}) that defines its domain and behavior.
 *   <li><strong>Implementation Agnostic:</strong> The same policy definition can be enforced by an
 *       SDK, a Collector, or any other component that understands the policy type.
 *   <li><strong>Standalone:</strong> Policies are atomic and self-contained. They do not depend on
 *       pipeline configuration or other policies.
 *   <li><strong>Dynamic:</strong> Policies are designed to be updated at runtime, allowing behavior
 *       to change without restarting the application.
 *   <li><strong>Idempotent:</strong> Policies can be applied multiple times safely. The system
 *       converges to the desired state.
 * </ul>
 *
 * <h2>Architecture</h2>
 *
 * <p>The telemetry policy ecosystem typically consists of:
 *
 * <ul>
 *   <li><strong>Policy Providers:</strong> Sources of policies (e.g., a file, an HTTP endpoint, an
 *       OpAMP server).
 *   <li><strong>Policy Aggregator:</strong> Merges policies from multiple providers. Policies of the
 *       same type are merged (e.g., using JSON Merge Patch), while policies of different types
 *       coexist.
 *   <li><strong>Policy Implementations:</strong> Components that react to policy updates and enforce
 *       the specified behavior (e.g., updating a trace sampler).
 * </ul>
 *
 * @see io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy
 */
@ParametersAreNonnullByDefault
package io.opentelemetry.contrib.dynamic.policy;

import javax.annotation.ParametersAreNonnullByDefault;
