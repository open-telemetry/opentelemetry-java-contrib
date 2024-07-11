/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal.util;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledOnOpenJ9Condition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    AnnotatedElement element = context.getElement().orElse(null);
    return findAnnotation(element, DisabledOnOpenJ9.class)
        .map(
            annotation ->
                isOnOpenJ9()
                    ? disabled(element + " is @DisabledOnOpenJ9", annotation.value())
                    : enabled("Not running on OpenJ9 JVM"))
        .orElse(enabled("@DisabledOnOpenJ9 is not present"));
  }

  public boolean isOnOpenJ9() {
    return System.getProperty("java.vm.name").contains("J9");
  }
}
