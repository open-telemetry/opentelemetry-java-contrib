/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMojoGoalExecutionHandler implements MojoGoalExecutionHandler {
  private static final Logger logger =
      LoggerFactory.getLogger(AbstractMojoGoalExecutionHandler.class);
}
