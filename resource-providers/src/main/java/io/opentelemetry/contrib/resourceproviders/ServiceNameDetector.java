/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import javax.annotation.Nullable;

/**
 * Functional interface for implementations that know how to detect a service name for a specific
 * application server type.
 */
interface ServiceNameDetector {
  @Nullable
  String detect() throws Exception;
}
