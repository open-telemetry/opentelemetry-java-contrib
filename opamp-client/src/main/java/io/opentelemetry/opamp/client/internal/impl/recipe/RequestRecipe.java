/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe;

import io.opentelemetry.opamp.client.internal.request.Field;
import java.util.Collection;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class RequestRecipe {
  private final Collection<Field> fields;

  public RequestRecipe(Collection<Field> fields) {
    this.fields = fields;
  }

  public Collection<Field> getFields() {
    return fields;
  }
}
